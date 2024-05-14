package software.amazon.smithy.swift.codegen.middleware

import software.amazon.smithy.aws.traits.auth.UnsignedPayloadTrait
import software.amazon.smithy.model.Model
import software.amazon.smithy.model.shapes.OperationShape
import software.amazon.smithy.model.shapes.ServiceShape
import software.amazon.smithy.swift.codegen.ClientRuntimeTypes
import software.amazon.smithy.swift.codegen.ClientRuntimeTypes.Middleware.OperationStack
import software.amazon.smithy.swift.codegen.SwiftWriter
import software.amazon.smithy.swift.codegen.integration.HTTPProtocolCustomizable
import software.amazon.smithy.swift.codegen.integration.HttpBindingResolver
import software.amazon.smithy.swift.codegen.integration.ProtocolGenerator
import software.amazon.smithy.swift.codegen.integration.middlewares.handlers.MiddlewareShapeUtils
import software.amazon.smithy.swift.codegen.model.toLowerCamelCase
import software.amazon.smithy.swift.codegen.model.toUpperCamelCase
import software.amazon.smithy.swift.codegen.swiftFunctionParameterIndent

typealias HttpMethodCallback = (OperationShape) -> String
class MiddlewareExecutionGenerator(
    private val ctx: ProtocolGenerator.GenerationContext,
    private val writer: SwiftWriter,
    private val httpBindingResolver: HttpBindingResolver,
    private val httpProtocolCustomizable: HTTPProtocolCustomizable,
    private val operationMiddleware: OperationMiddleware,
    private val operationStackName: String,
    private val httpMethodCallback: HttpMethodCallback? = null
) {
    private val model: Model = ctx.model
    private val symbolProvider = ctx.symbolProvider

    fun render(
        serviceShape: ServiceShape,
        op: OperationShape,
        flowType: ContextAttributeCodegenFlowType = ContextAttributeCodegenFlowType.NORMAL,
        onError: (SwiftWriter, String) -> Unit,
    ) {
        val operationErrorName = "${op.toUpperCamelCase()}OutputError"
        val inputShape = MiddlewareShapeUtils.inputSymbol(symbolProvider, ctx.model, op)
        val outputShape = MiddlewareShapeUtils.outputSymbol(symbolProvider, ctx.model, op)
        writer.write("let context = \$N()", ClientRuntimeTypes.Http.HttpContextBuilder)
        writer.swiftFunctionParameterIndent {
            renderContextAttributes(op, flowType)
        }
        httpProtocolCustomizable.renderEventStreamAttributes(ctx, writer, op)
        if (!ctx.settings.useInterceptors) {
            writer.write(
                "var \$L = \$N<\$L, \$L>(id: \$S)",
                operationStackName,
                OperationStack,
                inputShape.name,
                outputShape.name,
                op.toLowerCamelCase(),
            )
        } else {
            writer.write(
                "let builder = \$N<\$N, \$N, \$N, \$N, \$N>()",
                ClientRuntimeTypes.Operation.OrchestratorBuilder,
                inputShape,
                outputShape,
                ClientRuntimeTypes.Http.SdkHttpRequest,
                ClientRuntimeTypes.Http.HttpResponse,
                ClientRuntimeTypes.Http.HttpContext,
            )
            writer.write("config.interceptorProviders.forEach { builder.interceptors.add($$0.create()) }")
            // Swift can't infer the generic arguments to `create` for some reason
            writer.write(
                """
                config.httpInterceptorProviders.forEach {
                    let i: any HttpInterceptor<${'$'}N, ${'$'}N> = $$0.create()
                    builder.interceptors.add(i)
                }
                """.trimIndent(),
                inputShape,
                outputShape,
            )
        }

        renderMiddlewares(ctx, op, operationStackName)

        if (ctx.settings.useInterceptors) {
            writer.write(
                """
                let op = builder.attributes(context)
                    .executeRequest(client)
                    .build()
                """.trimIndent()
            )
        }
    }

    private fun renderContextAttributes(op: OperationShape, flowType: ContextAttributeCodegenFlowType) {
        val httpMethod = resolveHttpMethod(op)

        // FIXME it over indents if i add another indent, come up with better way to properly indent or format for swift

        writer.write("  .withMethod(value: .$httpMethod)")
        writer.write("  .withServiceName(value: serviceName)")
        writer.write("  .withOperation(value: \"${op.toLowerCamelCase()}\")")
        writer.write("  .withIdempotencyTokenGenerator(value: config.idempotencyTokenGenerator)")
        writer.write("  .withLogger(value: config.logger)")
        writer.write("  .withPartitionID(value: config.partitionID)")
        writer.write("  .withAuthSchemes(value: config.authSchemes ?? [])")
        writer.write("  .withAuthSchemeResolver(value: config.authSchemeResolver)")
        writer.write("  .withUnsignedPayloadTrait(value: ${op.hasTrait(UnsignedPayloadTrait::class.java)})")
        writer.write("  .withSocketTimeout(value: config.httpClientConfiguration.socketTimeout)")

        // Add flag for presign / presign-url flows
        if (flowType == ContextAttributeCodegenFlowType.PRESIGN_REQUEST) {
            writer.write("  .withFlowType(value: .PRESIGN_REQUEST)")
        } else if (flowType == ContextAttributeCodegenFlowType.PRESIGN_URL) {
            writer.write("  .withFlowType(value: .PRESIGN_URL)")
        }
        // Add expiration flag for presign / presign-url flows
        if (flowType != ContextAttributeCodegenFlowType.NORMAL) {
            writer.write("  .withExpiration(value: expiration)")
        }

        val serviceShape = ctx.service
        httpProtocolCustomizable.renderContextAttributes(ctx, writer, serviceShape, op)
        writer.write("  .build()")
    }

    private fun resolveHttpMethod(op: OperationShape): String {
        return httpMethodCallback?.let {
            it(op)
        } ?: run {
            val httpTrait = httpBindingResolver.httpTrait(op)
            httpTrait.method.toLowerCase()
        }
    }

    private fun renderMiddlewares(ctx: ProtocolGenerator.GenerationContext, op: OperationShape, operationStackName: String) {
        operationMiddleware.renderMiddleware(ctx, writer, op, operationStackName, MiddlewareStep.INITIALIZESTEP)
        operationMiddleware.renderMiddleware(ctx, writer, op, operationStackName, MiddlewareStep.BUILDSTEP)
        operationMiddleware.renderMiddleware(ctx, writer, op, operationStackName, MiddlewareStep.SERIALIZESTEP)
        operationMiddleware.renderMiddleware(ctx, writer, op, operationStackName, MiddlewareStep.FINALIZESTEP)
        operationMiddleware.renderMiddleware(ctx, writer, op, operationStackName, MiddlewareStep.DESERIALIZESTEP)
    }

    /*
     * The enum in this companion object is used to determine under which codegen flow
     * the middleware context is being code-generated.
     *
     * For PRESIGN_REQUEST & PRESIGN_URL flows:
     * - The value of expiration is saved to middleware context during codegen.
     * - The flow type information is saved to middleware context during codegen, for consumption by
     *   AWS auth schemes during runtime to determine where to put the request signature in the request.
     */
    companion object {
        enum class ContextAttributeCodegenFlowType {
            NORMAL, PRESIGN_REQUEST, PRESIGN_URL
        }
    }
}

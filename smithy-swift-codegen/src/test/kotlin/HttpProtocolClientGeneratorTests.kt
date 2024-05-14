/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0.
 */

import io.kotest.matchers.string.shouldContain
import io.kotest.matchers.string.shouldContainOnlyOnce
import org.junit.jupiter.api.Test
import software.amazon.smithy.swift.codegen.DefaultClientConfigurationIntegration

class HttpProtocolClientGeneratorTests {
    @Test
    fun `it renders client initialization block`() {
        val context = setupTests("service-generator-test-operations.smithy", "com.test#Example")
        val contents = getFileContents(context.manifest, "/RestJson/RestJsonProtocolClient.swift")
        contents.shouldSyntacticSanityCheck()
        val expected = """
public class RestJsonProtocolClient: Client {
    public static let clientName = "RestJsonProtocolClient"
    let client: ClientRuntime.SdkHttpClient
    let config: RestJsonProtocolClient.RestJsonProtocolClientConfiguration
    let serviceName = "Rest Json Protocol"

    public required init(config: RestJsonProtocolClient.RestJsonProtocolClientConfiguration) {
        client = ClientRuntime.SdkHttpClient(engine: config.httpClientEngine, config: config.httpClientConfiguration)
        self.config = config
    }

    public convenience required init() throws {
        let config = try RestJsonProtocolClient.RestJsonProtocolClientConfiguration()
        self.init(config: config)
    }

}

extension RestJsonProtocolClient {
    public class RestJsonProtocolClientConfiguration: DefaultClientConfiguration & DefaultHttpClientConfiguration {
        public var telemetryProvider: ClientRuntime.TelemetryProvider

        public var retryStrategyOptions: ClientRuntime.RetryStrategyOptions

        public var clientLogMode: ClientRuntime.ClientLogMode

        public var endpoint: Swift.String?

        public var idempotencyTokenGenerator: ClientRuntime.IdempotencyTokenGenerator

        public var httpClientEngine: ClientRuntime.HTTPClient

        public var httpClientConfiguration: ClientRuntime.HttpClientConfiguration

        public var authSchemes: [ClientRuntime.AuthScheme]?

        public var authSchemeResolver: ClientRuntime.AuthSchemeResolver

        public private(set) var interceptorProviders: [ClientRuntime.InterceptorProvider]

        public private(set) var httpInterceptorProviders: [ClientRuntime.HttpInterceptorProvider]

        private init(_ telemetryProvider: ClientRuntime.TelemetryProvider, _ retryStrategyOptions: ClientRuntime.RetryStrategyOptions, _ clientLogMode: ClientRuntime.ClientLogMode, _ endpoint: Swift.String?, _ idempotencyTokenGenerator: ClientRuntime.IdempotencyTokenGenerator, _ httpClientEngine: ClientRuntime.HTTPClient, _ httpClientConfiguration: ClientRuntime.HttpClientConfiguration, _ authSchemes: [ClientRuntime.AuthScheme]?, _ authSchemeResolver: ClientRuntime.AuthSchemeResolver, _ interceptorProviders: [ClientRuntime.InterceptorProvider], _ httpInterceptorProviders: [ClientRuntime.HttpInterceptorProvider]) {
            self.telemetryProvider = telemetryProvider
            self.retryStrategyOptions = retryStrategyOptions
            self.clientLogMode = clientLogMode
            self.endpoint = endpoint
            self.idempotencyTokenGenerator = idempotencyTokenGenerator
            self.httpClientEngine = httpClientEngine
            self.httpClientConfiguration = httpClientConfiguration
            self.authSchemes = authSchemes
            self.authSchemeResolver = authSchemeResolver
            self.interceptorProviders = interceptorProviders
            self.httpInterceptorProviders = httpInterceptorProviders
        }

        public convenience init(telemetryProvider: ClientRuntime.TelemetryProvider? = nil, retryStrategyOptions: ClientRuntime.RetryStrategyOptions? = nil, clientLogMode: ClientRuntime.ClientLogMode? = nil, endpoint: Swift.String? = nil, idempotencyTokenGenerator: ClientRuntime.IdempotencyTokenGenerator? = nil, httpClientEngine: ClientRuntime.HTTPClient? = nil, httpClientConfiguration: ClientRuntime.HttpClientConfiguration? = nil, authSchemes: [ClientRuntime.AuthScheme]? = nil, authSchemeResolver: ClientRuntime.AuthSchemeResolver? = nil, interceptorProviders: [ClientRuntime.InterceptorProvider]? = nil, httpInterceptorProviders: [ClientRuntime.HttpInterceptorProvider]? = nil) throws {
            self.init(telemetryProvider ?? ClientRuntime.DefaultTelemetry.provider, retryStrategyOptions ?? ClientConfigurationDefaults.defaultRetryStrategyOptions, clientLogMode ?? ClientConfigurationDefaults.defaultClientLogMode, endpoint, idempotencyTokenGenerator ?? ClientConfigurationDefaults.defaultIdempotencyTokenGenerator, httpClientEngine ?? ClientConfigurationDefaults.makeClient(httpClientConfiguration: httpClientConfiguration ?? ClientConfigurationDefaults.defaultHttpClientConfiguration), httpClientConfiguration ?? ClientConfigurationDefaults.defaultHttpClientConfiguration, authSchemes, authSchemeResolver ?? ClientConfigurationDefaults.defaultAuthSchemeResolver, interceptorProviders ?? [], httpInterceptorProviders ?? [])
        }

        public convenience required init() async throws {
            try await self.init(telemetryProvider: nil, retryStrategyOptions: nil, clientLogMode: nil, endpoint: nil, idempotencyTokenGenerator: nil, httpClientEngine: nil, httpClientConfiguration: nil, authSchemes: nil, authSchemeResolver: nil, interceptorProviders: nil, httpInterceptorProviders: nil)
        }

        public var partitionID: String? {
            return ""
        }
        public func addInterceptorProvider(_ provider: ClientRuntime.InterceptorProvider) {
            self.interceptorProviders.append(provider)
        }

        public func addInterceptorProvider(_ provider: ClientRuntime.HttpInterceptorProvider) {
            self.httpInterceptorProviders.append(provider)
        }

    }

    public static func builder() -> ClientBuilder<RestJsonProtocolClient> {
        return ClientBuilder<RestJsonProtocolClient>(defaultPlugins: [
            ClientRuntime.DefaultClientPlugin()
        ])
    }
}

public struct RestJsonProtocolClientLogHandlerFactory: ClientRuntime.SDKLogHandlerFactory {
    public var label = "RestJsonProtocolClient"
    let logLevel: ClientRuntime.SDKLogLevel
    public func construct(label: String) -> LogHandler {
        var handler = StreamLogHandler.standardOutput(label: label)
        handler.logLevel = logLevel.toLoggerType()
        return handler
    }
    public init(logLevel: ClientRuntime.SDKLogLevel) {
        self.logLevel = logLevel
    }
}
"""
        contents.shouldContainOnlyOnce(expected)
    }
    @Test
    fun `it renders host prefix with label in context correctly`() {
        val context = setupTests("host-prefix-operation.smithy", "com.test#Example")
        val contents = getFileContents(context.manifest, "/RestJson/RestJsonProtocolClient.swift")
        contents.shouldSyntacticSanityCheck()
        val expectedFragment = """
        let context = ClientRuntime.HttpContextBuilder()
                      .withMethod(value: .post)
                      .withServiceName(value: serviceName)
                      .withOperation(value: "getStatus")
                      .withIdempotencyTokenGenerator(value: config.idempotencyTokenGenerator)
                      .withLogger(value: config.logger)
                      .withPartitionID(value: config.partitionID)
                      .withAuthSchemes(value: config.authSchemes ?? [])
                      .withAuthSchemeResolver(value: config.authSchemeResolver)
                      .withUnsignedPayloadTrait(value: false)
                      .withSocketTimeout(value: config.httpClientConfiguration.socketTimeout)
                      .build()
"""
        contents.shouldContainOnlyOnce(expectedFragment)
    }
    @Test
    fun `it renders operation implementations in extension`() {
        val context = setupTests("service-generator-test-operations.smithy", "com.test#Example")
        val contents = getFileContents(context.manifest, "/RestJson/RestJsonProtocolClient.swift")
        contents.shouldSyntacticSanityCheck()
        contents.shouldContain("extension RestJsonProtocolClient {")
    }
    @Test
    fun `it renders an operation body`() {
        val context = setupTests("service-generator-test-operations.smithy", "com.test#Example")
        val contents = getFileContents(context.manifest, "/RestJson/RestJsonProtocolClient.swift")
        contents.shouldSyntacticSanityCheck()
        val expected = """
    public func allocateWidget(input: AllocateWidgetInput) async throws -> AllocateWidgetOutput {
        let context = ClientRuntime.HttpContextBuilder()
                      .withMethod(value: .post)
                      .withServiceName(value: serviceName)
                      .withOperation(value: "allocateWidget")
                      .withIdempotencyTokenGenerator(value: config.idempotencyTokenGenerator)
                      .withLogger(value: config.logger)
                      .withPartitionID(value: config.partitionID)
                      .withAuthSchemes(value: config.authSchemes ?? [])
                      .withAuthSchemeResolver(value: config.authSchemeResolver)
                      .withUnsignedPayloadTrait(value: false)
                      .withSocketTimeout(value: config.httpClientConfiguration.socketTimeout)
                      .build()
        var operation = ClientRuntime.OperationStack<AllocateWidgetInput, AllocateWidgetOutput>(id: "allocateWidget")
        operation.initializeStep.intercept(position: .after, middleware: ClientRuntime.IdempotencyTokenMiddleware<AllocateWidgetInput, AllocateWidgetOutput>(keyPath: \.clientToken))
        operation.initializeStep.intercept(position: .after, middleware: ClientRuntime.URLPathMiddleware<AllocateWidgetInput, AllocateWidgetOutput>(AllocateWidgetInput.urlPathProvider(_:)))
        operation.initializeStep.intercept(position: .after, middleware: ClientRuntime.URLHostMiddleware<AllocateWidgetInput, AllocateWidgetOutput>())
        operation.buildStep.intercept(position: .before, middleware: ClientRuntime.AuthSchemeMiddleware<AllocateWidgetOutput>())
        operation.serializeStep.intercept(position: .after, middleware: ContentTypeMiddleware<AllocateWidgetInput, AllocateWidgetOutput>(contentType: "application/json"))
        operation.serializeStep.intercept(position: .after, middleware: ClientRuntime.BodyMiddleware<AllocateWidgetInput, AllocateWidgetOutput, SmithyJSON.Writer>(rootNodeInfo: "", inputWritingClosure: AllocateWidgetInput.write(value:to:)))
        operation.finalizeStep.intercept(position: .before, middleware: ClientRuntime.ContentLengthMiddleware<AllocateWidgetInput, AllocateWidgetOutput>())
        operation.finalizeStep.intercept(position: .after, middleware: ClientRuntime.RetryMiddleware<ClientRuntime.DefaultRetryStrategy, ClientRuntime.DefaultRetryErrorInfoProvider, AllocateWidgetOutput>(options: config.retryStrategyOptions))
        operation.finalizeStep.intercept(position: .before, middleware: ClientRuntime.SignerMiddleware<AllocateWidgetOutput>())
        operation.deserializeStep.intercept(position: .after, middleware: ClientRuntime.DeserializeMiddleware<AllocateWidgetOutput>(AllocateWidgetOutput.httpOutput(from:), AllocateWidgetOutputError.httpError(from:)))
        operation.deserializeStep.intercept(position: .after, middleware: ClientRuntime.LoggerMiddleware<AllocateWidgetInput, AllocateWidgetOutput>(clientLogMode: config.clientLogMode))
        let result = try await operation.handleMiddleware(context: context, input: input, next: client.getHandler())
        return result
    }
"""
        contents.shouldContainOnlyOnce(expected)
    }

    @Test
    fun `ContentLengthMiddleware generates correctly with requiresLength false and unsignedPayload true`() {
        val context = setupTests("service-generator-test-operations.smithy", "com.test#Example")
        val contents = getFileContents(context.manifest, "/RestJson/RestJsonProtocolClient.swift")
        contents.shouldSyntacticSanityCheck()
        val expected = """
    public func unsignedFooBlobStream(input: UnsignedFooBlobStreamInput) async throws -> UnsignedFooBlobStreamOutput {
        let context = ClientRuntime.HttpContextBuilder()
                      .withMethod(value: .post)
                      .withServiceName(value: serviceName)
                      .withOperation(value: "unsignedFooBlobStream")
                      .withIdempotencyTokenGenerator(value: config.idempotencyTokenGenerator)
                      .withLogger(value: config.logger)
                      .withPartitionID(value: config.partitionID)
                      .withAuthSchemes(value: config.authSchemes ?? [])
                      .withAuthSchemeResolver(value: config.authSchemeResolver)
                      .withUnsignedPayloadTrait(value: true)
                      .withSocketTimeout(value: config.httpClientConfiguration.socketTimeout)
                      .build()
        var operation = ClientRuntime.OperationStack<UnsignedFooBlobStreamInput, UnsignedFooBlobStreamOutput>(id: "unsignedFooBlobStream")
        operation.initializeStep.intercept(position: .after, middleware: ClientRuntime.URLPathMiddleware<UnsignedFooBlobStreamInput, UnsignedFooBlobStreamOutput>(UnsignedFooBlobStreamInput.urlPathProvider(_:)))
        operation.initializeStep.intercept(position: .after, middleware: ClientRuntime.URLHostMiddleware<UnsignedFooBlobStreamInput, UnsignedFooBlobStreamOutput>())
        operation.buildStep.intercept(position: .before, middleware: ClientRuntime.AuthSchemeMiddleware<UnsignedFooBlobStreamOutput>())
        operation.serializeStep.intercept(position: .after, middleware: ContentTypeMiddleware<UnsignedFooBlobStreamInput, UnsignedFooBlobStreamOutput>(contentType: "application/json"))
        operation.serializeStep.intercept(position: .after, middleware: ClientRuntime.BodyMiddleware<UnsignedFooBlobStreamInput, UnsignedFooBlobStreamOutput, SmithyJSON.Writer>(rootNodeInfo: "", inputWritingClosure: UnsignedFooBlobStreamInput.write(value:to:)))
        operation.finalizeStep.intercept(position: .before, middleware: ClientRuntime.ContentLengthMiddleware<UnsignedFooBlobStreamInput, UnsignedFooBlobStreamOutput>(requiresLength: false, unsignedPayload: true))
        operation.finalizeStep.intercept(position: .after, middleware: ClientRuntime.RetryMiddleware<ClientRuntime.DefaultRetryStrategy, ClientRuntime.DefaultRetryErrorInfoProvider, UnsignedFooBlobStreamOutput>(options: config.retryStrategyOptions))
        operation.finalizeStep.intercept(position: .before, middleware: ClientRuntime.SignerMiddleware<UnsignedFooBlobStreamOutput>())
        operation.deserializeStep.intercept(position: .after, middleware: ClientRuntime.DeserializeMiddleware<UnsignedFooBlobStreamOutput>(UnsignedFooBlobStreamOutput.httpOutput(from:), UnsignedFooBlobStreamOutputError.httpError(from:)))
        operation.deserializeStep.intercept(position: .after, middleware: ClientRuntime.LoggerMiddleware<UnsignedFooBlobStreamInput, UnsignedFooBlobStreamOutput>(clientLogMode: config.clientLogMode))
        let result = try await operation.handleMiddleware(context: context, input: input, next: client.getHandler())
        return result
    }
"""
        contents.shouldContainOnlyOnce(expected)
    }

    @Test
    fun `ContentLengthMiddleware generates correctly with requiresLength true and unsignedPayload false`() {
        val context = setupTests("service-generator-test-operations.smithy", "com.test#Example")
        val contents = getFileContents(context.manifest, "/RestJson/RestJsonProtocolClient.swift")
        contents.shouldSyntacticSanityCheck()
        val expected = """
    public func explicitBlobStreamWithLength(input: ExplicitBlobStreamWithLengthInput) async throws -> ExplicitBlobStreamWithLengthOutput {
        let context = ClientRuntime.HttpContextBuilder()
                      .withMethod(value: .post)
                      .withServiceName(value: serviceName)
                      .withOperation(value: "explicitBlobStreamWithLength")
                      .withIdempotencyTokenGenerator(value: config.idempotencyTokenGenerator)
                      .withLogger(value: config.logger)
                      .withPartitionID(value: config.partitionID)
                      .withAuthSchemes(value: config.authSchemes ?? [])
                      .withAuthSchemeResolver(value: config.authSchemeResolver)
                      .withUnsignedPayloadTrait(value: false)
                      .withSocketTimeout(value: config.httpClientConfiguration.socketTimeout)
                      .build()
        var operation = ClientRuntime.OperationStack<ExplicitBlobStreamWithLengthInput, ExplicitBlobStreamWithLengthOutput>(id: "explicitBlobStreamWithLength")
        operation.initializeStep.intercept(position: .after, middleware: ClientRuntime.URLPathMiddleware<ExplicitBlobStreamWithLengthInput, ExplicitBlobStreamWithLengthOutput>(ExplicitBlobStreamWithLengthInput.urlPathProvider(_:)))
        operation.initializeStep.intercept(position: .after, middleware: ClientRuntime.URLHostMiddleware<ExplicitBlobStreamWithLengthInput, ExplicitBlobStreamWithLengthOutput>())
        operation.buildStep.intercept(position: .before, middleware: ClientRuntime.AuthSchemeMiddleware<ExplicitBlobStreamWithLengthOutput>())
        operation.serializeStep.intercept(position: .after, middleware: ContentTypeMiddleware<ExplicitBlobStreamWithLengthInput, ExplicitBlobStreamWithLengthOutput>(contentType: "application/octet-stream"))
        operation.serializeStep.intercept(position: .after, middleware: ClientRuntime.BlobStreamBodyMiddleware<ExplicitBlobStreamWithLengthInput, ExplicitBlobStreamWithLengthOutput>(keyPath: \.payload1))
        operation.finalizeStep.intercept(position: .before, middleware: ClientRuntime.ContentLengthMiddleware<ExplicitBlobStreamWithLengthInput, ExplicitBlobStreamWithLengthOutput>(requiresLength: true, unsignedPayload: false))
        operation.finalizeStep.intercept(position: .after, middleware: ClientRuntime.RetryMiddleware<ClientRuntime.DefaultRetryStrategy, ClientRuntime.DefaultRetryErrorInfoProvider, ExplicitBlobStreamWithLengthOutput>(options: config.retryStrategyOptions))
        operation.finalizeStep.intercept(position: .before, middleware: ClientRuntime.SignerMiddleware<ExplicitBlobStreamWithLengthOutput>())
        operation.deserializeStep.intercept(position: .after, middleware: ClientRuntime.DeserializeMiddleware<ExplicitBlobStreamWithLengthOutput>(ExplicitBlobStreamWithLengthOutput.httpOutput(from:), ExplicitBlobStreamWithLengthOutputError.httpError(from:)))
        operation.deserializeStep.intercept(position: .after, middleware: ClientRuntime.LoggerMiddleware<ExplicitBlobStreamWithLengthInput, ExplicitBlobStreamWithLengthOutput>(clientLogMode: config.clientLogMode))
        let result = try await operation.handleMiddleware(context: context, input: input, next: client.getHandler())
        return result
    }
"""
        contents.shouldContainOnlyOnce(expected)
    }

    @Test
    fun `ContentLengthMiddleware generates correctly with requiresLength true and unsignedPayload true`() {
        val context = setupTests("service-generator-test-operations.smithy", "com.test#Example")
        val contents = getFileContents(context.manifest, "/RestJson/RestJsonProtocolClient.swift")
        contents.shouldSyntacticSanityCheck()
        val expected = """
    public func unsignedFooBlobStreamWithLength(input: UnsignedFooBlobStreamWithLengthInput) async throws -> UnsignedFooBlobStreamWithLengthOutput {
        let context = ClientRuntime.HttpContextBuilder()
                      .withMethod(value: .post)
                      .withServiceName(value: serviceName)
                      .withOperation(value: "unsignedFooBlobStreamWithLength")
                      .withIdempotencyTokenGenerator(value: config.idempotencyTokenGenerator)
                      .withLogger(value: config.logger)
                      .withPartitionID(value: config.partitionID)
                      .withAuthSchemes(value: config.authSchemes ?? [])
                      .withAuthSchemeResolver(value: config.authSchemeResolver)
                      .withUnsignedPayloadTrait(value: true)
                      .withSocketTimeout(value: config.httpClientConfiguration.socketTimeout)
                      .build()
        var operation = ClientRuntime.OperationStack<UnsignedFooBlobStreamWithLengthInput, UnsignedFooBlobStreamWithLengthOutput>(id: "unsignedFooBlobStreamWithLength")
        operation.initializeStep.intercept(position: .after, middleware: ClientRuntime.URLPathMiddleware<UnsignedFooBlobStreamWithLengthInput, UnsignedFooBlobStreamWithLengthOutput>(UnsignedFooBlobStreamWithLengthInput.urlPathProvider(_:)))
        operation.initializeStep.intercept(position: .after, middleware: ClientRuntime.URLHostMiddleware<UnsignedFooBlobStreamWithLengthInput, UnsignedFooBlobStreamWithLengthOutput>())
        operation.buildStep.intercept(position: .before, middleware: ClientRuntime.AuthSchemeMiddleware<UnsignedFooBlobStreamWithLengthOutput>())
        operation.serializeStep.intercept(position: .after, middleware: ContentTypeMiddleware<UnsignedFooBlobStreamWithLengthInput, UnsignedFooBlobStreamWithLengthOutput>(contentType: "application/octet-stream"))
        operation.serializeStep.intercept(position: .after, middleware: ClientRuntime.BlobStreamBodyMiddleware<UnsignedFooBlobStreamWithLengthInput, UnsignedFooBlobStreamWithLengthOutput>(keyPath: \.payload1))
        operation.finalizeStep.intercept(position: .before, middleware: ClientRuntime.ContentLengthMiddleware<UnsignedFooBlobStreamWithLengthInput, UnsignedFooBlobStreamWithLengthOutput>(requiresLength: true, unsignedPayload: true))
        operation.finalizeStep.intercept(position: .after, middleware: ClientRuntime.RetryMiddleware<ClientRuntime.DefaultRetryStrategy, ClientRuntime.DefaultRetryErrorInfoProvider, UnsignedFooBlobStreamWithLengthOutput>(options: config.retryStrategyOptions))
        operation.finalizeStep.intercept(position: .before, middleware: ClientRuntime.SignerMiddleware<UnsignedFooBlobStreamWithLengthOutput>())
        operation.deserializeStep.intercept(position: .after, middleware: ClientRuntime.DeserializeMiddleware<UnsignedFooBlobStreamWithLengthOutput>(UnsignedFooBlobStreamWithLengthOutput.httpOutput(from:), UnsignedFooBlobStreamWithLengthOutputError.httpError(from:)))
        operation.deserializeStep.intercept(position: .after, middleware: ClientRuntime.LoggerMiddleware<UnsignedFooBlobStreamWithLengthInput, UnsignedFooBlobStreamWithLengthOutput>(clientLogMode: config.clientLogMode))
        let result = try await operation.handleMiddleware(context: context, input: input, next: client.getHandler())
        return result
    }
"""
        contents.shouldContainOnlyOnce(expected)
    }
    private fun setupTests(smithyFile: String, serviceShapeId: String): TestContext {
        val context = TestContext.initContextFrom(
            listOf(smithyFile),
            serviceShapeId,
            MockHTTPRestJsonProtocolGenerator(),
            { model -> model.defaultSettings(serviceShapeId, "RestJson", "2019-12-16", "Rest Json Protocol") },
            listOf(DefaultClientConfigurationIntegration())
        )

        context.generator.initializeMiddleware(context.generationCtx)
        context.generator.generateProtocolClient(context.generationCtx)
        context.generator.generateSerializers(context.generationCtx)
        context.generationCtx.delegator.flushWriters()
        return context
    }
}

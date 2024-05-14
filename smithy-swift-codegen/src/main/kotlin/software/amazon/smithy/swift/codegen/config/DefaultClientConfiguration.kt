/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0.
 */

package software.amazon.smithy.swift.codegen.config

import software.amazon.smithy.codegen.core.Symbol
import software.amazon.smithy.swift.codegen.ClientRuntimeTypes
import software.amazon.smithy.swift.codegen.SwiftDependency
import software.amazon.smithy.swift.codegen.SwiftTypes
import software.amazon.smithy.swift.codegen.config.ClientConfiguration.Companion.runtimeSymbol
import software.amazon.smithy.swift.codegen.integration.ProtocolGenerator
import software.amazon.smithy.swift.codegen.lang.AccessModifier
import software.amazon.smithy.swift.codegen.lang.Function
import software.amazon.smithy.swift.codegen.lang.FunctionParameter
import software.amazon.smithy.swift.codegen.model.toOptional

class DefaultClientConfiguration : ClientConfiguration {
    override val swiftProtocolName: Symbol
        get() = runtimeSymbol("DefaultClientConfiguration", SwiftDependency.CLIENT_RUNTIME)

    override fun getProperties(ctx: ProtocolGenerator.GenerationContext): Set<ConfigProperty> = setOf(
        ConfigProperty(
            "telemetryProvider",
            ClientRuntimeTypes.Core.TelemetryProvider,
            "ClientRuntime.DefaultTelemetry.provider"
        ),
        ConfigProperty(
            "retryStrategyOptions",
            ClientRuntimeTypes.Core.RetryStrategyOptions,
            "ClientConfigurationDefaults.defaultRetryStrategyOptions"
        ),
        ConfigProperty(
            "clientLogMode",
            ClientRuntimeTypes.Core.ClientLogMode,
            "ClientConfigurationDefaults.defaultClientLogMode"
        ),
        ConfigProperty("endpoint", SwiftTypes.String.toOptional()),
        ConfigProperty(
            "idempotencyTokenGenerator",
            ClientRuntimeTypes.Core.IdempotencyTokenGenerator,
            "ClientConfigurationDefaults.defaultIdempotencyTokenGenerator"
        ),
        ConfigProperty(
            "interceptorProviders",
            ClientRuntimeTypes.Interceptor.Providers,
            "[]",
            accessModifier = AccessModifier.PublicPrivateSet
        ),
    )

    override fun getMethods(ctx: ProtocolGenerator.GenerationContext): Set<Function> = setOf(
        Function(
            name = "addInterceptorProvider",
            renderBody = { writer -> writer.write("self.interceptorProviders.append(provider)") },
            parameters = listOf(
                FunctionParameter.NoLabel("provider", ClientRuntimeTypes.Interceptor.Provider)
            ),
        )
    )
}

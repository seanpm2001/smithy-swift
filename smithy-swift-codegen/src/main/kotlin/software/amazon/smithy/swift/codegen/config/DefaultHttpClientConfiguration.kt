/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0.
 */

package software.amazon.smithy.swift.codegen.config

import software.amazon.smithy.codegen.core.Symbol
import software.amazon.smithy.swift.codegen.ClientRuntimeTypes
import software.amazon.smithy.swift.codegen.SwiftDependency
import software.amazon.smithy.swift.codegen.config.ClientConfiguration.Companion.runtimeSymbol
import software.amazon.smithy.swift.codegen.integration.ProtocolGenerator
import software.amazon.smithy.swift.codegen.lang.AccessModifier
import software.amazon.smithy.swift.codegen.lang.Function
import software.amazon.smithy.swift.codegen.lang.FunctionParameter
import software.amazon.smithy.swift.codegen.model.toOptional

class DefaultHttpClientConfiguration : ClientConfiguration {
    override val swiftProtocolName: Symbol
        get() = runtimeSymbol("DefaultHttpClientConfiguration", SwiftDependency.CLIENT_RUNTIME)

    override fun getProperties(ctx: ProtocolGenerator.GenerationContext): Set<ConfigProperty> = setOf(
        ConfigProperty(
            "httpClientEngine",
            ClientRuntimeTypes.Http.HttpClient,
            "ClientConfigurationDefaults.makeClient(httpClientConfiguration: " +
                "httpClientConfiguration ?? ClientConfigurationDefaults.defaultHttpClientConfiguration" +
                ")"
        ),
        ConfigProperty(
            "httpClientConfiguration",
            ClientRuntimeTypes.Http.HttpClientConfiguration,
            "ClientConfigurationDefaults.defaultHttpClientConfiguration"
        ),
        ConfigProperty("authSchemes", ClientRuntimeTypes.Auth.AuthSchemes.toOptional()),
        ConfigProperty(
            "authSchemeResolver",
            ClientRuntimeTypes.Auth.AuthSchemeResolver,
            "ClientConfigurationDefaults.defaultAuthSchemeResolver"
        ),
        ConfigProperty(
            "httpInterceptorProviders",
            ClientRuntimeTypes.Interceptor.HttpProviders,
            "[]",
            accessModifier = AccessModifier.PublicPrivateSet
        ),
    )

    override fun getMethods(ctx: ProtocolGenerator.GenerationContext): Set<Function> = setOf(
        Function(
            name = "addInterceptorProvider",
            renderBody = { writer -> writer.write("self.httpInterceptorProviders.append(provider)") },
            parameters = listOf(
                FunctionParameter.NoLabel("provider", ClientRuntimeTypes.Interceptor.HttpProvider)
            ),
        )
    )
}

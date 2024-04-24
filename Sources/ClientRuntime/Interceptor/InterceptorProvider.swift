//
// Copyright Amazon.com Inc. or its affiliates.
// All Rights Reserved.
//
// SPDX-License-Identifier: Apache-2.0
//

/// Provides implementations of `Interceptor` for any Request, Response, and Attributes types.
///
/// This can be used to create `Interceptor`s that are generic on their Request/Response/Attributes
/// types, when you don't have access to the exact types until later.
public protocol InterceptorProvider {

    /// Creates an instance of an `Interceptor` implementation, specialized on the given
    /// `RequestType`, `ResponseType`, and `AttributesType`.
    ///
    /// - Returns: The `Interceptor` implementation.
    func create<
        InputType,
        OutputType,
        RequestType: RequestMessage,
        ResponseType: ResponseMessage,
        AttributesType: HasAttributes
    >() -> any Interceptor<InputType, OutputType, RequestType, ResponseType, AttributesType>
}

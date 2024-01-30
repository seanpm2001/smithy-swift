/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0.
 */
import AwsCommonRuntimeKit

public actor HttpResponse: HttpUrlResponse {

    public var headers: Headers
    public var body: ByteStream
    public var statusCode: HttpStatusCode

    public func addHeaders(additionalHeaders: Headers) {
        self.headers.addAll(headers: additionalHeaders)
    }

    public func setBody(newBody: ByteStream) {
        self.body = newBody
    }

    public func setStatusCode(newStatusCode: HttpStatusCode) {
        self.statusCode = newStatusCode
    }

    public init(headers: Headers = .init(), statusCode: HttpStatusCode = .processing, body: ByteStream = .noStream) {
        self.headers = headers
        self.statusCode = statusCode
        self.body = body
    }

    public init(headers: Headers = .init(), body: ByteStream, statusCode: HttpStatusCode) {
        self.body = body
        self.statusCode = statusCode
        self.headers = headers
    }
}

extension HttpResponse {
    public var debugDescriptionWithBody: String {
        return debugDescription + "\nResponseBody: \(body.debugDescription)"
    }
    public var debugDescription: String {
        return "\nStatus Code: \(statusCode.description) \n \(headers)"
    }
}

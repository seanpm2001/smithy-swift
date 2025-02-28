/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0.
 */
import struct Foundation.CharacterSet
import struct Foundation.URLQueryItem
import struct Foundation.URLComponents
import AwsCommonRuntimeKit
// In Linux, Foundation.URLRequest is moved to FoundationNetworking.
#if canImport(FoundationNetworking)
import FoundationNetworking
#else
import struct Foundation.URLRequest
#endif

// we need to maintain a reference to this same request while we add headers
// in the CRT engine so that is why it's a class
public final class SdkHttpRequest: RequestMessage {
    public var body: ByteStream
    public let endpoint: Endpoint
    public let method: HttpMethodType
    private var additionalHeaders: Headers = Headers()
    public var headers: Headers {
        var allHeaders = endpoint.headers ?? Headers()
        allHeaders.addAll(headers: additionalHeaders)
        return allHeaders
    }
    public var trailingHeaders: Headers = Headers()
    public var path: String { endpoint.path }
    public var host: String { endpoint.host }
    public var queryItems: [SDKURLQueryItem]? { endpoint.queryItems }

    public init(method: HttpMethodType,
                endpoint: Endpoint,
                body: ByteStream = ByteStream.noStream) {
        self.method = method
        self.endpoint = endpoint
        self.body = body
    }

    public func toBuilder() -> SdkHttpRequestBuilder {
        let builder = SdkHttpRequestBuilder()
            .withBody(self.body)
            .withMethod(self.method)
            .withHeaders(self.headers)
            .withTrailers(self.trailingHeaders)
            .withPath(self.path)
            .withHost(self.host)
            .withPort(self.endpoint.port)
            .withProtocol(self.endpoint.protocolType ?? .https)
        if let qItems = self.queryItems {
            builder.withQueryItems(qItems)
        }
        return builder
    }

    public func withHeader(name: String, value: String) {
        self.additionalHeaders.add(name: name, value: value)
    }

    public func withoutHeader(name: String) {
        self.additionalHeaders.remove(name: name)
    }

    public func withBody(_ body: ByteStream) {
        self.body = body
    }
}

extension SdkHttpRequest {

    internal var isChunked: Bool {

        // Check if body is a stream
        let isStreamBody: Bool
        switch body {
        case .stream(let stream):
            if stream.isEligibleForAwsChunkedStreaming() {
                isStreamBody = true
            } else {
                isStreamBody = false
            }
        default:
            isStreamBody = false
        }

        let isTransferEncodingChunked = headers.value(for: "Transfer-Encoding")?.lowercased() == "chunked"

        return isStreamBody && isTransferEncodingChunked
    }

    public func toHttpRequest() throws -> HTTPRequest {
        let httpRequest = try HTTPRequest()
        httpRequest.method = method.rawValue
        httpRequest.path = [endpoint.path, endpoint.queryItemString].compactMap { $0 }.joined(separator: "?")
        httpRequest.addHeaders(headers: headers.toHttpHeaders())
        httpRequest.body = isChunked ? nil : StreamableHttpBody(body: body) // body needs to be nil to use writeChunk()
        return httpRequest
    }

    /// Convert the SDK request to a CRT HTTPRequestBase
    /// CRT converts the HTTPRequestBase to HTTP2Request internally if the protocol is HTTP/2
    /// - Returns: the CRT request
    public func toHttp2Request() throws -> HTTPRequestBase {
        let httpRequest = try HTTPRequest()
        httpRequest.method = method.rawValue
        httpRequest.path = [endpoint.path, endpoint.queryItemString].compactMap { $0 }.joined(separator: "?")
        httpRequest.addHeaders(headers: headers.toHttpHeaders())

        // Remove the "Transfer-Encoding" header if it exists since h2 does not support it
        httpRequest.removeHeader(name: "Transfer-Encoding")

        // HTTP2Request used with manual writes hence we need to set the body to nil
        // so that CRT does not write the body for us (we will write it manually)
        httpRequest.body = nil
        return httpRequest
    }
}

public extension URLRequest {
    init(sdkRequest: SdkHttpRequest) async throws {
        // Set URL
        guard let url = sdkRequest.endpoint.url else {
            throw ClientError.dataNotFound("Failed to construct URLRequest due to missing URL.")
        }
        self.init(url: url)
        // Set method type
        self.httpMethod = sdkRequest.method.rawValue
        // Set body, handling any serialization errors
        do {
            let data = try await sdkRequest.body.readData()
            sdkRequest.body = .data(data)
            self.httpBody = data
        } catch {
            throw ClientError.serializationFailed("Failed to construct URLRequest due to HTTP body conversion failure.")
        }
        // Set headers
        sdkRequest.headers.headers.forEach { header in
            header.value.forEach { value in
                self.addValue(value, forHTTPHeaderField: header.name)
            }
        }
    }
}

extension SdkHttpRequest: CustomDebugStringConvertible, CustomStringConvertible {

    public var debugDescriptionWithBody: String {
        return debugDescription + "\nRequestBody: \(body.debugDescription)"
    }

    public var debugDescription: String {
        description
    }

    public var description: String {
        let method = method.rawValue.uppercased()
        let protocolType = endpoint.protocolType ?? ProtocolType.https
        let query = String(describing: queryItems)
        return "\(method) \(protocolType):\(endpoint.port) \n Path: \(endpoint.path) \n \(headers) \n \(query)"
    }
}

extension SdkHttpRequestBuilder {

    /// Update the builder with the values from the CRT request
    /// - Parameters:
    ///   - crtRequest: the CRT request, this can be either a `HTTPRequest` or a `HTTP2Request`
    ///   - originalRequest: the SDK request that is used to hold the original values
    /// - Returns: the builder
    public func update(from crtRequest: HTTPRequestBase, originalRequest: SdkHttpRequest) -> SdkHttpRequestBuilder {
        headers = convertSignedHeadersToHeaders(crtRequest: crtRequest)
        methodType = originalRequest.method
        host = originalRequest.host
        if let crtRequest = crtRequest as? HTTPRequest, let components = URLComponents(string: crtRequest.path) {
            path = components.percentEncodedPath
            queryItems = components.percentEncodedQueryItems?.map { SDKURLQueryItem(name: $0.name, value: $0.value) }
                ?? [SDKURLQueryItem]()
        } else if crtRequest as? HTTP2Request != nil {
            assertionFailure("HTTP2Request not supported")
        } else {
            assertionFailure("Unknown request type")
        }
        return self
    }

    func convertSignedHeadersToHeaders(crtRequest: HTTPRequestBase) -> Headers {
        return Headers(httpHeaders: crtRequest.getHeaders())
    }
}

public class SdkHttpRequestBuilder: RequestMessageBuilder {

    required public init() {}

    var headers: Headers = Headers()
    var methodType: HttpMethodType = .get
    var host: String = ""
    var path: String = "/"
    var body: ByteStream = .noStream
    var queryItems: [SDKURLQueryItem]?
    var port: Int16 = 443
    var protocolType: ProtocolType = .https
    var trailingHeaders: Headers = Headers()

    public var currentQueryItems: [SDKURLQueryItem]? {
        return queryItems
    }

    // We follow the convention of returning the builder object
    // itself from any configuration methods, and by adding the
    // @discardableResult attribute we won't get warnings if we
    // don't end up doing any chaining.
    @discardableResult
    public func withHeaders(_ value: Headers) -> SdkHttpRequestBuilder {
        self.headers.addAll(headers: value)
        return self
    }

    @discardableResult
    public func withHeader(name: String, value: String) -> SdkHttpRequestBuilder {
        self.headers.add(name: name, value: value)
        return self
    }

    @discardableResult
    public func updateHeader(name: String, value: [String]) -> SdkHttpRequestBuilder {
        self.headers.update(name: name, value: value)
        return self
    }

    @discardableResult
    public func withTrailers(_ value: Headers) -> SdkHttpRequestBuilder {
        self.trailingHeaders.addAll(headers: value)
        return self
    }

    @discardableResult
    public func updateTrailer(name: String, value: [String]) -> SdkHttpRequestBuilder {
        self.trailingHeaders.update(name: name, value: value)
        return self
    }

    @discardableResult
    public func withMethod(_ value: HttpMethodType) -> SdkHttpRequestBuilder {
        self.methodType = value
        return self
    }

    @discardableResult
    public func withHost(_ value: String) -> SdkHttpRequestBuilder {
        self.host = value
        return self
    }

    @discardableResult
    public func withPath(_ value: String) -> SdkHttpRequestBuilder {
        self.path = value
        return self
    }

    @discardableResult
    public func withBody(_ value: ByteStream) -> SdkHttpRequestBuilder {
        self.body = value
        return self
    }

    @discardableResult
    public func withQueryItems(_ value: [SDKURLQueryItem]) -> SdkHttpRequestBuilder {
        self.queryItems = self.queryItems ?? []
        self.queryItems?.append(contentsOf: value)
        return self
    }

    @discardableResult
    public func withQueryItem(_ value: SDKURLQueryItem) -> SdkHttpRequestBuilder {
        withQueryItems([value])
    }

    @discardableResult
    public func withPort(_ value: Int16) -> SdkHttpRequestBuilder {
        self.port = value
        return self
    }

    @discardableResult
    public func withProtocol(_ value: ProtocolType) -> SdkHttpRequestBuilder {
        self.protocolType = value
        return self
    }

    public func build() -> SdkHttpRequest {
        let endpoint = Endpoint(host: host,
                                path: path,
                                port: port,
                                queryItems: queryItems,
                                protocolType: protocolType,
                                headers: headers)
        return SdkHttpRequest(method: methodType,
                              endpoint: endpoint,
                              body: body)
    }
}

extension HTTPRequestBase {
    public var signature: String? {
        let authHeader = getHeaderValue(name: "Authorization")
        guard let signatureSequence = authHeader?.split(separator: "=").last else {
            return nil
        }
        return String(signatureSequence)
    }
}

extension SdkHttpRequestBuilder {
    public var signature: String? {
        let authHeader = self.headers.value(for: "Authorization")
        guard let signatureSequence = authHeader?.split(separator: "=").last else {
            return nil
        }
        return String(signatureSequence)
    }
}

//
// Copyright Amazon.com Inc. or its affiliates.
// All Rights Reserved.
//
// SPDX-License-Identifier: Apache-2.0
//

import Foundation
import XCTest
import SmithyTestUtil
@testable import ClientRuntime

final class MiddlewareStackIntegrationTests: XCTestCase {

    func testFullBlownOperationRequestWithClientHandler() async throws {
        let httpClientConfiguration = HttpClientConfiguration()
        let clientEngine = CRTClientEngine()
        let httpClient = SdkHttpClient(engine: clientEngine, config: httpClientConfiguration)

        let builtContext = HttpContextBuilder()
            .withMethod(value: .get)
            .withPath(value: "/headers")
            .withEncoder(value: JSONEncoder())
            .withDecoder(value: JSONDecoder())
            .withOperation(value: "Test Operation")
            .build()
        var stack = OperationStack<MockInput, MockOutput, MockMiddlewareError>(id: "Test Operation")
        stack.serializeStep.intercept(position: .after,
                                      middleware: MockSerializeMiddleware(id: "TestMiddleware", headerName: "TestName", headerValue: "TestValue"))
        stack.deserializeStep.intercept(position: .after,
                                        middleware: MockDeserializeMiddleware<MockOutput, MockMiddlewareError>(id: "TestDeserializeMiddleware"))

        let result = try await stack.handleMiddleware(context: builtContext, input: MockInput(), next: httpClient.getHandler())

        XCTAssert(result.value == 200)
        XCTAssert(result.headers.headers.contains(where: { (header) -> Bool in
            header.name == "Content-Length"
        }))
    }
}

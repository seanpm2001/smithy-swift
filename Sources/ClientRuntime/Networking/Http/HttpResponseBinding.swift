//
// Copyright Amazon.com Inc. or its affiliates.
// All Rights Reserved.
//
// SPDX-License-Identifier: Apache-2.0
//

import typealias SmithyReadWrite.DocumentReadingClosure
import typealias SmithyReadWrite.ReadingClosure

/// The interface for creating the response object that results from a successful HTTP/HTTPS response.
public typealias HTTPResponseBinding<T> = (HttpResponse) async throws -> T

enum HTTPBindings {

    static func makeOutput<T, Reader>(readingClosure: @escaping ReadingClosure<T, Reader>, documentReadingClosure: @escaping DocumentReadingClosure<T, Reader>) async throws -> HTTPResponseBinding<T> {
        return { response in
            return try documentReadingClosure(try await response.body.readData() ?? Data(), readingClosure)
        }
    }
}

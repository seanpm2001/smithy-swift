//
// Copyright Amazon.com Inc. or its affiliates.
// All Rights Reserved.
//
// SPDX-License-Identifier: Apache-2.0
//

import typealias SmithyReadWrite.DocumentReadingClosure
import typealias SmithyReadWrite.ReadingClosure

/// The interface for creating the response object that results from a HTTP/HTTPS error response.
///
/// Value returned may be of any type that is a Swift `Error`.
public typealias HTTPResponseErrorBinding = (HttpResponse) async throws -> Error

extension HTTPBindings {

    static func makeError<E, Reader>(readingClosure: @escaping ReadingClosure<E, Reader>, documentReadingClosure: @escaping DocumentReadingClosure<E, Reader>) async throws -> HTTPResponseErrorBinding {
        return { response in
            return try documentReadingClosure(try await response.body.readData() ?? Data(), readingClosure)
        }
    }
}

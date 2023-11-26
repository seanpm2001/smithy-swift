//
// Copyright Amazon.com Inc. or its affiliates.
// All Rights Reserved.
//
// SPDX-License-Identifier: Apache-2.0
//

import struct Foundation.Data
import typealias SmithyReadWrite.ReadingClosure

public enum DocumentReader {

    static func read<T>(_ data: Data, rootNodeInfo: NodeInfo, readingClosure: ReadingClosure<T, Reader>) throws -> T {
        return try readingClosure(Reader(rootNodeInfo: rootNodeInfo))
    }
}

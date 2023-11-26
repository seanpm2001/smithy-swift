//
// Copyright Amazon.com Inc. or its affiliates.
// All Rights Reserved.
//
// SPDX-License-Identifier: Apache-2.0
//

import struct Foundation.Date
import typealias SmithyReadWrite.ReadingClosure
import enum SmithyTimestamps.TimestampFormat

public extension String {

    static func readingClosure(from reader: Reader) throws -> String {
        try reader.read()
    }
}

public extension RawRepresentable where RawValue == Int {

    static func readingClosure(from reader: Reader) throws -> Self {
        try reader.read()
    }
}

public extension RawRepresentable where RawValue == String {

    static func readingClosure(from reader: Reader) throws -> Self {
        try reader.read()
    }
}

public extension Bool {

    static func readingClosure(from reader: Reader) throws -> Bool {
        try reader.read()
    }
}

public extension Int {

    static func readingClosure(from reader: Reader) throws -> Int {
        try reader.read()
    }
}

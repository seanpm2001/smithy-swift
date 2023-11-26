//
// Copyright Amazon.com Inc. or its affiliates.
// All Rights Reserved.
//
// SPDX-License-Identifier: Apache-2.0
//

import SmithyReadWrite

public class JSONWriter {
    private let encoder: any RequestEncoder
    var data = Data()

    init(encoder: any RequestEncoder) {
        self.encoder = encoder
    }

    func encode<T: Encodable>(_ value: T) throws {
        self.data = try encoder.encode(value)
    }
}

public class JSONReader {
    private let decoder: any ResponseDecoder

    init(decoder: any ResponseDecoder) {
        self.decoder = decoder
    }

    func decode<T: Decodable>(_ data: Data) throws -> T {
        try decoder.decode(responseBody: data)
    }
}

public enum JSONReadWrite {

    public static func documentWritingClosure<T: Encodable>(
        encoder: RequestEncoder
    ) -> DocumentWritingClosure<T, JSONWriter> {
        return { value, writingClosure in
            let jsonEncoder = JSONWriter(encoder: encoder)
            try writingClosure(value, jsonEncoder)
            return jsonEncoder.data
        }
    }

    public static func documentReadingClosure<T: Decodable>(
        decoder: ResponseDecoder
    ) -> DocumentReadingClosure<T, JSONReader> {
        return { data, readingClosure in
            let jsonDecoder = JSONReader(decoder: decoder)
            return try readingClosure(jsonDecoder)
        }
    }

    public static func writingClosure<T: Encodable>() -> WritingClosure<T, JSONWriter> {
        return { value, writer in
            try writer.encode(value)
        }
    }
}

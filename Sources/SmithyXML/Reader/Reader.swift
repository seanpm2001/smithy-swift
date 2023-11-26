//
// Copyright Amazon.com Inc. or its affiliates.
// All Rights Reserved.
//
// SPDX-License-Identifier: Apache-2.0
//

import typealias SmithyReadWrite.ReadingClosure
import struct Foundation.Date
import struct Foundation.Data
import enum SmithyTimestamps.TimestampFormat

public class Reader {
    public var children: [Reader] = []
    public weak var parent: Reader?
    public let nodeInfo: NodeInfo
    let nodeInfoPath: [NodeInfo]

    // MARK: - init & deinit

    /// Used by the `DocumentWriter` to begin serialization of a model to XML.
    /// - Parameter rootNodeInfo: The node info for the root XML node.
    init(rootNodeInfo: NodeInfo) {
        self.nodeInfo = rootNodeInfo
        self.nodeInfoPath = [rootNodeInfo]
    }

    private init(nodeInfo: NodeInfo, nodeInfoPath: [NodeInfo], parent: Reader?) {
        self.nodeInfo = nodeInfo
        self.nodeInfoPath = nodeInfoPath
        self.parent = parent
    }

    // MARK: - creating and detaching readers for subelements

    public subscript(_ nodeInfo: NodeInfo) -> Reader {
        let namespace = nodeInfoPath.compactMap { $0.namespace }.contains(nodeInfo.namespace) ? nil : nodeInfo.namespace
        let newNodeInfo = NodeInfo(nodeInfo.name, location: nodeInfo.location, namespace: namespace)
        let newChild = Reader(nodeInfo: newNodeInfo, nodeInfoPath: nodeInfoPath + [newNodeInfo], parent: self)
        addChild(newChild)
        return newChild
    }

    /// Detaches this reader from its parent.  Typically used when this reader no longer
    /// belongs in the tree, either because its data is nil or its contents were flattened
    /// into its parents.
    public func detach() {
        parent?.children.removeAll { $0 === self }
        parent = nil
    }

    // MARK: - Reading values

    public func read<T>(readingClosure: ReadingClosure<T, Reader>) throws -> T {
        return try readingClosure(self)
    }

    public func read() throws -> String {
        // TODO: implement me
        return ""
    }

    public func read() throws -> Int {
        // TODO: implement me
        return 0
    }

    public func read() throws -> Bool {
        // TODO: implement me
        return false
    }

    public func write() throws -> Data? {
        // TODO: implement me
        return Data()
    }

    public func readTimestamp(format: TimestampFormat) throws -> Date? {
        // TODO: implement me
        return Date()
    }

    public func read<T: RawRepresentable>() throws -> T where T.RawValue == Int {
        // TODO: implement me
        return T(rawValue: 0)!
    }

    public func read<T: RawRepresentable>() throws -> T where T.RawValue == String {
        // TODO: implement me
        return T(rawValue: "")!
    }

    public func readMap<T>(
        valueReadingClosure: ReadingClosure<T, Reader>,
        keyNodeInfo: NodeInfo,
        valueNodeInfo: NodeInfo,
        isFlattened: Bool
    ) throws -> [String: T]? {
        // TODO: implement me
        return [:]
    }

    public func readList<T>(
        memberReadingClosure: ReadingClosure<T, Reader>,
        memberNodeInfo: NodeInfo,
        isFlattened: Bool
    ) throws -> [T]? {
        // TODO: implement me
        return []
    }


    // MARK: - Private methods

    private func addChild(_ child: Reader) {
        children.append(child)
        child.parent = self
    }
}

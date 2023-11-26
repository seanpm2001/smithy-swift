/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0.
 */

package software.amazon.smithy.swift.codegen.integration.serde.xml

import software.amazon.smithy.model.shapes.ListShape
import software.amazon.smithy.model.shapes.MapShape
import software.amazon.smithy.model.shapes.MemberShape
import software.amazon.smithy.model.shapes.Shape
import software.amazon.smithy.model.shapes.StructureShape
import software.amazon.smithy.model.shapes.TimestampShape
import software.amazon.smithy.model.shapes.UnionShape
import software.amazon.smithy.model.traits.TimestampFormatTrait
import software.amazon.smithy.model.traits.XmlFlattenedTrait
import software.amazon.smithy.swift.codegen.SwiftWriter
import software.amazon.smithy.swift.codegen.integration.ProtocolGenerator
import software.amazon.smithy.swift.codegen.integration.serde.json.TimestampUtils
import software.amazon.smithy.swift.codegen.integration.serde.readwrite.ReadingClosureUtils
import software.amazon.smithy.swift.codegen.model.getTrait
import software.amazon.smithy.swift.codegen.model.hasTrait

class MemberShapeDecodeXMLGenerator(
    private val ctx: ProtocolGenerator.GenerationContext,
    private val writer: SwiftWriter,
    private val defaultTimestampFormat: TimestampFormatTrait.Format
) {
    private val nodeInfoUtils = NodeInfoUtils(ctx, writer)
    private val readingClosureUtils = ReadingClosureUtils(ctx, writer)
    fun render(member: MemberShape, unionMember: Boolean) {
        val targetShape = ctx.model.expectShape(member.target)
        val readExp = when (targetShape) {
            is StructureShape, is UnionShape -> {
                renderStructOrUnionExp(member, targetShape)
            }
            is MapShape -> {
                renderMapExp(member, targetShape)
            }
            is ListShape -> {
                renderListExp(member, targetShape)
            }
            is TimestampShape -> {
                renderTimestampExp(member, targetShape)
            }
            else -> {
                renderMemberExp(member)
            }
        }
        val memberName = ctx.symbolProvider.toMemberName(member)
        if (unionMember) {
            writer.write("return .\$L(\$L)", memberName, readExp)
        } else {
            writer.write("value.\$L = \$L", memberName, readExp)
        }
    }

    fun renderStructOrUnionExp(memberShape: MemberShape, shape: Shape): String {
        val propertyNodeInfo = nodeInfoUtils.nodeInfo(memberShape)
        val readingClosure = readingClosureUtils.readingClosure(memberShape)
        return writer.format("try reader[\$L].read(readingClosure: \$L)", propertyNodeInfo, readingClosure)
    }

    fun renderListExp(memberShape: MemberShape, listShape: ListShape): String {
        val nodeInfo = nodeInfoUtils.nodeInfo(memberShape)
        val memberReadingClosure = readingClosureUtils.readingClosure(listShape.member)
        val memberNodeInfo = nodeInfoUtils.nodeInfo(listShape.member)
        val isFlattened = memberShape.hasTrait<XmlFlattenedTrait>()
        return writer.format(
            "try reader[\$L].readList(memberReadingClosure: \$L, memberNodeInfo: \$L, isFlattened: \$L)",
            nodeInfo,
            memberReadingClosure,
            memberNodeInfo,
            isFlattened
        )
    }

    fun renderMapExp(member: MemberShape, mapShape: MapShape): String {
        val mapNodeInfo = nodeInfoUtils.nodeInfo(member)
        val valueReadingClosure = ReadingClosureUtils(ctx, writer).readingClosure(mapShape.value)
        val keyNodeInfo = nodeInfoUtils.nodeInfo(mapShape.key)
        val valueNodeInfo = nodeInfoUtils.nodeInfo(mapShape.value)
        val isFlattened = member.hasTrait<XmlFlattenedTrait>()
        return writer.format(
            "try writer[\$L].readMap(valueWritingClosure: \$L, keyNodeInfo: \$L, valueNodeInfo: \$L, isFlattened: \$L)",
            mapNodeInfo,
            valueReadingClosure,
            keyNodeInfo,
            valueNodeInfo,
            isFlattened
        )
    }

    fun renderTimestampExp(memberShape: MemberShape, timestampShape: TimestampShape): String {
        val timestampNodeInfo = NodeInfoUtils(ctx, writer).nodeInfo(memberShape)
        val memberTimestampFormatTrait = memberShape.getTrait<TimestampFormatTrait>()
        val swiftTimestampFormatCase = TimestampUtils.timestampFormat(memberTimestampFormatTrait, timestampShape)
        return writer.format("try reader[\$L].readTimestamp(format: \$L)",
            timestampNodeInfo,
            swiftTimestampFormatCase
        )
    }

    fun renderMemberExp(memberShape: MemberShape): String {
        val propertyNodeInfo = nodeInfoUtils.nodeInfo(memberShape)
        return writer.format("try reader[\$L].read()", propertyNodeInfo)
    }
}

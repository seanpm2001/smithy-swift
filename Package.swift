// swift-tools-version:5.5

import PackageDescription
import class Foundation.ProcessInfo

var includeIntegrationTests = ProcessInfo.processInfo.environment["SMITHY_SWIFT_INCLUDE_INTEGRATION_TESTS"] != nil

var products: [Product] = [
    .library(name: "ClientRuntime", targets: ["ClientRuntime"]),
    .library(name: "SmithyTestUtil", targets: ["SmithyTestUtil"])
]

if includeIntegrationTests {
    products.append(
        .library(name: "SmithyIntegrationTests", targets: ["SmithyIntegrationTests"])
    )
}

var targets: [Target] = [
        .target(
            name: "ClientRuntime",
            dependencies: [
                .product(name: "AwsCommonRuntimeKit", package: "aws-crt-swift"),
                .product(name: "Logging", package: "swift-log"),
                .product(name: "XMLCoder", package: "XMLCoder")
            ]
        ),
        .testTarget(
            name: "ClientRuntimeTests",
            dependencies: ["ClientRuntime", "SmithyTestUtil"]
        ),
        .target(
            name: "SmithyTestUtil",
            dependencies: ["ClientRuntime"]
        ),
        .testTarget(
            name: "SmithyTestUtilTests",
            dependencies: ["SmithyTestUtil"]
        )
]

if includeIntegrationTests {
    targets.append(
        .testTarget(
            name: "SmithyIntegrationTests",
            dependencies: ["ClientRuntime", "SmithyTestUtil"]
        )
    )
}


let package = Package(
    name: "smithy-swift",
    platforms: [
        .macOS(.v10_15),
        .iOS(.v13)
    ],
    products: products,
    dependencies: [
        .package(url: "https://github.com/awslabs/aws-crt-swift.git", .exact("0.9.0")),
        .package(url: "https://github.com/apple/swift-log.git", from: "1.0.0"),
        .package(url: "https://github.com/MaxDesiatov/XMLCoder.git", from: "0.13.0")
    ],
    targets: targets
)

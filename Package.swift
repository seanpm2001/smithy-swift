// swift-tools-version:5.7

import PackageDescription

// Define libxml2 only on Linux, since it causes warnings
// about "pkgconfig not found" on Mac
#if os(Linux)
let libXML2DependencyOrNil: Target.Dependency? = "libxml2"
let libXML2TargetOrNil: Target? = Target.systemLibrary(
    name: "libxml2",
    pkgConfig: "libxml-2.0",
    providers: [
        .apt(["libxml2 libxml2-dev"]),
        .yum(["libxml2 libxml2-devel"])
    ]
)
#else
let libXML2DependencyOrNil: Target.Dependency? = nil
let libXML2TargetOrNil: Target? = nil
#endif

let package = Package(
    name: "smithy-swift",
    platforms: [
        .macOS(.v10_15),
        .iOS(.v13),
        .tvOS(.v13),
        .watchOS(.v6)
    ],
    products: [
        .library(name: "ClientRuntime", targets: ["ClientRuntime"]),
        .library(name: "SmithyReadWrite", targets: ["SmithyReadWrite"]),
        .library(name: "SmithyXML", targets: ["SmithyXML"]),
        .library(name: "SmithyTestUtil", targets: ["SmithyTestUtil"]),
    ],
    dependencies: [
        .package(url: "https://github.com/awslabs/aws-crt-swift.git", exact: "0.30.0"),
        .package(url: "https://github.com/apple/swift-log.git", from: "1.0.0"),
    ],
    targets: [
        .target(
            name: "ClientRuntime",
            dependencies: [
                "SmithyRetriesAPI",
                "SmithyRetries",
                "SmithyXML",
                .product(name: "AwsCommonRuntimeKit", package: "aws-crt-swift"),
                .product(name: "Logging", package: "swift-log"),
            ],
            resources: [
                .copy("PrivacyInfo.xcprivacy")
            ]
        ),
        .target(
            name: "SmithyRetriesAPI"
        ),
        .target(
            name: "SmithyRetries",
            dependencies: [
                .product(name: "AwsCommonRuntimeKit", package: "aws-crt-swift"),
            ]
        ),
        .testTarget(
            name: "SmithyRetriesTests",
            dependencies: ["ClientRuntime", "SmithyRetriesAPI", "SmithyRetries"]
        ),
        .target(name: "SmithyReadWrite"),
        .target(
            name: "SmithyXML",
            dependencies: [
                "SmithyReadWrite",
                "SmithyTimestamps",
                libXML2DependencyOrNil
            ].compactMap { $0 }
        ),
        libXML2TargetOrNil,
        .target(
            name: "SmithyTimestamps"
        ),
        .target(
            name: "SmithyTestUtil",
            dependencies: ["ClientRuntime"]
        ),
        .testTarget(
            name: "ClientRuntimeTests",
            dependencies: ["ClientRuntime", "SmithyTestUtil"]
        ),
        .testTarget(
            name: "SmithyXMLTests",
            dependencies: ["SmithyXML", "ClientRuntime"]
        ),
        .testTarget(
            name: "SmithyTimestampsTests",
            dependencies: ["SmithyTimestamps"]
        ),
        .testTarget(
            name: "SmithyTestUtilTests",
            dependencies: ["SmithyTestUtil"]
        ),
    ].compactMap { $0 }
)

func addTestServiceTargets() {
    package.targets += [
        .target(
            name: "WeatherSDK",
            dependencies: ["SmithyTestUtil", "ClientRuntime"]
        ),
        .testTarget(
            name: "WeatherSDKTests",
            dependencies: ["WeatherSDK"]
        )
    ]
}

addTestServiceTargets()

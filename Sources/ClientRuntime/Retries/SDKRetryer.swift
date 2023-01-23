//
// Copyright Amazon.com Inc. or its affiliates.
// All Rights Reserved.
//
// SPDX-License-Identifier: Apache-2.0
//
import AwsCommonRuntimeKit

public class SDKRetryer: Retryer {
    let crtRetryStrategy: AwsCommonRuntimeKit.RetryStrategy
    private let sharedDefaultIO = SDKDefaultIO.shared

    public init(options: RetryOptions = RetryOptions()) throws {
        self.crtRetryStrategy = try AwsCommonRuntimeKit.RetryStrategy(
            options: options,
            eventLoopGroup: sharedDefaultIO.eventLoopGroup
        )
    }

    public func acquireToken(partitionId: String) async throws -> RetryToken {
        let token = try await crtRetryStrategy.acquireToken(partitionId: partitionId)
        return RetryToken(crtToken: token)
    }

    public func scheduleRetry(token: RetryToken, error: RetryError) async throws -> RetryToken {
        let token = try await crtRetryStrategy.scheduleRetry(token: token.crtToken, errorType: error.toCRTType())
        return RetryToken(crtToken: token)
    }

    public func recordSuccess(token: RetryToken) {
        crtRetryStrategy.recordSuccess(token: token.crtToken)
    }

    @available(*, deprecated, message: "This function will be removed soon.")
    public func releaseToken(token: RetryToken) {
    }

    public func isErrorRetryable<E>(error: SdkError<E>) -> Bool {
<<<<<<< Updated upstream
        switch error {
        case .client(let clientError, _):
            switch clientError {
            case .networkError, .crtError:
                return true
            default:
                return false
            }
        case .service(let serviceError, let httpResponse):
            if httpResponse.headers.exists(name: "x-amz-retry-after") {
                return true
            }

            if let serviceError = serviceError as? ServiceError {
                return serviceError._retryable
            }

            if httpResponse.statusCode.isRetryable {
                return true
            }
            return false
        case .unknown:
            return false
=======
        switch getErrorType(error: error) {
        case .transient, .throttling: return true
        case .clientError, .serverError: return false
>>>>>>> Stashed changes
        }
    }

    public func getErrorType<E>(error: SdkError<E>) -> RetryError {
        // Errors with these codes are to be considered throttling errors per the Retries 2.0 SEP.
        let retryableErrorCodes = [
            "Throttling",
            "ThrottlingException",
            "ThrottledException",
            "RequestThrottledException",
            "TooManyRequestsException",
            "ProvisionedThroughputExceededException",
            "TransactionInProgressException",
            "RequestLimitExceeded",
            "BandwidthLimitExceeded",
            "LimitExceededException",
            "RequestThrottled",
            "SlowDown",
            "PriorRequestNotComplete",
            "EC2ThrottledException"
        ]
        // Errors with these codes are to be considered transient errors per the Retries 2.0 SEP.
        let transientErrorCodes = [
            "RequestTimeout",
            "InternalError",
            "RequestTimeoutException"
        ]
        if let errorCode = (error as? CodedError)?.code {
            if retryableErrorCodes.contains(errorCode) {
                return .throttling
            } else if transientErrorCodes.contains(errorCode) {
                return .transient
            }
        }
        switch error {
        case .client(let clientError, _):
            if case ClientError.crtError = clientError {
                return .transient
            }
            return .clientError

        case .service(let serviceError, let httpResponse):
            if httpResponse.headers.exists(name: "x-amz-retry-after") {
                return .serverError
            }

            if let serviceError = serviceError as? ServiceError {
                if serviceError._isThrottling {
                    return .throttling
                }
                return .serverError
            }

            if httpResponse.statusCode.isRetryable {
                return .transient
            }

            return .serverError
        case .unknown:
            return .clientError
        }
    }
}

extension AwsCommonRuntimeKit.RetryStrategy {
    convenience init(options: RetryOptions, eventLoopGroup: EventLoopGroup) throws {
       try self.init(
            eventLoopGroup: eventLoopGroup,
            initialBucketCapacity: options.initialBucketCapacity,
            maxRetries: options.maxRetries,
            backOffScaleFactor: options.backOffScaleFactor,
            jitterMode: options.jitterMode.toCRTType(),
            generateRandom: nil // we should pass in the options.generateRandom but currently
                                // it fails since the underlying closure is a c closure
        )
    }
}

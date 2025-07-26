## 8. Development Workflow

This is a standalone excerpt from the [Architecture and Development Guide](../README_Architecture_And_Development_Guide.md).

*Source: `README_How_To_Add_Features.md`*

[Back to TOC](#table-of-contents)

### 8.1 Adding New Features

The recommended workflow for adding new features follows a methodical progression from domain modeling to testing. Let's walk through each step with detailed examples, using a feedback submission feature as an illustration.

#### 1. Define Domain Models and Error Types

Start by defining your domain objects, value classes, and error types in the model package:

```scala
// Step 1: Domain models and error types
// Example pattern from model/user/feedback/package.scala
package com.crib.bills.ouroboros
package model.user

package object feedback {
  // Value classes for strong typing
  case class FeedbackContents(value: String) extends AnyVal
  case class Id(value: java.util.UUID) extends AnyVal
  case class FeedbackSubmissionTimestamp(value: java.time.Instant)
  
  // ADT for the feedback category
  sealed trait FeedbackType extends Product with Serializable
  object FeedbackType {
    case object Bug extends FeedbackType
    case object FeatureRequest extends FeedbackType
    case object General extends FeedbackType
  }
  
  // Domain entity
  case class FeedbackRecord(
    feedbackId: Id, 
    userId: model.user.Id, 
    category: FeedbackType, 
    contents: FeedbackContents, 
    timestamp: FeedbackSubmissionTimestamp
  )

  // Domain-specific error types
  class Error(cause: Throwable) extends model.Error(cause)
  object Error {
    case class UnspecifiedError(cause: Throwable) extends Error(cause)
    case class FeedbackNotFound(feedbackId: Id, traceId: model.trace.Id) extends Error(
      new Throwable(s"Could not find feedback with id `$feedbackId`; trace: `$traceId`")
    )
    case class ValidationError(details: String, traceId: model.trace.Id) extends Error(
      new Throwable(s"Feedback validation failed: $details; trace: `$traceId`")
    )
    
    // Error builder for integration with effect system
    given errorBuilder: model.status.DomainErrorBuilder[feedback.Error] with {
      def fromThrowable(cause: Throwable) = UnspecifiedError(cause)
    }
  }
}
```

#### 2. Create Service Capability Traits

Define the service interface with its capabilities. Remember to follow the effect system pattern with `Sequencer` and `ErrorChannel`:

```scala
// Step 2: Service capability trait
// Example based on pattern from apps/services/document/compatibility/Service.scala
package com.crib.bills.ouroboros
package apps.services.feedback

import cats.{MonadError, Traverse}

import model.{status, trace, user}
import user.feedback

trait FeedbackService[Sequencer[_]] {
  // Self-alias for better discoverability
  protected def feedbackService: FeedbackService[Sequencer] = this
  
  def submitFeedback[ErrorChannel[_]](
    user: model.user.Id,
    category: feedback.FeedbackType,
    contents: feedback.FeedbackContents
  )(using 
    errorChannel: MonadError[ErrorChannel, feedback.Error] & Traverse[ErrorChannel],
    traceId: trace.Id
  ): Sequencer[ErrorChannel[status.Ok]]
  
  def retrieveFeedback[ErrorChannel[_]](
    feedbackId: feedback.Id
  )(using
    errorChannel: MonadError[ErrorChannel, feedback.Error] & Traverse[ErrorChannel],
    traceId: trace.Id
  ): Sequencer[ErrorChannel[feedback.FeedbackRecord]]
}
```

#### 3. Create Repository Contracts

Define the repository interface that handles data access:

```scala
// Step 3: Repository contract
// Example based on pattern from model/document/repo/Repo.scala
package com.crib.bills.ouroboros
package model.user.feedback

import model.{trace, user}

trait Repo[Sequencer[_]] {
  def submitFeedback[ErrorChannel[_]](
    userId: user.Id,
    category: feedback.FeedbackType,
    contents: feedback.FeedbackContents
  )(using 
    errorChannel: cats.MonadError[ErrorChannel, feedback.Error] & cats.Traverse[ErrorChannel],
    traceId: trace.Id
  ): Sequencer[ErrorChannel[feedback.Id]]
  
  def retrieveFeedback[ErrorChannel[_]](
    id: feedback.Id
  )(using 
    errorChannel: cats.MonadError[ErrorChannel, feedback.Error] & cats.Traverse[ErrorChannel],
    traceId: trace.Id
  ): Sequencer[ErrorChannel[feedback.FeedbackRecord]]
}
```

#### 4. Implement In-Memory Repository

Create an in-memory implementation for rapid development and testing:

```scala
// Step 4: In-memory repository implementation
// Example based on pattern from factories/user/feedback/InMemoryImpl.scala
// Follows similar structure to InMemory implementations used in document compatibility tests
package com.crib.bills.ouroboros
package factories.user.feedback

import cats.effect
import cats.syntax.all.*
import com.github.f4b6a3.uuid.UuidCreator

import model.{logger, trace, user}
import user.feedback
import feedback.Error.errorBuilder
import typeclasses.ops.{attemptAndAbortSequenceOnError}

class InMemoryRepo[Sequencer[_]](
  memory: effect.Ref[Sequencer, Map[feedback.Id, feedback.FeedbackRecord]]
)(using 
  sequencer: effect.Sync[Sequencer], 
  loggerFactory: logger.Factory[Sequencer]
) extends feedback.Repo[Sequencer] with logger.DefaultLogName {

  override def submitFeedback[ErrorChannel[_]](
    userId: user.Id,
    category: feedback.FeedbackType,
    contents: feedback.FeedbackContents
  )(using
    errorChannel: cats.MonadError[ErrorChannel, feedback.Error] & cats.Traverse[ErrorChannel],
    traceId: trace.Id
  ): Sequencer[ErrorChannel[feedback.Id]] = {
    (for {
      logger <- loggerFactory.create.attemptAndAbortSequenceOnError
      _ <- logger.info(s"Submitting feedback for user `$userId`; trace: $traceId").attemptAndAbortSequenceOnError
      
      // Generate a new ID for the feedback
      newId <- sequencer.delay(feedback.Id(UuidCreator.getTimeOrderedEpoch())).attemptAndAbortSequenceOnError
      timestamp = feedback.FeedbackSubmissionTimestamp(java.time.Instant.now())
      
      // Create record and store in memory
      record = feedback.FeedbackRecord(newId, userId, category, contents, timestamp)
      _ <- memory.update(_ + (newId -> record)).attemptAndAbortSequenceOnError
      
      _ <- logger.info(s"Feedback submitted successfully with id `$newId`; trace: $traceId").attemptAndAbortSequenceOnError
    } yield newId).value
  }
  
  // Implementation of retrieveFeedback...
}
```

#### 5. Implement Service Using Repository

Implement the service using the repository:

```scala
// Step 5: Service implementation
// Example based on pattern from apps/services/document/compatibility/Impl.scala
// Uses modular helper methods with proper error handling, similar to the docxToHtml pattern
package com.crib.bills.ouroboros
package apps.services.feedback

import cats.syntax.all.*
import model.{status, trace, user}
import user.feedback
import feedback.Error.errorBuilder
import typeclasses.ops.{abortSequenceOnError, attemptAndAbortSequenceOnError}

trait Impl[Sequencer[_]] extends FeedbackService[Sequencer] with model.logger.DefaultLogName {
  // Explicit dependency injection
  protected def feedbackRepo: model.user.feedback.Repo[Sequencer]
  protected given sequencer: cats.effect.Sync[Sequencer]
  protected given loggerFactory: model.logger.Factory[Sequencer]
  
  override def submitFeedback[ErrorChannel[_]](
    userId: model.user.Id,
    category: feedback.FeedbackType,
    contents: feedback.FeedbackContents
  )(using
    errorChannel: cats.MonadError[ErrorChannel, feedback.Error] & cats.Traverse[ErrorChannel],
    traceId: trace.Id
  ): Sequencer[ErrorChannel[status.Ok]] = {
    (for {
      logger <- loggerFactory.create.attemptAndAbortSequenceOnError
      _ <- logger.info(s"Processing feedback submission; trace: $traceId").attemptAndAbortSequenceOnError
      
      // Validate content length
      _ <- Option.when(contents.value.nonEmpty)(())
        .liftTo[ErrorChannel](feedback.Error.ValidationError("Feedback cannot be empty", traceId))
        .pure[Sequencer].abortSequenceOnError
      
      // Delegate to repository
      _ <- feedbackRepo.submitFeedback[ErrorChannel](userId, category, contents).abortSequenceOnError
      
      _ <- logger.info(s"Feedback submission processed; trace: $traceId").attemptAndAbortSequenceOnError
    } yield status.Ok).value
  }
  
  // Implementation of retrieveFeedback...
}
```

#### 6. Define Web Routes for the Service

Create routes that connect your service to HTTP endpoints:

```scala
// Step 6: Web routes
// Example based on pattern from apps/web/routes/auth/required/writer/DocumentRetrievalRoutesImpl.scala
package com.crib.bills.ouroboros
package apps.web.routes.auth.required

import cats.syntax.all.*
import org.http4s
import io.circe.generic.auto.*
import tsec.authentication.asAuthed

import model.{trace, user}
import user.feedback
import typeclasses.ops.{attemptAndAbortSequenceOnErrorUsingEither}

class FeedbackRoutesImpl[Effect[_]](
  feedbackService: apps.services.feedback.FeedbackService[Effect]
)(using 
  effect: cats.effect.Async[Effect],
  loggerFactory: model.logger.Factory[Effect]
) extends FeedbackRoutes[Effect] with http4s.dsl.Http4sDsl[Effect] with model.logger.DefaultLogName {

  // Data transfer objects
  case class FeedbackRequest(category: String, contents: String)
  
  override val authedRoutes: tsec.authentication.TSecAuthService[
    (user.User, trace.Id),
    tsec.authentication.AugmentedJWT[tsec.mac.jca.HMACSHA512, model.user.Id],
    Effect
  ] = tsec.authentication.TSecAuthService {
    case req@POST -> Root / "submit" asAuthed ((user, traceId)) =>
      (for {
        // Parse request body
        feedbackReq <- req.request.as[FeedbackRequest].attemptAndAbortSequenceOnErrorUsingEither
        
        // Convert string category to domain type
        category <- parseFeedbackType(feedbackReq.category)
          .liftTo[Either[feedback.Error, *]](feedback.Error.ValidationError(s"Invalid category: ${feedbackReq.category}", traceId))
          .pure[Effect].abortSequenceOnError
        
        // Call service
        given trace.Id = traceId
        _ <- feedbackService.submitFeedback[Either[feedback.Error, *]](
          user.id,
          category,
          feedback.FeedbackContents(feedbackReq.contents)
        ).abortSequenceOnError
        
        // Return success response
        response <- Ok("Feedback submitted successfully").attemptAndAbortSequenceOnErrorUsingEither
      } yield response).value.flatMap {
        case Right(success) => success.pure[Effect]
        case Left(error) => web.error.feedbackErrorMapper[Effect].findErrorResponse(error)
      }
  }
  
  private def parseFeedbackType(str: String): Option[feedback.FeedbackType] = str.toLowerCase match {
    case "bug" => Some(feedback.FeedbackType.Bug)
    case "feature" => Some(feedback.FeedbackType.FeatureRequest)
    case "general" => Some(feedback.FeedbackType.General)
    case _ => None
  }
}
```

#### 7. Implement Database Repository

Create a database-backed implementation:

```scala
// Step 7: Database repository implementation (using Skunk)
// Example follows Resource-based pattern similar to the PDF text extraction implementation
package com.crib.bills.ouroboros
package factories.user.feedback

import cats.effect
import cats.effect.Resource
import cats.syntax.all.*
import skunk.*
import skunk.codec.all.*
import skunk.implicits.*

import model.{logger, trace, user}
import user.feedback
import typeclasses.ops.{attemptAndAbortSequenceOnError}

// Queries interface
trait FeedbackQueries {
  val feedbackIdCodec: Codec[feedback.Id]
  val feedbackContentsCodec: Codec[feedback.FeedbackContents]
  val feedbackTypeCodec: Codec[feedback.FeedbackType]
  
  val insertFeedback: Command[feedback.Id ~ user.Id ~ feedback.FeedbackType ~ feedback.FeedbackContents ~ feedback.FeedbackSubmissionTimestamp]
  val selectFeedbackById: Query[feedback.Id, feedback.FeedbackRecord]
}

class AuroraRepo[Sequencer[_]](
  sessionPool: Resource[Sequencer, Session[Sequencer]],
  queries: FeedbackQueries
)(using
  sequencer: effect.Sync[Sequencer],
  loggerFactory: logger.Factory[Sequencer]
) extends feedback.Repo[Sequencer] with logger.DefaultLogName {

  override def submitFeedback[ErrorChannel[_]](
    userId: user.Id,
    category: feedback.FeedbackType,
    contents: feedback.FeedbackContents
  )(using
    errorChannel: cats.MonadError[ErrorChannel, feedback.Error] & cats.Traverse[ErrorChannel],
    traceId: trace.Id
  ): Sequencer[ErrorChannel[feedback.Id]] = {
    (for {
      logger <- loggerFactory.create.attemptAndAbortSequenceOnError
      newId <- sequencer.delay(feedback.Id(java.util.UUID.randomUUID())).attemptAndAbortSequenceOnError
      timestamp = feedback.FeedbackSubmissionTimestamp(java.time.Instant.now())
      
      // Execute DB operation
      _ <- sessionPool.use { session =>
        session.prepare(queries.insertFeedback).flatMap { cmd =>
          cmd.execute(newId ~ userId ~ category ~ contents ~ timestamp)
        }
      }.attemptAndAbortSequenceOnError
        .adaptError(ex => feedback.Error.UnspecifiedError(ex))
        
      _ <- logger.info(s"Feedback stored in database with id: $newId; trace: $traceId").attemptAndAbortSequenceOnError
    } yield newId).value
  }
  
  // Implementation of retrieveFeedback...
}
```

#### 8. Add Tests for All Components

Finally, implement tests for your new feature:

```scala
// Step 8: Tests
// Example follows pattern from document compatibility service property-based tests
// Uses Weaver's Checkers trait for ScalaCheck integration with forall pattern
package com.crib.bills.ouroboros
package apps.services.feedback

import cats.effect.IO
import weaver.{Expectations, SimpleIOSuite}
import weaver.scalacheck.Checkers

import model.user.feedback
import model.trace

// Base test trait that works against the service interface
trait FeedbackServiceTests extends SimpleIOSuite with Checkers {
  // Abstract builder to be implemented by test implementations
  def buildServices: IO[FeedbackTestServices]
  
  test("submitting valid feedback returns success") {
    forall { (contents: String) =>
      val nonEmptyContents = if (contents.isEmpty) "test feedback" else contents
      
      for {
        services <- buildServices
        traceId = trace.Id(java.util.UUID.randomUUID())
        given trace.Id = traceId
        
        result <- services.feedbackService
          .submitFeedback[Either[feedback.Error, *]](
            services.testUser.id,
            feedback.FeedbackType.Bug,
            feedback.FeedbackContents(nonEmptyContents)
          ).flatMap(IO.fromEither)
      } yield expect(result == model.status.Ok)
    }
  }
  
  // Additional tests...
}

// In-memory implementation test
object InMemoryFeedbackServiceTests 
  extends FeedbackServiceTests
  with setup.repo.InMemoryRepoBuilder
  with setup.dependency.NoExternalDependencyBuilder {
  
  override def buildServices: IO[FeedbackTestServices] = for {
    repos <- buildRepos
    feedbackService = FeedbackService.apply[IO](repos.feedbackRepo)
  } yield FeedbackTestServices(feedbackService, testUser)
}

// Database implementation test
object DatabaseFeedbackServiceTests 
  extends FeedbackServiceTests
  with setup.repo.DatabaseRepoBuilder
  with setup.dependency.NoExternalDependencyBuilder {
  
  override def buildServices: IO[FeedbackTestServices] = for {
    repos <- buildRepos
    feedbackService = FeedbackService.apply[IO](repos.feedbackRepo)
  } yield FeedbackTestServices(feedbackService, testUser)
}
```

By following this workflow, you ensure your feature is built on a solid foundation with proper separation of concerns, type safety, and comprehensive test coverage.

### 8.2 Feature Development Flow

```
┌─────────────┐     ┌─────────────────┐     ┌────────────────┐
│ Define      │────▶│ Create Service  │────▶│ Create Repo    │
│ Domain      │     │ Trait           │     │ Trait          │
└─────────────┘     └─────────────────┘     └────────────────┘
                                                     │
                                                     │
                                                     ▼
┌─────────────┐     ┌─────────────────┐     ┌────────────────┐
│ Implement   │◀────│ Create Service  │◀────│ Implement      │
│ Routes      │     │ Implementation  │     │ In-Memory Repo │
└─────────────┘     └─────────────────┘     └────────────────┘
       │                                             │
       │                                             │
       ▼                                             ▼
┌─────────────┐                           ┌────────────────┐
│ Write Tests │◀──────────────────────────│ Implement DB   │
│             │                           │ Repo           │
└─────────────┘                           └────────────────┘
```

### 8.3 The Value of Stubs

Stubs provide numerous benefits:

- **Incremental Development**: Build UI and routes while service is incomplete
- **Isolated Testing**: Test without dependencies on external systems
- **Safe Merges**: Merge code that compiles but isn't fully implemented
- **Minimal Code Churn**: Swap implementations with minimal changes
- **Faster Feedback Loops**: Test user flows early in development

### 8.4 Recommendations for Efficient Development

1. **Interface First**: Always define interfaces before implementations
2. **Test Against Contracts**: Write tests against interfaces, not concrete classes
3. **Stubs Before Full Implementations**: Create stubs early to unblock dependencies
4. **Maintain Naming Conventions**: Follow the Service/Impl/Stub pattern consistently
5. **Explicit Dependencies**: Use protected definitions rather than bundled traits
6. **Leverage Self-Aliases**: Use explicit self-references for better discoverability

[Back to TOC](#table-of-contents)


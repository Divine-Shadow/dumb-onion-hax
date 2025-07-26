# Capabilities, Stubs, and Impls

## About This Document

This document is designed for Scala developers seeking to understand how services are structured within our system. We examine how services are broken down into distinct capabilities, production implementations, and testing stubs. Through clear examples and detailed explanations, this guide explains our design choices and naming conventions, helping you navigate our codebase effectively.

## Document Organization and Key Points

The document is organized into several focused sections:

- **Defining Capability Traits:**
   We begin by explaining the principle of coding to interfaces. This section illustrates how pure contracts (capability traits) specify what a service should do without dictating implementation details, using examples like repository traits and single capability services.
- **Naming Conventions for Implementations and Stubs:**
   This section details our evolved naming strategy. It highlights how we differentiate between service contracts (`Service`), production-ready implementations (`Impl`), and safe, non-production stubs (`Stub`). We also address challenges such as AI tool preferences and the need for multiple implementations.
- **Why Have Stubs at All?**
   Here, we discuss the rationale behind using stubs. By building and testing against service interfaces, stubs allow for incremental development, isolated testing, minimized code churn, and safe merging—ensuring that incomplete services do not lead to runtime failures.
- **Testing Strategy: Contract-Driven Development:**
   We describe our approach to writing tests against service contracts rather than specific implementations. This section includes detailed examples of user flow tests that validate end-to-end functionality while remaining agnostic to the underlying implementation. It also demonstrates how easily implementations can be swapped—such as moving from an in-memory stub to a database-backed version—without altering the test logic.

Each section builds on the last, reinforcing our core principles: clear separation of concerns, modularity through abstraction, and the flexibility to change implementations without affecting overall system behavior.

By following this guide, you will gain a deep understanding of how our services are structured, how they can be tested in isolation or in integration, and how to apply these principles to your own Scala development projects.

## Defining Capability Traits

Our codebase adheres to the principle of coding to interfaces rather than to concrete implementations. In practice, this means we define **capability traits**—pure contracts that specify *what* a service should do, without dictating *how* it should be done. These traits focus entirely on the capabilities required, abstracting away all implementation details.

A capability trait is designed to be small and cohesive, typically encapsulating only one or two closely related functionalities. By isolating the interface from its implementation, we enable flexibility, easier testing, and a clear separation of concerns. The focus is on the contract—the usage—and not on the construction or dependencies of the service.

### Example: A Repository Capability Trait

Consider the following example of a repository for storing and retrieving messages:

```scala
package com.crib.bills.ouroboros
package model
package message

/**
 * `message.Repo` is a capability trait allowing for the storage and retrieval
 * of messages. The external effect type `Sequencer` signifies the completion
 * of operations, ensuring consistency, while the `ErrorChannel` provides
 * a clear path for error handling separate from the operational sequencing.
 */
trait Repo[Sequencer[_]]:
  def add[ErrorChannel[_], Message](
    conversationID: context.ID,
    role: interlocutor.Role[Message],
    message: Message
  )(using errorChannel: cats.MonadError[ErrorChannel, model.Error] & cats.Traverse[ErrorChannel])
  : Sequencer[ErrorChannel[Key[Message]]]

  def load[ErrorChannel[_], Message](
    conversationID: context.ID,
    key: message.Key[Message]
  )(using errorChannel: cats.MonadError[ErrorChannel, model.Error] & cats.Traverse[ErrorChannel])
  : Sequencer[ErrorChannel[Message]]
```

In this trait, the methods `add` and `load` define how messages are handled in an abstract manner. They specify the types of effects involved—using a `Sequencer` to manage the execution order and an `ErrorChannel` to handle errors—without committing to any specific implementation. This allows any concrete repository to conform to this contract, whether it interacts with a database, a file system, or even an in-memory store.

### Example: A Single Capability Service

Similarly, consider a service dedicated to extracting text from an uploaded document:

```scala
package com.crib.bills.ouroboros
package apps.services.journey.writer.document.upload.extraction

import cats.{MonadError, Traverse}
import fs2.Stream

import model.{journey, status, trace, user}
import journey.writer.document

/** `extraction.Service` */
trait Service[Sequencer[_]] {
  def extractText[ErrorChannel[_]](
    userID: user.Id,
    fileStream: Stream[Sequencer, Byte]
  )(using
    errorChannel: MonadError[ErrorChannel, status.Error] & Traverse[ErrorChannel],
    traceID: trace.Id
  ): Sequencer[ErrorChannel[document.RawText]]
}
```

Here, the `Service` trait defines a single operation `extractText`. The method signature clearly indicates the use of effect types for sequencing and error handling. There are no implementation details included—only the contract that any implementation must follow.

### Key Takeaways

- **Interface over Implementation:**
   Capability traits define *what* a service does, leaving the *how* to the concrete implementation. This makes the interfaces highly reusable and easy to test.
- **Purity and Flexibility:**
   By keeping these interfaces pure—focusing solely on the capabilities without binding to any concrete dependencies—we promote modularity and allow for easy substitution of implementations as requirements evolve.
- **Clear Separation of Concerns:**
   The explicit declaration of effect types (such as `Sequencer` and `ErrorChannel`) in the method signatures ensures that the focus remains on the contract and that error handling and sequencing are handled consistently across the codebase.

This design approach lays the foundation for a system where services are robust, interchangeable, and maintainable, allowing developers to focus on building the desired behavior without being encumbered by unnecessary implementation details.

## Naming Conventions for Implementations and Stubs

Our naming conventions for service implementations have evolved through experimentation, particularly to align with the strengths of AI tooling. The core idea is to differentiate clearly between the contract, production implementations, and testing stubs while keeping names informative yet manageable.

### Common Patterns

A typical service package follows a pattern where:

- **`Service`** designates the capability trait or contract.
- **`Impl`** indicates the production-ready implementation.
- **`Stub`** represents an alternative implementation that is safe (e.g., free from runtime exceptions) but not intended for production use.

For example, in the `registration` package:

- `registration.Service` is the contract.
- `registration.Impl` is the production implementation.
- `registration.Stub` provides a safe, non-production version.

### Challenges and Considerations

Two challenges have emerged in practice:

1. **AI Tool Preferences:**
    AI tools often favor fully qualified names such as `registration.RegistrationService` and `registration.RegistrationServiceImpl`. This trend can lead to naming drift from our intended concise and meaningful patterns.

2. **Multiple Valid Implementations:**
    Some components—often repositories—may have several valid implementations. For instance, our `message.Repo` might have implementations like:

   ```scala
   /** `factories.message.repo.Aurora` */
   trait Aurora[Sequencer[_]](
     sessionPool: Resource[Sequencer, skunk.Session[Sequencer]]
   )(using cats.effect.Async[Sequencer])
     extends model.message.Repo[Sequencer]
   ```

   ```scala
   /** `factories.message.repo.FileSystem` */
   trait FileSystem[Sequencer[_]](
     workingDirectory: fs2.io.file.Path
   )(using
     sequencer: cats.effect.Async[Sequencer],
     loggerFactory: model.logger.Factory[Sequencer],
     files: fs2.io.file.Files[Sequencer]
   ) extends message.Repo[Sequencer]
   ```

   ```scala
   /** `factories.message.repo.InMemory` */
   class InMemory[Sequencer[_]](
     lookupTable: Ref[Sequencer, Map[context.ID, Map[message.ID, Object]]],
     idGenerator: factories.message.Id.Factory[Sequencer]
   )(using
     sequencer: cats.effect.Sync[Sequencer],
     loggerFactory: model.logger.Factory[Sequencer]
   ) extends model.message.Repo[Sequencer]
   ```

   This diversity in implementations is useful and often necessary; however, it requires a naming strategy that can clearly communicate the intent and context of each implementation.

### Current Policy and Best Practices

To balance these concerns, the revised codebase policy is as follows:

- **Service Contracts:**
   The contract should include `Service` in its name and reside in the same package as its implementations and stubs. This makes it immediately clear what the interface is and what capabilities it exposes.
- **Production Implementations:**
   Production-ready implementations should include `Impl` as part of their name. For example, `registration.Impl` or in cases where the full qualified name is used, something like `beatparser.DocumentStructureExtractionBeatParsingServiceImpl` is permitted. Developers should strive for clarity without excessive verbosity.
- **Testing Stubs:**
   Any stub implementations intended for testing or non-production environments should include `Stub` in their name. This explicitly signals that the implementation is designed to be safe but may not offer full production functionality.
- **Consistency in Packaging:**
   All related artifacts—the service contract, production implementations, and stubs—should be located in the same package. This not only reinforces the relationship between them but also aids both human developers and AI tooling in understanding and navigating the codebase.
- **Mindful Naming:**
   While descriptive names are encouraged, developers should balance the amount of information in the class path versus the class name. Long class names are acceptable if they add clarity, but redundancy should be avoided.

By following these guidelines, our naming conventions provide clear, consistent cues about the role and intended usage of each component. This helps ensure that both human developers and AI tools can quickly understand the organization of services and select the appropriate implementation for their needs.

## Why Have Stubs at All?

_see also (README_Why_Stubs_Matter.md)[../philosophy/README_Why_Stubs_Matter.md] _
When adding new functionality—such as a feature that allows users to submit feedback or bug reports—it’s important to build and test against the service interface rather than a complete production implementation. This approach supports several key design goals:

- **Interface-Driven Development:**
   You want to build to the contract (the service interface) so that the rest of your system can progress without waiting for the full production implementation. This enables incremental development and testing.
- **Minimized Code Churn:**
   When you eventually swap out a stub for a full implementation, having a stable interface minimizes the changes required. The stub acts as a temporary, yet correct, placeholder, reducing disruption in your codebase.
- **Incremental Testing:**
   With a stub, you can test just the new or modified routes without relying on the complete, end-to-end functionality of the service. This isolates your changes and makes it easier to validate incremental progress.
- **Safe Merging:**
   Using unsafe placeholders like `null` or `???` can compile but will cause runtime exceptions if invoked. A stub, on the other hand, provides a hard-coded, correctly typed response that lets you merge code safely and ensures that even incomplete services won’t cause unexpected failures.

Consider the following route implementation for feedback submission:

```scala
class Impl[Sequencer[_]](
  service: apps.services.feedback.FeedbackService[Sequencer]
)(
  using sequencer: cats.effect.Async[Sequencer],
        loggerFactory: model.logger.Factory[Sequencer]
) extends FeedbackRoutes[Sequencer]
  with org.http4s.dsl.Http4sDsl[Sequencer]
  with model.logger.DefaultLogName {

  override def handleFeedbackRequest(
    req: authentication.SecuredRequest[Sequencer, (model.user.User, model.trace.Id), AugmentedJWT[HMACSHA512, user.Id]]
  ): Sequencer[Response[Sequencer]] =
    (for {
      _            <- ().asRight[model.user.feedback.Error].pure[Sequencer].abortSequenceOnError
      logger       <- loggerFactory.create.attemptAndAbortSequenceOnErrorUsingEither
      (callingUser, traceId) = req.identity
      given model.trace.Id = traceId

      _            <- logger.info(s"Submitting contents from ${callingUser.email}").attemptAndAbortSequenceOnErrorUsingEither

      given circe.Decoder[apps.requests.feedback.Submit] = apps.web.codecs.submitUserFeedback(callingUser.id)
      request      <- req.request
                        .as[apps.requests.feedback.Submit]
                        .attemptAndAbortSequenceOnErrorUsingEither
                        .adaptError(model.user.feedback.Error.ParsingError(traceId))

      _            <- service.submitFeedback[Either[feedback.Error, *]](request.user, request.category, request.contents).abortSequenceOnError
      _            <- logger.info(s"Feedback parsed; Trace Id: $traceId").attemptAndAbortSequenceOnErrorUsingEither

      response     <- Ok().attemptAndAbortSequenceOnErrorUsingEither
    } yield response).value.flatMap {
      case Right(success) => success.pure[Sequencer]
      case Left(error)    => feedbackErrorMapper[Sequencer].findErrorResponse(error)
    }

  override def tsecService: tsec.authentication.TSecAuthService[
    (user.User, model.trace.Id),
    tsec.authentication.AugmentedJWT[tsec.mac.jca.HMACSHA512, user.Id],
    Sequencer
  ] = tsec.authentication.TSecAuthService {
    case req@POST -> Root asAuthed _ => handleFeedbackRequest(req)
  }
}
```

This route relies on a service contract (`FeedbackService`) to connect to the underlying functionality. A centralized route constructor ties various services together, ensuring that each route is built from a consistent set of service interfaces.

But what happens if the actual implementation isn’t ready yet? If we were to use placeholders like `null` or `???`, our code would compile, but invoking these routes would lead to runtime exceptions—disrupting testing and merging efforts. Instead, we use a **stub**:

```scala
package com.crib.bills.ouroboros
package apps.services.feedback

import cats.{MonadError, Traverse}
import model.status
import model.trace
import model.user.feedback
import cats.syntax.all.catsSyntaxApplicativeId

trait FeedbackStub[Sequencer[_]](using cats.Applicative[Sequencer]) extends FeedbackService[Sequencer]:
  override def submitFeedback[ErrorChannel[_]](
    user: model.user.Id,
    category: feedback.FeedbackType,
    contents: feedback.FeedbackContents
  )(using errorChannel: MonadError[ErrorChannel, feedback.Error] & Traverse[ErrorChannel],
    traceId: trace.Id
  ): Sequencer[ErrorChannel[status.Ok]] =
    status.Ok.pure[ErrorChannel].pure[Sequencer]
```

With this stub, even though the full functionality isn’t implemented, the service still provides a well-typed, hard-coded response. This allows you to:

- **Test Incrementally:** Verify that your routes compile and work as expected with a controlled, predictable output.
- **Merge Safely:** Avoid runtime exceptions by ensuring that every service dependency returns a valid response, even if it’s a placeholder.
- **Focus on the Interface:** Keep developing and refining new code (like routes) without being blocked by incomplete service implementations.

Using stubs in this way is a strategic design choice—it lets us progress confidently, ensures the stability of merges, and allows for incremental testing without waiting for every dependency to be fully implemented. More details on testing strategies using stubs will be covered in the next section.

## Testing Strategy: Contract-Driven Development

Our testing strategy embraces the principle of writing tests to a contract rather than to a specific implementation. By doing so, we gain several advantages:

- **Interface-Driven Testing:**
   Tests are written against service interfaces (or contracts), which means they focus on what a service is expected to do, not how it’s built. This allows us to verify business logic and behavior without being coupled to any particular implementation.
- **Flexible Implementation Swaps:**
   Since tests target the contract, we can easily swap out different implementations—such as in-memory or database-backed versions—without modifying the tests. This flexibility enables incremental development. For example, you can begin with an in-memory stub to validate the new user flow and later switch to a full database implementation by changing a single line.
- **Isolated and Incremental Testing:**
   Writing tests against interfaces allows us to isolate new functionality and test it incrementally. You can merge changes without fear of runtime exceptions, even if the complete service is not yet implemented. This minimizes code churn and prevents integration issues caused by incomplete dependencies.

### An Illustrative Example

Consider the following test for a user flow that activates a free trial using a promo code and demographic data. Notice the key line:

```scala
type Res = apps.web.service.Services[IO]
```

This declaration states that the test requires a full suite of services, without caring about the underlying construction of those services. The test then proceeds to execute various steps—such as creating an account, validating promo codes, and checking subscription status—all while working strictly against the service interfaces.

```scala
trait TrialActivationUserFlowServiceTests extends IOSuite with Checkers {

  // Define the type of the shared resource (tuple of services)
  type Res = apps.web.service.Services[IO]

  // Shared resource setup (to be provided by specific implementations)
  def sharedResource: Resource[IO, Res]

  test("User flow: create account, activate trial via promo and demographics") { services =>
    forall { (email: Email, password: Password, surveyData: demographics.SurveyData, promoText: PromoCodeText) =>
      for {
        // Generate required IDs and trace
        given trace.Id <- IO.delay(trace.Id.apply(UuidCreator.getTimeOrderedEpoch()))

        // Step 1: Create an account
        creationRequest = apps.requests.account.Creation(email, password)
        creationResponse <- services.createAccount.create(creationRequest).flatMap(IO.fromEither)
        createdUser = creationResponse.user
        expectEmailSaved = expect(createdUser.email == email)

        // Step 2: Validate promo code (should return NotFound before it's created)
        initialStatus <- services.trialActivationService.validatePromoCode[Either[model.promo.PromoCodeError, *]](promoText, createdUser.id).flatMap(IO.fromEither)
        expectPromoNotFoundBeforeCreation = expect(initialStatus == PromoCodeStatus.NotFound)

        // Step 3: Create a promo code
        promoCode = PromoCode(
          id = PromoCodeId(UuidCreator.getTimeOrdered()),
          code = promoText,
          validity = ValidityPeriod(
            validFrom = java.time.LocalDate.now().minusDays(1),
            validTo = Some(java.time.LocalDate.now().plusDays(7))
          )
        )
        _ <- services.promoService.createPromoCode[Either[model.promo.PromoCodeError, *]](promoCode).flatMap(IO.fromEither)

        // Step 4: Validate the promo code (should now return Valid)
        validStatus <- services.trialActivationService.validatePromoCode[Either[model.promo.PromoCodeError, *]](promoText, createdUser.id).flatMap(IO.fromEither)
        expectFindsValidPromoCodes = expect(validStatus == PromoCodeStatus.Valid)

        // Step 5: Associate promo code with user and submit demographics
        _ <- services.trialActivationService.activateTrial[Either[model.promo.PromoCodeError, *]](createdUser, promoText, surveyData).flatMap(IO.fromEither)

        // Step 6: Validate Demographic Storage
        retrievedDemographics <- services.demographicService.getDemographics[Either[model.user.demographics.DemographicsError, *]](createdUser.id)
                                  .flatMap(IO.fromEither)
                                  .flatMap(opt => IO.fromOption(opt)(new Throwable("Demographics not found")))
        expectedDemographics = Demographics(createdUser.id, surveyData, Some(promoCode.id))
        expectSavedDemographics = expect(retrievedDemographics == expectedDemographics)

        // Step 7: Validate the user's subscription status
        loginStatus <- services.auth.authenticate(apps.requests.account.Login(email, password)).flatMap(IO.fromEither).map(_.status)
        expectUpdatedSubscription = expect(loginStatus == model.user.subscription.Status.FreeTrialMode)
      } yield expectEmailSaved and expectPromoNotFoundBeforeCreation and expectFindsValidPromoCodes and expectSavedDemographics and expectUpdatedSubscription
    }
  }
}
```

### Implementation Flexibility in Tests

To support this interface-driven approach, our test setup allows for easy swapping of implementations. For instance, the user flow test above can run with an in-memory implementation:

```scala
object InMemoryTrialActivationUserFlowServiceTests
  extends TrialActivationUserFlowServiceTests
    with factories.logger.Factory.NoOpLogger
    with setup.repo.InMemoryRepoBuilder
    with setup.dependency.NoExternalDependencyBuilder
    with setup.CombinedServiceSetup
```

Switching to a database-backed implementation is as simple as changing the repository builder:

```scala
object DatabaseTrialActivationUserFlowServiceTests
  extends TrialActivationUserFlowServiceTests
    with factories.logger.Factory.NoOpLogger
    with setup.repo.DatabaseRepoBuilder
    with setup.dependency.NoExternalDependencyBuilder
    with setup.CombinedServiceSetup
```

The `setup.CombinedServiceSetup` abstraction enables fluid swapping of dependencies, whether they are in-memory, stubbed, or fully integrated implementations.

### Benefits of This Approach

- **Code Reuse:**
   High-quality tests written against service contracts promote reuse and reduce duplication. Once the test logic is in place, it can validate different implementations with minimal changes.
- **Predictable Behavior:**
   Testing against interfaces ensures that behavior is consistent across various environments. This predictability bolsters the confidence that local builds will mirror production behavior.
- **Confidence in Dependency Graphs:**
   By continually demonstrating that our service dependencies adhere to the defined contracts, we can safely introduce new services without fear of regressions. This strategy also bounds the potential impact (blast radius) of changes, making the system more maintainable.

By writing tests to contracts rather than specific implementations, we enable robust, flexible, and incremental testing. This approach ensures that our codebase remains modular and maintainable, and that new features can be developed and integrated with confidence, regardless of the underlying implementation details.

## Conclusion

This guide has taken you through our approach to structuring services in our Scala codebase by focusing on three core elements: capability traits, production implementations (Impls), and testing stubs. Here are the key takeaways:

- **Capability Traits:**
   We design our services as pure interfaces—small, cohesive contracts that define *what* a service should do without imposing *how* it should be done. This separation of concerns encourages modularity, facilitates testing, and supports flexible implementations.
- **Naming Conventions:**
   Our naming strategy distinguishes between service contracts (`Service`), production-ready implementations (`Impl`), and testing stubs (`Stub`). This convention not only clarifies the intended usage of each component but also aligns with both human intuition and AI tooling preferences, ensuring clarity and consistency across the codebase.
- **The Role of Stubs:**
   Stubs provide a safe, well-typed placeholder for incomplete functionality. They enable incremental development, isolated testing, and safe merging by allowing you to test new features without waiting for full implementations. This approach minimizes code churn and mitigates the risk of runtime exceptions during development.
- **Contract-Driven Testing:**
   By writing tests against service interfaces rather than specific implementations, we promote code reuse, predictability, and confidence in our dependency graphs. This strategy allows us to easily swap implementations—from in-memory stubs to full database-backed versions—while ensuring that the essential business logic remains robust and consistent.

Overall, these principles enable a scalable and maintainable development process where new features can be integrated with confidence. The separation between capabilities, implementations, and stubs not only drives clearer code but also enhances collaboration between human and AI developers. This guide serves as a roadmap for building systems that are both flexible in design and resilient in production.

We hope the insights provided here empower you to apply these practices in your own projects, ultimately leading to safer, more predictable, and highly maintainable Scala applications.
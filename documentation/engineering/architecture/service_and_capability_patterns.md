## 3. Service and Capability Patterns

This is a standalone excerpt from the [Architecture and Development Guide](../README_Architecture_And_Development_Guide.md).

*Source: `README_Capabilities_Stubs_and_Impls.md`*

[Back to TOC](#table-of-contents)

Ouroboros uses a capability-based architecture to define services and their implementations.

### 3.1 The Capability Trait Pattern

Services are defined as pure contracts that specify what a service should do without implementation details:

```scala
// From apps/services/document/compatibility/Service.scala
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

### 3.2 Implementation Hierarchy

#### Production Implementation (Impl)

```scala
// From apps/services/document/compatibility/Impl.scala
trait Impl[Sequencer[_]]
  extends Service[Sequencer]
    with model.logger.DefaultLogName {

  // Explicit dependency injection via protected definitions
  protected given sequencer: cats.effect.Async[Sequencer]
  protected given loggerFactory: model.logger.Factory[Sequencer]
  
  // Implementation details...
}
```

#### Testing Stub Implementation (Stub)

```scala
// From apps/services/document/compatibility/Stub.scala
trait Stub[Sequencer[_]]
  extends Service[Sequencer] {
  
  // Simple implementation that returns hardcoded values
  override def extractText[ErrorChannel[_]](
    userID: user.Id,
    fileStream: Stream[Sequencer, Byte]
  )(using
    errorChannel: MonadError[ErrorChannel, status.Error] & Traverse[ErrorChannel],
    traceID: trace.Id
  ): Sequencer[ErrorChannel[document.RawText]] =
    sequencer.pure(errorChannel.pure(document.RawText("Sample text")))
}
```

### 3.3 Self-Aliasing in Traits

Ouroboros uses explicit self-aliases to improve service discoverability and work better with AI tools:

```scala
// From apps/services/interlocutors/Service.scala (following pattern from README_Mixin_Patterns.md)
trait InterlocutorsService[Sequencer[_]] {
  // Self-alias provides a consistent reference
  protected def interlocutorsService: InterlocutorsService[Sequencer] = this

  def commitForTemplate(): Unit
}
```

### 3.4 Service Factory Pattern

```scala
// From apps/services/feedback/FeedbackService.scala (factory example)
object FeedbackService {
  def apply[Sequencer[_]](
    repo: model.user.feedback.Repo[Sequencer]
  )(using cats.Monad[Sequencer]) = 
    new FeedbackService[Sequencer] {
      override def submitFeedback[ErrorChannel[_]](
        user: model.user.Id,
        category: feedback.FeedbackType,
        contents: feedback.FeedbackContents
      )(using errorChannel: cats.MonadError[ErrorChannel, feedback.Error] & cats.Traverse[ErrorChannel],
        traceId: model.trace.Id
      ): Sequencer[ErrorChannel[status.Ok]] =
        repo
          .submitFeedback[ErrorChannel](user, category, contents)
          .abortSequenceOnError
          .as(status.Ok)
          .value
    }
}


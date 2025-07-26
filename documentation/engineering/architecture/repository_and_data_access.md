## 4. Repository and Data Access

This is a standalone excerpt from the [Architecture and Development Guide](../README_Architecture_And_Development_Guide.md).

*Source: Examples from the codebase repository patterns*

[Back to TOC](#table-of-contents)

Repositories provide data access capabilities and follow the same patterns as services.

### 4.1 Repository Contracts

```scala
// From model/user/feedback/Repo.scala (similar to model/document/repo/Repo.scala)
trait Repo[Sequencer[_]] {
  def submitFeedback[ErrorChannel[_]](
    user: model.user.Id,
    category: feedback.FeedbackType,
    contents: feedback.FeedbackContents
  )(using errorChannel: cats.MonadError[ErrorChannel, feedback.Error] & cats.Traverse[ErrorChannel], 
    traceId: model.trace.Id
  ): Sequencer[ErrorChannel[feedback.Id]]
  
  def retrieveFeedback[ErrorChannel[_]](
    id: feedback.Id
  )(using errorChannel: cats.MonadError[ErrorChannel, feedback.Error] & cats.Traverse[ErrorChannel],
    traceId: model.trace.Id
  ): Sequencer[ErrorChannel[feedback.FeedbackRecord]]
}
```

### 4.2 Repository Implementations

#### In-Memory Implementation

```scala
// From factories/user/feedback/InMemoryImpl.scala (follows pattern in unit tests)
class InMemoryFeedbackImpl[Sequencer[_]](
  memory: effect.Ref[Sequencer, Map[feedback.Id, feedback.FeedbackRecord]]
)(using sequencer: effect.Sync[Sequencer], loggerFactory: logger.Factory[Sequencer])
  extends feedback.Repo[Sequencer]
    with logger.DefaultLogName {
  
  override def submitFeedback[ErrorChannel[_]](
    userId: model.user.Id,
    category: feedback.FeedbackType,
    contents: feedback.FeedbackContents
  )(using
    errorChannel: cats.MonadError[ErrorChannel, feedback.Error] & cats.Traverse[ErrorChannel],
    traceId: trace.Id
  ): Sequencer[ErrorChannel[feedback.Id]] = {
    (for {
      logger <- loggerFactory.create.attemptAndAbortSequenceOnError
      newId <- sequencer.delay(feedback.Id(UuidCreator.getTimeOrderedEpoch())).attemptAndAbortSequenceOnError
      timestamp = feedback.FeedbackSubmissionTimestamp(java.time.Instant.now())
      record = feedback.FeedbackRecord(newId, userId, category, contents, timestamp)
      _ <- memory.update(_ + (newId -> record)).attemptAndAbortSequenceOnError
    } yield newId).value
  }
}
```

#### Database Implementation

```scala
// From factories/user/feedback/AuroraImpl.scala (uses Resource pattern similar to PDF text extraction)
class AuroraFeedbackImpl[Sequencer[_]](
  sessionPool: Resource[Sequencer, skunk.Session[Sequencer]],
  queries: FeedbackAuroraQueries
)(using
  sequencer: effect.Sync[Sequencer],
  logFactory: logger.Factory[Sequencer]
) extends feedback.Repo[Sequencer] with model.logger.DefaultLogName {

  override def submitFeedback[ErrorChannel[_]](
    userId: model.user.Id,
    category: feedback.FeedbackType,
    contents: feedback.FeedbackContents
  )(using
    errorChannel: cats.MonadError[ErrorChannel, feedback.Error] & cats.Traverse[ErrorChannel],
    traceId: trace.Id
  ): Sequencer[ErrorChannel[feedback.Id]] = {
    (for {
      logger <- logFactory.create.attemptAndAbortSequenceOnError
      newId <- sequencer.delay(feedback.Id(UuidCreator.getTimeOrderedEpoch())).attemptAndAbortSequenceOnError
      timestamp = feedback.FeedbackSubmissionTimestamp(java.time.Instant.now())
      
      // Database operation using skunk - follows Resource-based pattern similar to extractTextFromPdfBytes
      _ <- sessionPool.use { session =>
        session.prepare(queries.insertFeedback).flatMap { cmd =>
          cmd.execute((newId, userId, category, contents, timestamp))
        }
      }.attemptAndAbortSequenceOnError
        .adaptError(ex => feedback.Error.UnspecifiedError(ex))
    } yield newId).value
  }
}


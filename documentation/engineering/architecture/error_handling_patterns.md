## 5. Error Handling Patterns

This is a standalone excerpt from the [Architecture and Development Guide](../README_Architecture_And_Development_Guide.md).

*Source: Error handling examples throughout the codebase*

[Back to TOC](#table-of-contents)

Ouroboros uses a structured approach to error handling with domain-specific error types.

### 5.1 Domain Error Classes

```scala
// From model/user/feedback/Error.scala (pattern used throughout the codebase)
/** `feedback.Error` */
class Error(cause: Throwable) extends model.Error(cause)
object Error {
  case class UnspecifiedError(cause: Throwable) extends Error(cause)
  case class FeedbackNotFound(feedbackId: Id, traceId: model.trace.Id) extends Error(
    new Throwable(s"Could not find contents with id `$traceId`; trace: `$traceId`")
  )
  case class ParsingError(traceId: model.trace.Id)(details: Throwable) extends Error(
    new Throwable(s"Unable to parse feedback, details: `${details.toString}`; trace: `$traceId``")
  )
  
  given errorBuilder: model.status.DomainErrorBuilder[feedback.Error] with {
    def fromThrowable(cause: Throwable) = UnspecifiedError(cause)
  }
}
```

### 5.2 Error Handling Flow

```
┌─────────────┐     ┌─────────────────┐     ┌────────────────┐
│ Operation   │────▶│ Error Detected  │────▶│ ErrorChannel   │
│ Execution   │     │                 │     │ (Either[E, A]) │
└─────────────┘     └─────────────────┘     └────────────────┘
       │                    │                        │
       │                    │                        │
       ▼                    ▼                        ▼
┌─────────────┐     ┌─────────────────┐     ┌────────────────┐
│ Success     │     │ Conversion to   │     │ Sequence       │
│ Path        │     │ Domain Error    │     │ Aborted        │
└─────────────┘     └─────────────────┘     └────────────────┘
```

### 5.3 Error Adaptation Pattern

```scala
// From apps/services/document/compatibility/Impl.scala (following the pattern from PDF text extraction)
// Convert generic errors to domain-specific errors
docxStream <- htmlToDocxStream[ErrorChannel](html)
  .adaptError(ex => Error.MarkdownToDocxError(ex.getMessage, traceId))
  .abortSequenceOnError
```


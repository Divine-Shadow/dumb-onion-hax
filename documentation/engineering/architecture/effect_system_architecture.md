## 2. Effect System Architecture

This is a standalone excerpt from the [Architecture and Development Guide](../README_Architecture_And_Development_Guide.md).

*Source: `README_Effect_Guide.md`*

[Back to TOC](#table-of-contents)

The Ouroboros effect system is built around three core concepts that ensure robust and predictable execution of operations.
This is meant as a jumpstart guide, for a more comprehensive explanation, see `README_Effect_Guide.md`. Seriously, if you are having trouble compiling, find yourself with naming or implicit resolution issues, check that readme before searching for random files.
### 2.1 Core Components

#### The Sequencer

- Uses `cats.effect.Sync[Sequencer]` or `cats.effect.Async[Sequencer]`
- Always referenced via `sequencer`, as in `sequencer.delay` instead of `Sync[Sequnecer].delay`
- Orchestrates the orderly execution of effectful operations
- Typically an `IO` or similar effect type in production

#### The Error Channel

- Leverages `cats.MonadError[ErrorChannel, E]` & `cats.Traverse[ErrorChannel]`
- Always referenced via `errorChannel` as in `errorChannel.raiseError`
- Manages and propagates errors
- Typically an `Either[E, A]` in practice, where `E` is a domain-specific error type

#### Abort Mechanisms

- Functions like `attemptAndAbortSequenceOnError` and `abortSequenceOnError`
- Ensure that upon encountering an error, further processing is halted
- Expressive alternatives to `EitherT`

### 2.2 Operation Flow Diagram

```
┌─────────────┐     ┌─────────────────┐     ┌──────────────────┐
│  Operation  │────▶│ Error Channel   │────▶│  Final Result    │
│  Sequence   │     │ (Either[E, A])  │     │                  │
└─────────────┘     └─────────────────┘     └──────────────────┘
       │                    ▲                         │
       │                    │                         │
       ▼                    │                         ▼
┌─────────────┐     ┌─────────────────┐     ┌──────────────────┐
│ Sequencer   │     │ abortSequence   │     │ Sequencer[       │
│ (IO, Task)  │◀────│ OnError         │     │  ErrorChannel[A] │
└─────────────┘     └─────────────────┘     └──────────────────┘
```

### 2.3 Postfix Operators

Two key postfix operators control the effect flow:

#### `.abortSequenceOnError`

- Use when an operation explicitly involves an error channel in its type (`Sequencer[ErrorChannel[A]]`)
- Aborts the sequence immediately if an error is encountered

#### `.attemptAndAbortSequenceOnError`

- Use for operations without an explicit error channel in their type (`Sequencer[A]`)
- Wraps the operation to catch any errors and propagate them correctly

### 2.4 Example: Effect System in Action

```scala
// From apps/services/document/compatibility/Impl.scala
// Implementation follows the pattern demonstrated in docxToHtml and htmlToMarkdown helper methods
def registerDocument[ErrorChannel[_]](
  userID: user.Id,
  fileName: document.Name,
  hierarchicalDocument: document.HierarchicalDocument
)(using
  errorChannel: MonadError[ErrorChannel, status.Error] & Traverse[ErrorChannel],
  traceID: trace.Id
): Sequencer[ErrorChannel[document.Metadata]] =
  (for {
    // Create a logger (no explicit error channel in return type)
    logger <- loggerFactory.create.attemptAndAbortSequenceOnError
    
    // Log operation (no explicit error channel)
    _      <- logger.info(s"Starting registration for document '$fileName'")
               .attemptAndAbortSequenceOnError

    // Generate document ID (no explicit error channel)
    docId  <- sequencer.delay(document.Id(UuidCreator.getTimeOrderedEpoch))
               .attemptAndAbortSequenceOnError

    // Business logic (pure function, no error handling needed)
    flatTitles = collectTitles(hierarchicalDocument.sections)
    rawOutline = document.RawOutline(flatTitles.mkString("\n"))

    // Repository operation (has explicit error channel)
    metadata <- documentRepo.initializeDocument[ErrorChannel](
                 userID, docId, rawOutline, fileName
               ).abortSequenceOnError
               
    // Final log operation (no explicit error channel)
    _ <- logger.info(s"Document registered with id: ${metadata.id}")
         .attemptAndAbortSequenceOnError
  } yield metadata).value  // .value unwraps the monad transformer
```

### 2.5 Anti-patterns
There are two major anti-patterns to avoid:

1. Please avoid using a single large `delay` or `blocking` call to wrap multiple effectful operations. Instead wrap each side effect in its own call and then compose them. This also applies to `java.time.Now()` calls and UUID generation.
Bad Example:
```scala
sequencer.delay{
  val aRow = adjustmentsSheet.createRow(adjustmentRow)
  aRow.createCell(0).setCellValue(id)
  aRow.createCell(1).setCellValue(adj.description.value)
  val currentTime = java.time.Instant.now()
  aRow.createCell(2).setCellValue(currentTime.toString)
}
```

Better Example
```scala
for {
  arow <- sequencer.delay(adjustmentsSheet.createRow(adjustmentRow))
  _ <- sequencer.delay(aRow.createCell(0).setCellValue(id))
  _ <- sequencer.delay(
    aRow.createCell(1).setCellValue(adj.description.value)
  )
  currentTime <- sequencer.delay(java.time.Instant.now())
  _ <- sequencer.delay( aRow.createCell(2).setCellValue(currentTime.toString))
} yield model.status.Ok

```

2. Please avoid using `sequencer` to capture errors, explicitly use `errorChannel` or the postfix transformers for operations than can fail. This might mean updating the ADT to permit errors. For the example above, writing to a closed workbook can throw an exception, thus:
Idiomatic example:
```scala
(for {
  arow <- sequencer.delay(adjustmentsSheet.createRow(adjustmentRow)).attemptAndAbortSequenceOnError
  _ <- sequencer.delay(aRow.createCell(0).setCellValue(id)).attemptAndAbortSequenceOnError
  _ <- sequencer.delay(
    aRow.createCell(1).setCellValue(adj.description.value)
  ).attemptAndAbortSequenceOnError
  currentTime <- sequencer.delay(java.time.Instant.now()).attemptAndAbortSequenceOnError
  _ <- sequencer.delay( aRow.createCell(2).setCellValue(currentTime.toString)).attemptAndAbortSequenceOnError
} yield model.status.Ok).value

```
# README Section: Using Combined Context Bounds in Scala with Cats

## Overview

In our Scala application, we've adopted a unique pattern where we combine multiple context bounds into a single unified type. This approach was chosen after extensive experimentation and is particularly used to manage error handling and effect traversal in final-tagless styled code. The combined use of `cats.MonadError` with `cats.Traverse` under a single alias `ErrorChannel` provides a cohesive API that simplifies type signatures and enhances code readability.

## Why Combine Context Bounds?

1. **Reduced Complexity in Type Signatures**:
   By combining `MonadError` and `Traverse` into a single context bound, we streamline our function signatures. This is particularly beneficial in a codebase like ours, which is heavily reliant on the final-tagless pattern. The unified context bound reduces the clutter of multiple type parameters and makes higher-order functions more straightforward.

2. **Cohesion and Expressiveness in API**:
   Naming the context bounds explicitly as `errorChannel` aligns with our goal of having expressive and approachable APIs. This naming convention would not be as coherent if we were to separate the context bounds for `MonadError` and `Traverse`, as each performs distinct but overlapping duties within error handling and collection traversal.

3. **Consistency with Existing Codebase Patterns**:
   The existing codebase already utilizes a pattern where several distinct concepts are passed as implicit parameters. Introducing another disparate context bound would disrupt this established pattern, creating asymmetry in function definitions and potentially leading to confusion.

## Code Example

Here is a snippet from our codebase demonstrating this pattern in action:

```scala 3
package com.crib.bills.ouroboros
package factories.interlocutor

import cats.Monad
import cats.MonadError
import cats.syntax.all.{toFunctorOps, toFlatMapOps}
import model.context.projector.Projector
import model.{interlocutor, logger, status}

import typeclasses.ops.{appendOne, abortSequenceOnError}

object Interlocutor {
  def static[Sequencer[_], Message](message: Message, role: interlocutor.Role[Message])
                                           (using
                                            sequencer: Monad[Sequencer],
                                            loggerFactory: logger.Factory[Sequencer]
                                           ): interlocutor.Interlocutor[Sequencer] =
    new model.Interlocutor[Sequencer] {
      override def respond[ErrorChannel[_]]
      (using
       projector: model.context.Projector[Sequencer]
       , appender: model.context.Appender[Sequencer]
       , errorChannel: cats.MonadError[ErrorChannel, status.Error] & cats.Traverse[ErrorChannel]
      ): Sequencer[ErrorChannel[status.Ok]] =
        (for {
          updateResult <- appender
            .appendOne(role, message)
            .map(errorChannel.fromEither).abortSequenceOnError
        } yield updateResult).value
    }
}
```

## Advantages

- **Simplicity**: Combining the bounds simplifies the injection and usage of dependencies, making the code less prone to errors and easier to refactor.
- **Flexibility**: The pattern allows us to handle more complex operations involving error channels and data sequencing without increasing the number of implicit parameters or complicating the dependency graph.

## Tips
You can use `cats.instances.either.catsStdInstancesForEither` to get robust `ErrorChannel` instance.

## Conclusion

The choice to combine `MonadError` and `Traverse` into a single context bound has significantly impacted our codebase's maintainability and clarity. This documentation section aims to clarify the rationale behind this decision, helping current and future developers understand the advantages and guiding them in maintaining or extending this pattern. By documenting this approach, we also aim to ensure that changes to this pattern are well-considered, preserving the benefits it offers to our codebase architecture.


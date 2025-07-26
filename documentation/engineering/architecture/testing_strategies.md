## 7. Testing Strategies

This is a standalone excerpt from the [Architecture and Development Guide](../README_Architecture_And_Development_Guide.md).

*Source: Testing implementations throughout the codebase, including property-based testing patterns*

[Back to TOC](#table-of-contents)

Ouroboros uses advanced testing strategies to ensure code quality.

### 7.1 Contract-Driven Testing

Tests are written against service interfaces rather than specific implementations.

```scala
// From apps/src/test/scala/apps/services/trial/TrialActivationUserFlowServiceTests.scala
// Similar pattern used in document compatibility service property-based tests
trait TrialActivationUserFlowServiceTests extends weaver.IOSuite with Checkers {
  // This trait defines tests against a service interface
  // The actual implementation is provided by the extending class
  // Checkers trait enables property-based testing with ScalaCheck
  
  // Builder for service dependencies 
  def buildTrialActivationServices: IO[TrialActivationTestServices]
  
  test("User flow: create account, activate trial via promo") { 
    for {
      services <- buildTrialActivationServices
      // Test code using the services abstraction
    } yield succeed
  }
}
```

### 7.2 Implementation-Specific Test Extensions

```scala
// From apps/src/test/scala/apps/services/trial/impl/InMemoryImplTests.scala and DatabaseImplTests.scala
// In-memory implementation test
object InMemoryTrialActivationUserFlowServiceTests
  extends TrialActivationUserFlowServiceTests
    with factories.logger.Factory.NoOpLogger
    with setup.repo.InMemoryRepoBuilder
    with setup.dependency.NoExternalDependencyBuilder

// Database implementation test (same test logic, different implementation)
object DatabaseTrialActivationUserFlowServiceTests
  extends TrialActivationUserFlowServiceTests
    with factories.logger.Factory.NoOpLogger
    with setup.repo.DatabaseRepoBuilder
    with setup.dependency.NoExternalDependencyBuilder
```

### 7.3 Property-Based Testing

```scala
// From apps/src/test/scala/apps/services/document/compatibility/CompatibilityServiceTests.scala
// Implements frequency-weighted generators (80% known good samples, 20% variations)
test("All document types are properly detected") {
  forall { (docxFile: DocxFile, pdfFile: PdfFile) =>
    for {
      // Test with arbitrary generated files from typeclasses.document.compatibility.Arbitraries
      docxType <- compatibilityService.detectFileType[Either[Error, *]](
                 docxFile.toStream[IO]
               ).flatMap(IO.fromEither)
      pdfType <- compatibilityService.detectFileType[Either[Error, *]](
               pdfFile.toStream[IO]
             ).flatMap(IO.fromEither)
    } yield expect(docxType == SupportedFileType.DOCX) and 
           expect(pdfType == SupportedFileType.PDF)
  }
}
```


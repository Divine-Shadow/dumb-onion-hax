## 6. Web Routes and API Architecture

This is a standalone excerpt from the [Architecture and Development Guide](../README_Architecture_And_Development_Guide.md).

*Source: Examples from web route implementations in the codebase*

[Back to TOC](#table-of-contents)

Web routes in Ouroboros follow a structured pattern using http4s.

### 6.1 Route Structure

#### Public Routes

```scala
// From apps/src/main/scala/apps/web/routes/public/stripe/StripePublicRoutes.scala
override val publicRoutes: org.http4s.HttpRoutes[Effect] = org.http4s.HttpRoutes.of[Effect] {
  case GET -> Root / "stripe-publishable-key"  =>
    (for {
      _ <- ().pure[Either[model.Error, *]].pure[Effect].abortSequenceOnError
      traceId <- effect.delay(model.trace.Id(UuidCreator.getTimeOrderedEpoch))
                .attemptAndAbortSequenceOnErrorUsingEither
      given model.trace.Id = traceId
      key <- stripeService.getPublishableKey[Either[model.Error, *]]
            .abortSequenceOnError
      encodedBody <- org.http4s.EntityEncoder[Effect, model.stripe.PublishableKey]
                   .toEntity(key).body.pure[Effect]
                   .attemptAndAbortSequenceOnErrorUsingEither
      
      encodedResponse = http4s.Response[Effect](
        status = http4s.Status.Ok,
        body = encodedBody
      )
    } yield encodedResponse).value.flatMap {
      case Right(success) => success.pure[Effect]
      case Left(error) => web.error.defaultMapper[Effect].findErrorResponse(error)
    }
}
```

#### Authenticated Routes

```scala
// From apps/web/routes/auth/required/stripe/StripeAuthedRoutesImpl.scala
// Similar pattern used in apps/web/routes/auth/required/writer/DocumentRetrievalRoutesImpl.scala
override val authedRoutes: tsec.authentication.TSecAuthService[
  (user.User, model.trace.Id),
  tsec.authentication.AugmentedJWT[tsec.mac.jca.HMACSHA512, model.user.Id],
  Effect
] = tsec.authentication.TSecAuthService {
  case req@POST -> Root / "setup-intent" asAuthed ((user, traceID)) =>
    (for {
      typedRequest <- req.request.as[apps.requests.stripe.PaymentIntent]
                    .attemptAndAbortSequenceOnErrorUsingEither
      given model.trace.Id = traceID
      _ <- stripeService.setOuroboriteSubscription[Either[model.Error, *]](
           user.stripeCustomerId, typedRequest.subscription
         ).abortSequenceOnError
      intent <- stripeService.configureSetupPaymentIntent[Either[model.Error, *]](
               user.stripeCustomerId
             ).abortSequenceOnError
      
      // Encoding and response creation
      encodedBody <- org.http4s.EntityEncoder[Effect, apps.responses.stripe.PaymentIntent]
                   .toEntity(apps.responses.stripe.PaymentIntent(intent)).body.pure[Effect]
                   .attemptAndAbortSequenceOnErrorUsingEither
      
      encodedResponse = http4s.Response[Effect](
        status = http4s.Status.Ok,
        body = encodedBody
      )
    } yield encodedResponse).value.flatMap {
      case Right(success) => success.pure[Effect]
      case Left(error) => web.error.defaultMapper[Effect].findErrorResponse(error)
    }
}
```

### 6.2 Route Assembly

```scala
def makeRoutes[Effect[_]](
  services: server.Services[Effect],
  authenticator: tsec.authentication.Authenticator[Effect, user.Id, user.User, 
                tsec.authentication.AugmentedJWT[tsec.mac.jca.HMACSHA512, user.Id]],
  porcupineSecret: PorcupineSecret
)(using
  effect: cats.effect.Async[Effect],
  logger: model.logger.Factory[Effect]
): HttpRoutes[Effect] = {
  // Authentication middleware
  val authMiddleware = tsec.authentication.SecuredRequestHandler(authenticator)
  
  // Routes
  val loginRoutes = routes.auth.Login(services.authenticator)
  val createAccountRoutes = routes.auth.CreateAccount(services.accountCreator)
  val feedbackRoutes = routes.auth.required.FeedbackRoutes.Impl(services.feedbackService)
  
  // Combine routes
  loginRoutes.route <+> createAccountRoutes.route <+> 
  authMiddleware.liftService(feedbackRoutes.authedRoutes)
}
```


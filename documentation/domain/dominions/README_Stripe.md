# About Stripe
Stripe is a payment service that we use to process subscriptions, the key elements are:
- Pass a stripe publishable key to the client
- Create a stripe user on account creation
- Generate an intent via the payment service when the user goes to checkout
- Give the client the intent secret key
- Listen on the webhook listener for events impacting the subscription.
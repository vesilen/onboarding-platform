# onboarding-processor

Azure Function (Java 21) that consumes `ApplicationSubmitted` events from the Service Bus queue
`application-submitted`, published by `onboarding-api` after `POST /api/v1/applications`.

| Class | Role |
|---|---|
| `ApplicationSubmittedFunction` | Thin Functions entry point with the `@ServiceBusQueueTrigger` |
| `ApplicationSubmittedHandler` | Parses and validates the message; plain Java, unit tested |
| `ApplicationSubmitted` | Event contract (JSON); unknown fields are ignored |

## Message handling

- Function returns normally: the message is **completed** (removed from the queue).
- Function throws: the message is **abandoned** and redelivered. After 5 deliveries
  (`max_delivery_count` in Terraform) Service Bus moves it to the **dead-letter queue**.
- Only the `applicationId` is logged; names and e-mail addresses are personal data.

## Configuration

The trigger uses `connection = "ServiceBusConnection"` and `queueName = "%SERVICEBUS_QUEUE%"`.
Terraform sets these app settings on the Function App:

| Setting | Value |
|---|---|
| `ServiceBusConnection__fullyQualifiedNamespace` | `sb-ivaninterview-dev-bec.servicebus.windows.net` |
| `ServiceBusConnection__credential` | `managedidentity` |
| `ServiceBusConnection__clientId` | client id of `id-ivaninterview-func-dev-bec` |
| `SERVICEBUS_QUEUE` | `application-submitted` |

No connection strings: the namespace has local (SAS) auth disabled.

## Build and deploy

```bash
./mvnw -pl services/onboarding-processor -am package            # tests + target/azure-functions/func-ivaninterview-dev-bec
./mvnw -pl services/onboarding-processor azure-functions:deploy # uses your az login
```

The app stack is destroyed nightly, so deploy again after every morning `terraform apply`.
Check it in the portal: Function App → Overview → *Functions* tab, and Monitoring → *Log stream*.

## Run locally (optional)

Requires [Azure Functions Core Tools v4](https://learn.microsoft.com/azure/azure-functions/functions-run-local)
and [Azurite](https://learn.microsoft.com/azure/storage/common/storage-use-azurite) for the runtime storage
(`docker run -p 10000-10002:10000-10002 mcr.microsoft.com/azure-storage/azurite`).

```bash
cp services/onboarding-processor/local.settings.json.example services/onboarding-processor/local.settings.json
./mvnw -pl services/onboarding-processor -am package -DskipTests
./mvnw -pl services/onboarding-processor azure-functions:run
```

Locally the connection has no `credential` setting, so it falls back to your `az login`
(you have *Azure Service Bus Data Owner* on the namespace). Note that a local run and the Function in
Azure **compete** for the same messages: each message goes to only one of them.

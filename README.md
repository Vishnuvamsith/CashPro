# CashPro

CashPro is a Maven reactor containing the shared `payment-events` library and the deployable `payment-service` and `payment-processing-service` Spring Boot applications.

## Build and test

Use the repository Maven wrapper and Java 17:

```sh
./mvnw clean verify
```

The verification command builds all modules in dependency order and runs their automated tests. The payment-service integration tests use Testcontainers and require a working Docker installation.

## Build Docker images locally

First package the applications with the Maven wrapper (or run the full verification command above), then run these commands from the repository root. The Dockerfiles copy only the resulting executable JAR into a small Java runtime image:

```sh
./mvnw --batch-mode --no-transfer-progress clean package
docker build -f payment-service/Dockerfile -t cashpro/payment-service:local .
docker build -f payment-processing-service/Dockerfile -t cashpro/payment-processing-service:local .
```

The images run as a non-root user and do not include `.env` files or Maven build output. Runtime configuration is supplied through environment variables rather than baked into the image.

## Run the full local stack

The existing default Compose setup continues to start only the PostgreSQL, Redis, Kafka/Schema Registry, Prometheus, and Grafana infrastructure. Compose now requires local database and Grafana credentials via environment variables; keep them in your shell or an ignored local `.env` file, never commit them. For example, export `POSTGRES_DB`, `POSTGRES_USER`, `POSTGRES_PASSWORD`, `GRAFANA_ADMIN_USER`, and `GRAFANA_ADMIN_PASSWORD` with your own local values. Package the applications first, then build and start both application images by opting into the `apps` profile:

```sh
./mvnw --batch-mode --no-transfer-progress clean package
IMAGE_TAG=local docker compose --profile apps up --build
```

The payment API is available at <http://localhost:8080> and the processing service at <http://localhost:8081>. Use the same `POSTGRES_USER` and `POSTGRES_PASSWORD` values for the database and both applications. For a one-command example, substitute your own local credentials below:

```sh
POSTGRES_DB=cashpro POSTGRES_USER=cashpro POSTGRES_PASSWORD='your-local-password' GRAFANA_ADMIN_USER=admin GRAFANA_ADMIN_PASSWORD='your-grafana-password' IMAGE_TAG=local docker compose --profile apps up --build
```

To stop the stack, run `docker compose --profile apps down`. The named database and Kafka volumes are retained unless explicitly removed with `-v`.

## Continuous integration

GitHub Actions runs `./mvnw clean verify` for every push and pull request. Only after the Maven reactor and tests pass does it build both service images, tagging each with the full commit SHA (for example, `cashpro/payment-service:<commit-sha>`). CI loads these images locally and does not push them to a registry.

CI and Docker builds require no repository secrets. Credentials are passed at runtime and are not copied into the images. Do not add real credentials to application configuration, Dockerfiles, or the repository.

## Payment observability and alerting

With both applications running under the Compose `apps` profile, Prometheus scrapes `/actuator/prometheus` from the payment API and payment processor every 15 seconds. Prometheus is available at <http://localhost:9090>; Grafana is at <http://localhost:3000>. Grafana provisions the **CashPro Payment Operations** dashboard and its Prometheus datasource on startup.

The payment API exports `cashpro_payment_requests_total`, bounded `cashpro_payment_outcomes_total{outcome="success|failure|duplicate"}`, `cashpro_payment_idempotency_duplicates_total`, and the histogram `cashpro_payment_request_duration_seconds`. The processing service exports `cashpro_kafka_messages_total{outcome="success|failure"}`, `cashpro_kafka_consumer_processing_seconds`, retry and DLQ counters. No payment ID, client ID, or idempotency key is used as a metric label.

### Kafka consumer lag

Spring Boot's Kafka metrics do not provide authoritative consumer-group lag. Compose runs `kafka-exporter` against both internal brokers; Prometheus scrapes it on port 9308. The dashboard and `KafkaConsumerLagHigh` alert use `kafka_consumergroup_lag` for consumer group `payment-processing-service-group` and topic `payment-events`. For another cluster, deploy a Kafka exporter (or an equivalent broker/JMX metrics exporter) with access to the brokers and consumer offsets, then update its Prometheus target. DLQ topic `payment-events-dlq` has three partitions to match the input topic and is provisioned with the processor.

### Alerts and response

Rules are in `monitoring/alerts.yml`; Prometheus evaluates them every 15 seconds. The `for` value is the sustained evaluation window before an alert fires. Prometheus exposes firing rules at <http://localhost:9090/alerts>.

| Alert | Threshold and window | Initial response |
| --- | --- | --- |
| `ElevatedPaymentFailures` | More than 5% failures over 5 minutes, with at least 0.1 successful or failed requests per second; excludes expected idempotency duplicates | Check payment-service logs, PostgreSQL/Redis connectivity, and recent changes; restore the dependency and verify the failure ratio returns to baseline. |
| `HighPaymentProcessingLatency` | Consumer p95 over 2 seconds for 10 minutes | Check processor saturation, database health, Kafka errors/retries, and consumer lag; scale or restore the constrained dependency. |
| `CashProServiceUnavailable` | Prometheus scrape is down for 2 minutes | Check the app container and startup logs, Compose `apps` profile, and actuator reachability. |
| `KafkaConsumerLagHigh` | More than 100 queued messages for the payment consumer group for 10 minutes | Check consumer availability, retry/DLQ rates, database throughput, and broker health; inspect the DLQ before replaying messages. |

These rules are evaluated by Prometheus, but external notifications are not configured yet; monitor the Prometheus Alerts page until an Alertmanager/notification destination is chosen.

### Controlled validation

1. Start the stack with the `apps` profile and credentials above. Confirm both application scrape targets and `kafka-exporter` are **UP** on <http://localhost:9090/targets>.
2. Submit a valid `POST /api/payments` with an `Idempotency-Key`; repeat the same key to increment the separate duplicate series. Check the dashboard and <http://localhost:8080/actuator/prometheus>.
3. To exercise consumer failure/retry/DLQ metrics in a disposable local environment, publish a malformed Avro event to `payment-events`. Observe `cashpro_kafka_messages_total{outcome="failure"}`, retry attempts, and (after recovery) the DLQ counter and `payment-events-dlq` topic. Do not do this against production data.
4. Use Prometheus **Status → Rules** and the Alerts page to verify loaded rules. For threshold tests, use a disposable Prometheus test setup or temporarily lower a rule threshold, confirm it fires after its `for` duration, then restore the committed threshold.

Prometheus and Grafana rules/configuration and the provisioned dashboard are version-controlled under `monitoring/`.

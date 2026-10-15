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

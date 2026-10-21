# Lab 7 — E-Commerce приложение

## Сервисы и порты

| Сервис | Локальный порт | Docker-порт | БД |
|---|---|---|---|
| api-gateway | 8080 | 8080 | — |
| auth-service | 8081 | 8080 | auth_db (Postgres) |
| user-service | 8082 | 8080 | users_db (Postgres) + Redis |
| order-service | 8083 | 8080 | orders_db (Postgres) |
| kafka-broker-0/1/2 | 29092 / 29093 / 29094 | 9091 (internal) | KRaft, 3 брокера, RF=3 |
| schema-registry | 8087 | 8081 (internal) | Avro + Schema Registry |
| kafka-ui | 8086 | 8080 | Kafka UI |

## Архитектура

```mermaid
flowchart TB
    User([Покупатель / администратор]) <-->|HTTPS REST + JWT| GW[API Gateway<br/>:8080<br/>JWKS + JwtClaimAuthorize]

    GW -->|/api/auth/**| Auth[Auth Service<br/>:8081 local / :8080 docker<br/>RSA JWT, outbox, audit]
    GW -->|/api/users/**| UserSvc[User Service<br/>:8082 local / :8080 docker<br/>profiles, addresses, Redis-кэш]
    GW -->|/api/orders/**, /api/products/**| Order[Order Service<br/>:8083 local / :8080 docker<br/>orders, products]

    GW -->|JWKS /.well-known/jwks.json| Auth
    Order -->|REST + CircuitBreaker| UserSvc
    UserSvc -->|REST internal<br/>disable / enable / delete| Auth

    Auth -->|JDBC + Liquibase| AuthDB[(PostgreSQL<br/>auth_db :5432)]
    UserSvc -->|JDBC + Liquibase| UserDB[(PostgreSQL<br/>users_db :5432)]
    Order -->|JDBC + Liquibase| OrderDB[(PostgreSQL<br/>orders_db :5432)]
    UserSvc <-->|кэш профилей| Redis[(Redis<br/>:6379)]

    Auth -->|Avro produce<br/>user-registration| Kafka[(Kafka 3 брокера<br/>:29092-29094<br/>internal :9091)]
    Kafka -->|Avro consume<br/>user-registration| UserSvc
    UserSvc -->|Avro produce<br/>user-registration-result| Kafka
    Kafka -->|Avro consume<br/>user-registration-result| Auth

    Kafka <-->|Avro-схемы, топик _schemas| SR[Schema Registry<br/>:8087 / internal :8081]
    KUI[Kafka UI<br/>:8086] --> Kafka
    KUI --> SR
```

## Запуск в Docker

Образы `auth-service`, `user-service`, `order-service` и `api-gateway` собираются из исходников внутри Docker (multi-stage Maven build), предварительный `mvn package` не нужен.

```bash
cp .env.example .env
docker compose build
docker compose up -d
```

При повторной сборке Maven-зависимости берутся из BuildKit-кэша, а локальные образы `shoplab/*:local` переиспользуются без пулла. Инфраструктурные образы (`postgres`, `kafka`, `redis`, `liquibase`) скачиваются Docker-ом только при первом запуске и дальше берутся из локального кэша.

### Swagger UI

Единый Swagger UI с API всех сервисов доступен на API Gateway:

```text
http://localhost:8080/swagger-ui.html
```

По умолчанию открывается объединённая спецификация `All Services` со всеми роутами. В выпадающем списке также можно отдельно посмотреть API каждого сервиса.

Спецификации OpenAPI:

```text
http://localhost:8080/openapi/all
http://localhost:8080/openapi/auth
http://localhost:8080/openapi/users
http://localhost:8080/openapi/orders
```

Для защищённых методов получите `accessToken` через `POST /api/auth/login`, нажмите `Authorize` в Swagger UI и вставьте токен.

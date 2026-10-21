# Приложение, тестовые данные, индексы 

## 1. Приложение

Микросервисы `auth/user/order/gateway` (Java 21, Spring Boot, JdbcTemplate —
без JPA), инфраструктура из flowcommerce (Postgres×3 + Liquibase, Redis,
Kafka-кластер из 3 брокеров + Schema Registry/Avro, JWKS, CircuitBreaker).
Сборка и запуск:

```bash
cp .env.example .env && docker compose up --build -d
mvn test -Dtest='!*RepositoryTest'
mvn test
```

Покрытие: unit-тесты сервисов (`AuthServiceImplTest`, `ProfileServiceTest`,
`OrderFacadeTest`), MockMvc (`AuthControllerTest`, `OrderControllerTest`),
репозиторные Testcontainers (`UserJdbcRepositoryTest`, `ProfileJdbcRepositoryTest`,
`ProductJdbcRepositoryTest`), фильтр gateway (`JwtClaimAuthorizeGatewayFilterFactoryTest`).

## 2. Тестовые данные

- Миграции `008_seed`/`006_seed`: роли ADMIN/USER; 3 профиля + 3 адреса;
  3 категории, 7 товаров, 3 заказа + 4 позиции.
- Стартовый ADMIN создаётся `InitializerService` из `AUTH_ADMIN_USERNAME/PASSWORD`.
- Проверка наполнения:

```sql
SELECT (SELECT COUNT(*) FROM roles) AS roles,
       (SELECT COUNT(*) FROM products) AS products,
       (SELECT COUNT(*) FROM orders) AS orders;
```

## 3. Индексы

| Индекс | Запросы | Тип плана |
|---|---|---|
| `users(username)` UNIQUE | логин, `existsByUsername` | Index Scan |
| `profiles(email)`, `profiles(auth_user_id)` UNIQUE | `findByEmail` (order→user REST) | Index Scan |
| `products(category_id)`, `products(sku)` | каталог, join категорий | Index/Bitmap Scan |
| `products(active, price)` | витрина `WHERE active AND price<=? ORDER BY price` | Index Scan, без Sort |
| `orders(status, created_at DESC)` | фильтр по статусу/дате, `ORDER BY created_at DESC` | Index Scan, без Sort |
| `orders(buyer_profile_id)` | заказы покупателя | Index Scan |
| `orders(deleted) WHERE NOT deleted` частичный | все витринные выборки (`deleted=FALSE`) | Index Scan |
| `registration_event(status, next_attempt_at)` | outbox-пачки саги | Index Scan |
| `auth_audit_log(user_id, created_at)`, `(action, created_at)` | журнал пользователя/действия | Index Scan |
| `order_status_log(order_id, created_at)` | история заказа | Index Scan |
| `order_items(order_id/product_id)` | детали заказа, топ товаров | Index Scan |

## 4. Триггер

Функция `touch_updated_at()` + 4 триггера `BEFORE UPDATE` (`users`, `profiles`,
`products`, `orders`) проставляют `updated_at` на стороне БД, ручного
`updated_at = now()` в `UPDATE`-запросах нет. Триггер `BEFORE UPDATE ... FOR EACH ROW`
не меняет план запроса: он срабатывает при выполнении уже построенного `UPDATE`.
Проверка после поднятия стенда:

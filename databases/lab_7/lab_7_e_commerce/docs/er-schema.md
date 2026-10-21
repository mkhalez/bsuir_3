# ER-структура данных

## auth_db

```mermaid
erDiagram
    roles ||--o{ user_roles : содержит
    users ||--o{ user_roles : имеет
    users ||--o{ registration_event : порождает
    users ||--o{ auth_audit_log : логируется
    registration_event ||--|| registration_result : "saga-id (логически)"

    roles {
        BIGINT id PK
        VARCHAR name UK
    }
    users {
        BIGINT id PK
        VARCHAR username UK
        TEXT password_hash
        BOOLEAN active
        TIMESTAMPTZ created_at
    }
    user_roles {
        BIGINT user_id PK
        BIGINT role_id PK
    }
    registration_event {
        UUID id PK
        BIGINT user_id FK
        TEXT payload
        TEXT status
        INT attempt_count
        TIMESTAMPTZ next_attempt_at
    }
    registration_result {
        UUID id PK
        TEXT status
        TEXT message
    }
    auth_audit_log {
        BIGINT id PK
        BIGINT user_id FK
        VARCHAR username
        VARCHAR action
        TEXT details
        VARCHAR ip_address
    }
```

- users(1)—(N)user_roles(N)—(1)roles: многие-ко-многим через связку.
- registration_event(N)—(1)users: событие принадлежит пользователю;
  при компенсации (FAIL) связь обнуляется, пользователь удаляется.
- auth_audit_log(N)—(1)users: событие журнала опционально привязано к пользователю
  (NULL для анонимных попыток логина).

## users_db

```mermaid
erDiagram
    profiles ||--o{ addresses : имеет
    profiles ||--o{ user_audit_log : логируется
    registration_inbox ||--|| registration_outbox : "saga-id (логически)"

    profiles {
        BIGINT id PK
        BIGINT auth_user_id UK
        VARCHAR first_name
        VARCHAR last_name
        VARCHAR email UK
        VARCHAR phone
        DATE birth_date
        BOOLEAN active
    }
    addresses {
        BIGINT id PK
        BIGINT profile_id FK
        VARCHAR city
        VARCHAR street
        VARCHAR zip
        BOOLEAN is_default
    }
    registration_inbox {
        UUID id PK
        TEXT payload
        TEXT status
    }
    registration_outbox {
        UUID id PK
        TEXT status
        TEXT message
        BOOLEAN sent
    }
    user_audit_log {
        BIGINT id PK
        BIGINT profile_id FK
        BIGINT auth_user_id
        VARCHAR action
        TEXT details
    }
```

- profiles(1)—(N)addresses: каскадное удаление адресов вместе с профилем.
- user_audit_log(N)—(1)profiles: SET NULL при удалении профиля.

## orders_db

```mermaid
erDiagram
    categories ||--o{ products : содержит
    orders ||--o{ order_items : состоит_из
    products ||--o{ order_items : входит_в
    orders ||--o{ order_status_log : история
    orders ||--o{ order_audit_log : логируется

    categories {
        BIGINT id PK
        VARCHAR name UK
        TEXT description
    }
    products {
        BIGINT id PK
        BIGINT category_id FK
        VARCHAR sku UK
        VARCHAR name
        NUMERIC price
        INT stock
        BOOLEAN active
    }
    orders {
        BIGINT id PK
        BIGINT buyer_profile_id
        VARCHAR buyer_email
        TEXT status
        NUMERIC total
        BOOLEAN deleted
    }
    order_items {
        BIGINT id PK
        BIGINT order_id FK
        BIGINT product_id FK
        INT quantity
        NUMERIC unit_price
        NUMERIC line_total
    }
    order_status_log {
        BIGINT id PK
        BIGINT order_id FK
        TEXT old_status
        TEXT new_status
        VARCHAR changed_by
    }
    order_audit_log {
        BIGINT id PK
        BIGINT order_id FK
        VARCHAR action
        TEXT details
        VARCHAR actor
    }
```

- order_items — связка «многие-ко-многим» заказов и товаров с атрибутами
  (quantity, unit_price, line_total); удаление заказа каскадит позиции;
  товар удалить нельзя, пока есть позиции (RESTRICT).
- order_status_log — история переходов статусов (old→new, кто, когда).

## Межсервисные (логические, без FK)

```mermaid
flowchart LR
    AU["auth.users.id"] -->|"profiles.auth_user_id<br/>1—1, создаётся сагой"| P["user.profiles"]
    P -->|"orders.buyer_profile_id<br/>N—1, REST-проверка + CircuitBreaker"| O["orders"]
```

- `profiles.auth_user_id → auth.users.id` (1—1, создаётся сагой).
- `orders.buyer_profile_id → user.profiles.id` (N—1, проверяется REST-вызовом
  перед созданием заказа )

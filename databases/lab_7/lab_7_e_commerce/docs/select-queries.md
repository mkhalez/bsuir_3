# Выборка данных (SELECT)

## Фильтры и курсорная пагинация

```sql
SELECT ... FROM profiles
WHERE (? IS NULL OR first_name ILIKE '%'||?||'%')
  AND (? IS NULL OR last_name ILIKE '%'||?||'%')
  AND (? IS NULL OR id > ?)
ORDER BY id LIMIT ?;
SELECT p.*, c.name FROM products p JOIN categories c ON c.id=p.category_id
WHERE p.active=TRUE AND (? IS NULL OR p.category_id=?)
  AND (? IS NULL OR p.price<=?) AND (? IS NULL OR p.name ILIKE '%'||?||'%')
  AND (? IS NULL OR ? IS NULL OR (p.price, p.id) > (?, ?))
ORDER BY p.price, p.id LIMIT ?;
SELECT id, buyer_profile_id, buyer_email, status, total, created_at FROM orders
WHERE deleted=FALSE AND (? IS NULL OR status=?)
  AND (? IS NULL OR created_at>=?) AND (? IS NULL OR created_at<=?)
  AND (? IS NULL OR ? IS NULL OR (created_at, id) < (?, ?))
ORDER BY created_at DESC, id DESC LIMIT ?;
SELECT id, status, total, created_at FROM orders
WHERE buyer_profile_id=? AND deleted=FALSE
  AND (? IS NULL OR ? IS NULL OR (created_at, id) < (?, ?))
ORDER BY created_at DESC, id DESC LIMIT ?;
```

## JOIN

```sql
SELECT r.name FROM roles r JOIN user_roles ur ON ur.role_id=r.id WHERE ur.user_id=? ORDER BY r.name;
SELECT oi..., p.name FROM order_items oi JOIN products p ON p.id=oi.product_id WHERE oi.order_id=? ORDER BY oi.id;
SELECT c.id, c.name, COUNT(p.id) FROM categories c LEFT JOIN products p ON p.category_id=c.id
GROUP BY c.id, c.name ORDER BY c.name;
SELECT p.id, p.email, ..., COUNT(a.id) FROM profiles p LEFT JOIN addresses a ON a.profile_id=p.id
GROUP BY p.id ORDER BY p.id LIMIT ?;
```

## Агрегаты

```sql
SELECT COUNT(*) FROM users;
SELECT active, COUNT(*) FROM profiles GROUP BY active;
SELECT status, COUNT(*) FROM registration_event GROUP BY status ORDER BY status;
SELECT status, COUNT(*), COALESCE(SUM(total),0) FROM orders
  WHERE deleted=FALSE GROUP BY status ORDER BY status;
SELECT DATE_TRUNC('day',created_at)::date, COUNT(*), AVG(total) FROM orders
  WHERE created_at >= now()-(?||' days')::interval AND deleted=FALSE GROUP BY 1 ORDER BY 1;
SELECT p.id, p.name, SUM(oi.quantity), SUM(oi.line_total) FROM order_items oi
  JOIN products p ON p.id=oi.product_id GROUP BY p.id,p.name,p.sku ORDER BY sold_qty DESC LIMIT ?;
SELECT username, COUNT(*) FROM auth_audit_log GROUP BY username ORDER BY 2 DESC LIMIT ?;
```

## Outbox-выборка саги (FOR UPDATE SKIP LOCKED)

```sql
SELECT ... FROM registration_event
WHERE attempt_count<=? AND ((status='CREATED' AND next_attempt_at<now())
   OR (status='PRE_SENDING' AND sending_started_at<=now()-INTERVAL '2 minutes'))
ORDER BY created_at LIMIT ? FOR UPDATE SKIP LOCKED;
```

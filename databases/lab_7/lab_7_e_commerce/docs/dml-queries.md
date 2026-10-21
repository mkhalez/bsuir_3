# Манипуляции данными (INSERT/UPDATE/DELETE)

## INSERT

```sql
INSERT INTO users(username, password_hash, active) VALUES (?,?,?) RETURNING id;
INSERT INTO user_roles(user_id, role_id) SELECT ?, id FROM roles WHERE name=? ON CONFLICT DO NOTHING;
INSERT INTO registration_event(id, user_id, payload, status, attempt_count, next_attempt_at)
VALUES (?,?,?::text,'CREATED',0,now());
INSERT INTO profiles(auth_user_id, first_name, last_name, email, phone, birth_date)
VALUES (?,?,?,?,?,?) RETURNING id;
INSERT INTO addresses(profile_id, city, street, zip, is_default) VALUES (?,?,?,?,?) RETURNING id;
INSERT INTO orders(buyer_profile_id, buyer_email, status, total) VALUES (?,?, 'CREATED', ?) RETURNING id;
INSERT INTO order_items(order_id, product_id, quantity, unit_price, line_total) VALUES (?,?,?,?,?);
INSERT INTO categories(name) VALUES (?) ON CONFLICT (name) DO NOTHING;
INSERT INTO products(category_id, sku, name, price, stock) VALUES (?,?,?,?,?) RETURNING id;
INSERT INTO auth_audit_log(user_id, username, action, details, ip_address) VALUES (?,?,?,?,?);
INSERT INTO order_status_log(order_id, old_status, new_status, changed_by) VALUES (?,?,?,?);
```

## UPDATE

```sql
UPDATE users SET active=? WHERE id=?;
UPDATE profiles SET active=? WHERE id=?;
UPDATE profiles SET first_name=COALESCE(?,first_name), last_name=COALESCE(?,last_name), email=COALESCE(?,email), phone=COALESCE(?,phone), birth_date=COALESCE(?,birth_date) WHERE id=?;
UPDATE products SET name=COALESCE(?,name), price=COALESCE(?,price), stock=COALESCE(?,stock),
       active=COALESCE(?,active) WHERE id=?;
UPDATE orders SET status=? WHERE id=? AND deleted=FALSE;
UPDATE products SET stock=stock-? WHERE id=? AND stock>=?;
UPDATE products SET stock=stock+? WHERE id=?;
UPDATE registration_event SET status='PRE_SENDING', attempt_count=attempt_count+1,
       sending_started_at=now(), updated_at=now() WHERE id=?;
UPDATE registration_event SET status='CREATED',
       next_attempt_at=now()+(?||' seconds')::interval, updated_at=now() WHERE id=?;
```

## DELETE

```sql
DELETE FROM user_roles WHERE user_id=?;  DELETE FROM users WHERE id=?;
DELETE FROM addresses WHERE profile_id=?;  DELETE FROM profiles WHERE id=?;
DELETE FROM products WHERE id=?;
UPDATE orders SET deleted=TRUE WHERE id=?;
```
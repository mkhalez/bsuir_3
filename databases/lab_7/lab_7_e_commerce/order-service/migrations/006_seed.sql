INSERT INTO categories(name, description) VALUES
 ('Smartphones','Mobile phones and accessories'),
 ('Laptops','Notebooks and ultrabooks'),
 ('Audio','Headphones and speakers')
ON CONFLICT (name) DO NOTHING;
INSERT INTO products(category_id, sku, name, price, stock, active)
SELECT c.id, x.sku, x.name, x.price, x.stock, TRUE FROM categories c
JOIN (VALUES
 ('Smartphones','IP15-128','iPhone 15 128GB', 3499.00, 25),
 ('Smartphones','SGS24-256','Samsung Galaxy S24 256GB', 3299.00, 30),
 ('Smartphones','XMI14-512','Xiaomi 14 512GB', 2499.00, 40),
 ('Laptops','MBP14-M3','MacBook Pro 14 M3', 8999.00, 10),
 ('Laptops','LTP-T14','Lenovo ThinkPad T14', 5499.00, 15),
 ('Audio','SNY-WH1000','Sony WH-1000XM5', 1499.00, 50),
 ('Audio','JBL-FLIP6','JBL Flip 6', 499.00, 100)
) AS x(cat, sku, name, price, stock) ON x.cat = c.name
ON CONFLICT (sku) DO NOTHING;
INSERT INTO orders(buyer_profile_id, buyer_email, status, total) VALUES
 (1, 'ivan.petrov@shop.by', 'DELIVERED', 3998.00),
 (1, 'ivan.petrov@shop.by', 'PAID', 1499.00),
 (2, 'anna.sidorova@shop.by', 'CREATED', 499.00);
INSERT INTO order_items(order_id, product_id, quantity, unit_price, line_total)
SELECT o.id, p.id, x.qty, p.price, p.price * x.qty
FROM orders o
JOIN (VALUES (1,'IP15-128',1),(1,'JBL-FLIP6',1),(2,'SNY-WH1000',1),(3,'JBL-FLIP6',1)) AS x(ord, sku, qty) ON x.ord = o.id
JOIN products p ON p.sku = x.sku;

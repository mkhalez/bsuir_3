INSERT INTO profiles(auth_user_id, first_name, last_name, email, phone, birth_date, active) VALUES
 (1, 'Ivan', 'Petrov', 'ivan.petrov@shop.by', '+375291000001', '1998-04-12', TRUE),
 (2, 'Anna', 'Sidorova', 'anna.sidorova@shop.by', '+375292000002', '2000-09-03', TRUE),
 (3, 'Oleg', 'Kozlov', 'oleg.kozlov@shop.by', '+375293000003', '1995-01-25', FALSE)
ON CONFLICT (email) DO NOTHING;
INSERT INTO addresses(profile_id, city, street, zip, is_default)
SELECT p.id, x.city, x.street, x.zip, x.is_default FROM profiles p
JOIN (VALUES
 ('ivan.petrov@shop.by','Minsk','Lenina 10-5','220030',TRUE),
 ('ivan.petrov@shop.by','Minsk','Kirova 3-12','220030',FALSE),
 ('anna.sidorova@shop.by','Brest','Sovetskaya 25-1','224000',TRUE)
) AS x(email, city, street, zip, is_default) ON x.email = p.email;

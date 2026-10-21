UPDATE profiles
SET auth_user_id = auth_user_id + 1000
WHERE email IN ('anna.sidorova@shop.by', 'oleg.kozlov@shop.by');

CREATE OR REPLACE FUNCTION touch_updated_at() RETURNS TRIGGER AS $$
BEGIN
    NEW.updated_at = now();
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;
DROP TRIGGER IF EXISTS trg_touch_updated_at_products ON products;
CREATE TRIGGER trg_touch_updated_at_products BEFORE UPDATE ON products FOR EACH ROW EXECUTE FUNCTION touch_updated_at();
DROP TRIGGER IF EXISTS trg_touch_updated_at_orders ON orders;
CREATE TRIGGER trg_touch_updated_at_orders BEFORE UPDATE ON orders FOR EACH ROW EXECUTE FUNCTION touch_updated_at();

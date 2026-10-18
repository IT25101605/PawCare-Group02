-- Only needed if database schema is managed manually rather than Hibernate ddl-auto=update.
-- Run once, after selecting pawcare_db. Do not remove products referenced by order_items.
ALTER TABLE products ADD COLUMN is_deleted BOOLEAN NOT NULL DEFAULT FALSE;
CREATE INDEX idx_products_is_deleted ON products (is_deleted);

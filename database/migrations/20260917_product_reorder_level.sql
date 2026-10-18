-- Additive MySQL migration. Existing prices, stock, IDs and relationships are unchanged.
-- Safe to execute before or after Hibernate's ddl-auto=update.
SET @has_reorder = (SELECT COUNT(*) FROM information_schema.columns
    WHERE table_schema = DATABASE() AND table_name = 'products' AND column_name = 'reorder_level');
SET @reorder_sql = IF(@has_reorder = 0,
    'ALTER TABLE products ADD COLUMN reorder_level INT NOT NULL DEFAULT 0', 'SELECT 1');
PREPARE reorder_stmt FROM @reorder_sql;
EXECUTE reorder_stmt;
DEALLOCATE PREPARE reorder_stmt;

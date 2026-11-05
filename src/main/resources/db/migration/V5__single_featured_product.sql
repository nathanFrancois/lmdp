-- Serialize selection changes, including when no product is currently featured.
CREATE TABLE catalog_feature_lock (id INTEGER PRIMARY KEY);
INSERT INTO catalog_feature_lock (id) VALUES (1);

-- Preserve one selection if several products were highlighted before this migration.
UPDATE products SET featured = FALSE
WHERE featured = TRUE AND id <> (SELECT MIN(id) FROM products WHERE featured = TRUE);

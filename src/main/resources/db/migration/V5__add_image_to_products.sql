-- Ajoute la photo (facultative) associée à chaque création, gérée depuis le
-- back-office (/admin) et servie via /uploads/** (voir WebConfig / ImageStorageService).
ALTER TABLE products ADD COLUMN image_filename VARCHAR(255) NULL;


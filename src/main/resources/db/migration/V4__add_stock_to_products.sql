-- Ajoute la quantité en stock, gérée depuis le back-office d'administration.
ALTER TABLE products ADD COLUMN stock INT NOT NULL DEFAULT 0;

UPDATE products SET stock = 10 WHERE available = TRUE;
UPDATE products SET stock = 0 WHERE available = FALSE;


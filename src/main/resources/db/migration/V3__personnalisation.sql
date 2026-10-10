-- Ajoute la distinction entre un objet déjà gravé (page "Créations") et un objet
-- proposé à la personnalisation sur devis (page "Personnalisation").
ALTER TABLE products ADD COLUMN customizable BOOLEAN NOT NULL DEFAULT FALSE;


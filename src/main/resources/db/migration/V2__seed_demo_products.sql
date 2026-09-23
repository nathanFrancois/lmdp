-- Jeu de données de démonstration : ni les visuels, ni les prix ne constituent
-- un catalogue réel tant qu'aucune donnée définitive n'a été fournie.
INSERT INTO products (id, name, description, price, available) VALUES
    ('demo-gobelet-feuille', 'Gobelet « Feuille d''olivier » (démo)',
     'Gobelet en verre gravé à la main d''une feuille d''olivier stylisée, dans l''esprit végétal de la maison.',
     24.90, TRUE),
    ('demo-carafe-brindilles', 'Carafe « Brindilles » (démo)',
     'Carafe en verre soufflé, gravée d''un motif de brindilles fines courant tout autour du col.',
     39.50, TRUE),
    ('demo-verre-fleur', 'Verre « Fleur ouverte » (démo)',
     'Petit verre gravé d''une fleur épanouie, aux pétales délicatement détaillés.',
     18.00, TRUE),
    ('demo-vase-liseron', 'Vase « Liseron » (démo)',
     'Vase en verre gravé d''un motif de liseron grimpant, du pied jusqu''au col.',
     54.00, FALSE),
    ('demo-coupelle-graminee', 'Coupelle « Graminée » (démo)',
     'Coupelle en verre gravée de fines graminées, idéale pour un intérieur épuré.',
     21.50, TRUE);


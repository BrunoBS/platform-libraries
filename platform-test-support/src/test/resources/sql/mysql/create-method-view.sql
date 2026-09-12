INSERT INTO catalogs (name, active)
VALUES ('Catálogo do método', TRUE);

CREATE OR REPLACE VIEW vw_method_catalogs AS
SELECT id, name
FROM catalogs;

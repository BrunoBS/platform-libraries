CREATE VIEW vw_platform_resource_schemas AS
SELECT resource_type, resource_code, schema_version, definition
FROM resource_schema
WHERE active = 1;

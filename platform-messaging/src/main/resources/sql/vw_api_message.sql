CREATE VIEW vw_api_message AS
SELECT code, message_key, locale, message, solution, http_status
FROM api_message
WHERE active = 1;

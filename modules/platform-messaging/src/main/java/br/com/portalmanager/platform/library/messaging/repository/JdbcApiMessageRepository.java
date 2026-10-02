package br.com.portalmanager.platform.library.messaging.repository;

import br.com.portalmanager.platform.library.messaging.config.PlatformMessagingProperties;
import br.com.portalmanager.platform.library.messaging.model.ApiMessage;
import org.springframework.jdbc.core.JdbcTemplate;

import java.util.Locale;
import java.util.Optional;

public class JdbcApiMessageRepository implements ApiMessageRepository {

    private final JdbcTemplate jdbcTemplate;
    private final PlatformMessagingProperties properties;

    public JdbcApiMessageRepository(JdbcTemplate jdbcTemplate, PlatformMessagingProperties properties) {
        this.jdbcTemplate = jdbcTemplate;
        this.properties = properties;
    }

    public void validateSource() {
        String tableName = properties.getDatasource().getViewName();
        String sql = """
            SELECT code, message_key, locale, message, solution, http_status
            FROM %s
            WHERE 1 = 0
            """.formatted(tableName);
        jdbcTemplate.query(sql, rs -> null);
    }

    @Override
    public Optional<ApiMessage> find(String messageKey, Locale locale) {
        String tableName = properties.getDatasource().getViewName();

        String sql = """
            SELECT code, message_key, locale, message, solution, http_status 
            FROM %s 
            WHERE message_key = ? AND locale = ? 
            LIMIT 1
            """.formatted(tableName);


        return Optional.ofNullable(jdbcTemplate.query(
                sql,
                rs -> {
                    if (rs.next()) {
                        return new ApiMessage(
                                rs.getString("code"),
                                rs.getString("message_key"),
                                rs.getString("locale"),
                                rs.getString("message"),
                                rs.getString("solution"),
                                rs.getInt("http_status")
                        );
                    }
                    return null;
                },
                messageKey,
                locale.toLanguageTag()
        ));
    }
}

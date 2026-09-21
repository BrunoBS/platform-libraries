package br.com.portalmanager.platform.messaging.repository;

import br.com.portalmanager.platform.messaging.config.PlatformMessagingProperties;
import br.com.portalmanager.platform.messaging.model.ApiMessage;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.ResultSetExtractor;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Locale;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class JdbcApiMessageRepositoryTest {

    private JdbcTemplate jdbcTemplate;
    private PlatformMessagingProperties properties;
    private JdbcApiMessageRepository repository;

    @BeforeEach
    void setUp() {
        jdbcTemplate = mock(JdbcTemplate.class);
        properties = new PlatformMessagingProperties();
        // Configura o nome da tabela/view padrão para o teste
        properties.getDatasource().setViewName("vw_api_message");

        repository = new JdbcApiMessageRepository(jdbcTemplate, properties);
    }

    @Test
    void shouldReturnApiMessageWhenFoundInDatabase() throws SQLException {
        // Arrange
        String messageKey = "user.not.found";
        Locale locale = Locale.of("pt", "BR");

        // Mock do ResultSet com os dados simulados do banco
        ResultSet resultSet = mock(ResultSet.class);
        when(resultSet.next()).thenReturn(true);
        when(resultSet.getString("code")).thenReturn("ERR-404");
        when(resultSet.getString("message_key")).thenReturn(messageKey);
        when(resultSet.getString("locale")).thenReturn("pt-BR");
        when(resultSet.getString("message")).thenReturn("Usuário não encontrado.");
        when(resultSet.getString("solution")).thenReturn("Verifique o ID informado.");
        when(resultSet.getInt("http_status")).thenReturn(404);

        // Configura o jdbcTemplate para executar o lambda do ResultSetExtractor passando o nosso mock
        when(jdbcTemplate.query(any(String.class), any(ResultSetExtractor.class), eq(messageKey), eq("pt-BR")))
                .thenAnswer(invocation -> {
                    ResultSetExtractor<ApiMessage> extractor = invocation.getArgument(1);
                    return extractor.extractData(resultSet);
                });

        // Act
        Optional<ApiMessage> result = repository.find(messageKey, locale);

        // Assert
        assertTrue(result.isPresent());
        ApiMessage message = result.get();
        assertEquals("ERR-404", message.code());
        assertEquals(messageKey, message.messageKey());
        assertEquals("pt-BR", message.locale());
        assertEquals("Usuário não encontrado.", message.message());
        assertEquals("Verifique o ID informado.", message.solution());
        assertEquals(404, message.httpStatus());
    }

    @Test
    void shouldReturnEmptyOptionalWhenNotFoundInDatabase() throws SQLException {
        // Arrange
        String messageKey = "unknown.key";
        Locale locale = Locale.US;

        ResultSet resultSet = mock(ResultSet.class);
        when(resultSet.next()).thenReturn(false); // Simula fim do ResultSet (não achou nada)

        when(jdbcTemplate.query(any(String.class), any(ResultSetExtractor.class), eq(messageKey), eq("en-US")))
                .thenAnswer(invocation -> {
                    ResultSetExtractor<ApiMessage> extractor = invocation.getArgument(1);
                    return extractor.extractData(resultSet);
                });

        // Act
        Optional<ApiMessage> result = repository.find(messageKey, locale);

        // Assert
        assertTrue(result.isEmpty());
    }
}

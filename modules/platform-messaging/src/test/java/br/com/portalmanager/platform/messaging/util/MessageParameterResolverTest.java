package br.com.portalmanager.platform.messaging.util;

import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class MessageParameterResolverTest {

    @Test
    void shouldReplaceParameters() {
        String template = "Usuário {userId} não encontrado.";
        Map<String, Object> params = Map.of("userId", 10);

        String result = MessageParameterResolver.resolve(template, params);

        assertEquals("Usuário 10 não encontrado.", result);
    }

    @Test
    void shouldKeepOriginalTagWhenParameterIsMissing() {
        String template = "O campo {campo} é obrigatório para o usuário {userId}.";
        Map<String, Object> params = Map.of("campo", "E-mail"); // Faltou o 'userId'
        String result = MessageParameterResolver.resolve(template, params);
        assertEquals("O campo E-mail é obrigatório para o usuário {userId}.", result);
    }

    @Test
    void shouldHandleSpecialCharactersInValuesSafely() {
        String template = "O preço do produto é {preco}.";
        Map<String, Object> params = Map.of("preco", "R$ 15,00"); // Possui o caractere '$'

        String result = MessageParameterResolver.resolve(template, params);

        assertEquals("O preço do produto é R$ 15,00.", result);
    }

    @Test
    void shouldBeResilientToNullOrEmptyInputs() {
        assertNull(MessageParameterResolver.resolve(null, Map.of("id", 1)));
        assertEquals("", MessageParameterResolver.resolve("", Map.of("id", 1)));
        assertEquals("Texto puro sem chaves.", MessageParameterResolver.resolve("Texto puro sem chaves.", Map.of()));
        assertEquals("Texto com {chave} mas mapa nulo.", MessageParameterResolver.resolve("Texto com {chave} mas mapa nulo.", null));
    }
}

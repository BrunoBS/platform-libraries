package br.com.portalmanager.platform.library.testing.http;

import br.com.portalmanager.platform.library.testing.http.response.BaseResponse;
import io.restassured.response.ValidatableResponse;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.json.JsonMapper;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class BaseClientTest {

    @Test
    void shouldAdaptHttpResponseToServiceSpecificResponse() {
        ValidatableResponse httpResponse = mock(ValidatableResponse.class);
        TestClient client = new TestClient(
                mock(PlatformRequestSpecificationFactory.class),
                mock(JsonMapper.class)
        );

        TestResponse response = client.adapt(httpResponse);

        assertThat(response.response()).isSameAs(httpResponse);
    }

    @Test
    void shouldSerializeUsingTheConfiguredJsonMapper() {
        JsonMapper mapper = mock(JsonMapper.class);
        when(mapper.writeValueAsString("value")).thenReturn("{\"configured\":true}");
        TestClient client = new TestClient(mock(PlatformRequestSpecificationFactory.class), mapper);
        assertThat(client.serialize("value")).isEqualTo("{\"configured\":true}");
    }

    private static final class TestClient extends BaseClient {

        private TestClient(PlatformRequestSpecificationFactory requests, JsonMapper jsonMapper) {
            super(requests, jsonMapper);
        }

        private String serialize(Object value) {
            return json(value);
        }

        private TestResponse adapt(ValidatableResponse response) {
            return response(response, TestResponse::new);
        }
    }

    private static final class TestResponse extends BaseResponse<TestResponse> {

        private TestResponse(ValidatableResponse response) {
            super(response);
        }
    }
}

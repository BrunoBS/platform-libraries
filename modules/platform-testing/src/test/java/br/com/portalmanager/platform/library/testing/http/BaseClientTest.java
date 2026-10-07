package br.com.portalmanager.platform.library.testing.http;

import br.com.portalmanager.platform.library.testing.http.response.BaseResponse;
import io.restassured.response.ValidatableResponse;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

class BaseClientTest {

    @Test
    void shouldAdaptHttpResponseToServiceSpecificResponse() {
        ValidatableResponse httpResponse = mock(ValidatableResponse.class);
        TestClient client = new TestClient(
                mock(PlatformRequestSpecificationFactory.class)
        );

        TestResponse response = client.adapt(httpResponse);

        assertThat(response.response()).isSameAs(httpResponse);
    }

    private static final class TestClient extends BaseClient {

        private TestClient(PlatformRequestSpecificationFactory requests) {
            super(requests);
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

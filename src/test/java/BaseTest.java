import com.github.tomakehurst.wiremock.WireMockServer;
import io.qameta.allure.Step;
import io.restassured.RestAssured;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.TestInstance;

import static com.github.tomakehurst.wiremock.client.WireMock.*;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class BaseTest {
    protected static final String API_KEY = "qazWSXedc";
    protected AppClient appClient;
    private WireMockServer wireMockServer;

    @BeforeAll
    void startMockServer() {
        wireMockServer = new WireMockServer(8888);
        wireMockServer.start();
        configureFor("localhost", 8888);
        RestAssured.enableLoggingOfRequestAndResponseIfValidationFails();
        appClient = new AppClient(API_KEY);
    }

    @AfterAll
    void stopMockServer() {
        if (wireMockServer != null) {
            wireMockServer.stop();
        }
    }

    @BeforeEach
    void resetMock() {
        reset();
    }

    @Step("Настройка WireMock: эндпоинт {url} вернет статус {status}")
    protected void setupMockEndpoint(String url, int status) {
        stubFor(post(urlEqualTo(url))
                .willReturn(aResponse().withStatus(status)));
    }
}

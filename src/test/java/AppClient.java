import io.qameta.allure.Step;
import io.restassured.http.ContentType;
import io.restassured.response.Response;
import static io.restassured.RestAssured.given;

public class AppClient {
    private static final String APP_URL = "http://localhost:8080/endpoint";
    private final String apiKey;

    public AppClient(String apiKey) {
        this.apiKey = apiKey;
    }

    @Step("Отправка запроса к приложению: action={action}, token={token}")
    public Response sendRequest(String token, String action) {
        return given()
                .header("X-Api-Key", apiKey)
                .contentType("application/x-www-form-urlencoded; charset=UTF-8")
                .formParam("token", token)
                .formParam("action", action)
                .when()
                .post(APP_URL);
    }
}

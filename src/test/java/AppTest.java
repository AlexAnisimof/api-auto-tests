import io.qameta.allure.Description;
import io.qameta.allure.Epic;
import io.qameta.allure.Feature;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import java.util.UUID;
import static org.hamcrest.Matchers.equalTo;

@Epic("Тестирование API внутреннего сервиса")
@Feature("Аутентификация и действия пользователей")
public class AppTest extends BaseTest {

    private String generateValidToken() {
        return UUID.randomUUID().toString().replace("-", "").toUpperCase();
    }

    @Test
    @DisplayName("Успешный LOGIN пользователя")
    @Description("Проверка сохранения токена при успешном ответе внешнего сервиса")
    void testSuccessfulLogin() {
        String token = generateValidToken();
        setupMockEndpoint("/auth", 200);

        appClient.sendRequest(token, "LOGIN")
                .then()
                .statusCode(200)
                .body("result", equalTo("OK"));
    }

    @Test
    @DisplayName("Ошибка LOGIN при отказе внешнего сервиса")
    @Description("Проверка ответа приложения, если внешний сервис вернул 401")
    void testFailedLogin() {
        String token = generateValidToken();
        setupMockEndpoint("/auth", 401);

        appClient.sendRequest(token, "LOGIN")
                .then()
                .statusCode(400)
                .body("result", equalTo("ERROR"));
    }

    @Test
    @DisplayName("Успешное выполнение ACTION после LOGIN")
    @Description("Проверка цепочки LOGIN -> ACTION при успешных ответах внешнего сервиса")
    void testSuccessfulAction() {
        String token = generateValidToken();

        setupMockEndpoint("/auth", 200);
        appClient.sendRequest(token, "LOGIN");

        setupMockEndpoint("/doAction", 200);
        appClient.sendRequest(token, "ACTION")
                .then()
                .statusCode(200)
                .body("result", equalTo("OK"));
    }

    @Test
    @DisplayName("Ошибка ACTION без предварительного LOGIN")
    @Description("Попытка выполнить действие для неавторизованного токена")
    void testActionWithoutLogin() {
        String token = generateValidToken();
        setupMockEndpoint("/doAction", 200);

        appClient.sendRequest(token, "ACTION")
                .then()
                .body("result", equalTo("ERROR"));
    }

    @Test
    @DisplayName("Успешный LOGOUT пользователя")
    @Description("Проверка удаления токена из хранилища и невозможности ACTION после LOGOUT")
    void testLogout() {
        String token = generateValidToken();

        setupMockEndpoint("/auth", 200);
        appClient.sendRequest(token, "LOGIN");

        appClient.sendRequest(token, "LOGOUT")
                .then()
                .statusCode(200)
                .body("result", equalTo("OK"));

        appClient.sendRequest(token, "ACTION")
                .then()
                .body("result", equalTo("ERROR"));
    }

    @Test
    @DisplayName("Ошибка авторизации при неверном X-Api-Key")
    @Description("Проверка доступа к эндпоинту с невалидным статическим ключом")
    void testInvalidApiKey() {
        String token = generateValidToken();
        AppClient wrongClient = new AppClient("WRONG_KEY");

        wrongClient.sendRequest(token, "LOGIN")
                .then()
                .statusCode(401)
                .body("result", equalTo("ERROR"));
    }

    @Test
    @DisplayName("Ошибка при неверной длине токена")
    @Description("Передача токена длиной меньше 32 символов")
    void testInvalidTokenLength() {
        appClient.sendRequest("SHORT123", "LOGIN")
                .then()
                .body("result", equalTo("ERROR"));
    }

    @Test
    @DisplayName("Ошибка при запрещенных символах в токене")
    @Description("Передача токена с недопустимыми спецсимволами")
    void testInvalidTokenChars() {
        appClient.sendRequest("ABCDEF1234567890ABCDEF123456789@#", "LOGIN")
                .then()
                .body("result", equalTo("ERROR"));
    }

    @Test
    @DisplayName("Ошибка при неизвестном action")
    @Description("Передача экшена, которого нет в списке доступных")
    void testUnknownAction() {
        String token = generateValidToken();
        appClient.sendRequest(token, "DELETE_ALL")
                .then()
                .body("result", equalTo("ERROR"));
    }
}

import io.qameta.allure.*;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.DisplayName;
import java.util.UUID;
import static org.hamcrest.Matchers.equalTo;

@Epic("Тестирование API внутреннего сервиса")
@Feature("Аутентификация и действия пользователей")
@Severity(SeverityLevel.CRITICAL)
@Owner("QA Team")
@Link(name = "Документация API", url = "https://api-docs.example.com")
public class AppTest extends BaseTest {

    private String generateValidToken() {
        String token = UUID.randomUUID().toString().replace("-", "").toUpperCase();
        Allure.parameter("Test Data - Token", token);
        Allure.addAttachment("Test Data - Token", "text/plain", token);
        return token;
    }

    @Test
    @DisplayName("Успешный LOGIN пользователя")
    @Description("Проверка сохранения токена при успешном ответе внешнего сервиса")
    @Severity(SeverityLevel.BLOCKER)
    @Story("Аутентификация")
    @Tag("smoke")
    @Tag("auth")
    void testSuccessfulLogin() {
        // Arrange - Тестовые данные
        String token = generateValidToken();
        String endpoint = "/auth";
        int expectedStatus = 200;
        String expectedResult = "OK";

        Allure.parameter("Token", token);
        Allure.parameter("Endpoint", endpoint);
        Allure.parameter("Expected Status", String.valueOf(expectedStatus));
        Allure.parameter("Expected Result", expectedResult);

        // Step 1: Настройка мока
        Allure.step("Настроить мок эндпоинта " + endpoint + " со статусом " + expectedStatus, () -> {
            setupMockEndpoint(endpoint, expectedStatus);
        });

        // Step 2: Отправка запроса
        Allure.step("Отправить запрос LOGIN с токеном " + token, () -> {
            Allure.addAttachment("Request", "text/plain",
                    "Method: POST\nEndpoint: " + endpoint + "\nToken: " + token + "\nAction: LOGIN");
        });

        // Act & Assert
        appClient.sendRequest(token, "LOGIN")
                .then()
                .log().all()
                .statusCode(expectedStatus)
                .body("result", equalTo(expectedResult));

        // Step 3: Проверка ответа
        Allure.step("Проверить ответ: статус " + expectedStatus + ", результат " + expectedResult, () -> {
            Allure.addAttachment("Expected Response", "text/plain",
                    "Status: " + expectedStatus + "\nBody: {\"result\": \"" + expectedResult + "\"}");
        });
    }

    @Test
    @DisplayName("Ошибка LOGIN при отказе внешнего сервиса")
    @Description("Проверка ответа приложения, если внешний сервис вернул 401")
    @Severity(SeverityLevel.CRITICAL)
    @Story("Аутентификация")
    @Tag("auth")
    @Tag("negative")
    void testFailedLogin() {
        // Arrange - Тестовые данные
        String token = generateValidToken();
        String endpoint = "/auth";
        int mockStatus = 401;
        int expectedStatus = 400;
        String expectedResult = "ERROR";

        Allure.parameter("Token", token);
        Allure.parameter("Mock Status", String.valueOf(mockStatus));
        Allure.parameter("Expected Status", String.valueOf(expectedStatus));
        Allure.parameter("Expected Result", expectedResult);

        // Step 1: Настройка мока с ошибкой
        Allure.step("Настроить мок эндпоинта " + endpoint + " со статусом " + mockStatus, () -> {
            setupMockEndpoint(endpoint, mockStatus);
        });

        // Step 2: Отправка запроса
        Allure.step("Отправить запрос LOGIN с токеном " + token, () -> {
            Allure.addAttachment("Request", "text/plain",
                    "Method: POST\nEndpoint: " + endpoint + "\nToken: " + token + "\nAction: LOGIN");
        });

        // Act & Assert
        appClient.sendRequest(token, "LOGIN")
                .then()
                .log().all()
                .statusCode(expectedStatus)
                .body("result", equalTo(expectedResult));

        // Step 3: Проверка ответа
        Allure.step("Проверить ответ: статус " + expectedStatus + ", результат " + expectedResult, () -> {
            Allure.addAttachment("Expected Response", "text/plain",
                    "Status: " + expectedStatus + "\nBody: {\"result\": \"" + expectedResult + "\"}");
        });
    }

    @Test
    @DisplayName("Успешное выполнение ACTION после LOGIN")
    @Description("Проверка цепочки LOGIN -> ACTION при успешных ответах внешнего сервиса")
    @Severity(SeverityLevel.CRITICAL)
    @Story("Действия пользователя")
    @Tag("smoke")
    @Tag("action")
    void testSuccessfulAction() {
        // Arrange - Тестовые данные
        String token = generateValidToken();
        String authEndpoint = "/auth";
        String actionEndpoint = "/doAction";
        int expectedStatus = 200;
        String expectedResult = "OK";

        Allure.parameter("Token", token);
        Allure.parameter("Auth Endpoint", authEndpoint);
        Allure.parameter("Action Endpoint", actionEndpoint);
        Allure.parameter("Expected Status", String.valueOf(expectedStatus));
        Allure.parameter("Expected Result", expectedResult);

        // Step 1: LOGIN
        Allure.step("Шаг 1: Выполнить LOGIN с токеном " + token, () -> {
            setupMockEndpoint(authEndpoint, 200);
            appClient.sendRequest(token, "LOGIN")
                    .then()
                    .statusCode(200)
                    .body("result", equalTo("OK"));
        });

        // Step 2: ACTION
        Allure.step("Шаг 2: Выполнить ACTION с токеном " + token, () -> {
            setupMockEndpoint(actionEndpoint, 200);
            Allure.addAttachment("Request", "text/plain",
                    "Method: POST\nEndpoint: " + actionEndpoint + "\nToken: " + token + "\nAction: ACTION");
        });

        // Act & Assert
        appClient.sendRequest(token, "ACTION")
                .then()
                .log().all()
                .statusCode(expectedStatus)
                .body("result", equalTo(expectedResult));

        // Step 3: Проверка ответа
        Allure.step("Шаг 3: Проверить ответ ACTION: статус " + expectedStatus + ", результат " + expectedResult, () -> {
            Allure.addAttachment("Expected Response", "text/plain",
                    "Status: " + expectedStatus + "\nBody: {\"result\": \"" + expectedResult + "\"}");
        });
    }

    @Test
    @DisplayName("Ошибка ACTION без предварительного LOGIN")
    @Description("Попытка выполнить действие для неавторизованного токена")
    @Severity(SeverityLevel.NORMAL)
    @Story("Действия пользователя")
    @Tag("action")
    @Tag("negative")
    void testActionWithoutLogin() {
        // Arrange - Тестовые данные
        String token = generateValidToken();
        String endpoint = "/doAction";
        int mockStatus = 200;
        String expectedResult = "ERROR";

        Allure.parameter("Token", token);
        Allure.parameter("Endpoint", endpoint);
        Allure.parameter("Mock Status", String.valueOf(mockStatus));
        Allure.parameter("Expected Result", expectedResult);
        Allure.parameter("Precondition", "Токен не прошел LOGIN");

        // Step 1: Настройка мока
        Allure.step("Настроить мок эндпоинта " + endpoint + " со статусом " + mockStatus, () -> {
            setupMockEndpoint(endpoint, mockStatus);
        });

        // Step 2: Отправка ACTION без LOGIN
        Allure.step("Отправить запрос ACTION без предварительного LOGIN с токеном " + token, () -> {
            Allure.addAttachment("Request", "text/plain",
                    "Method: POST\nEndpoint: " + endpoint + "\nToken: " + token + "\nAction: ACTION\nPrecondition: Нет предварительного LOGIN");
        });

        // Act & Assert
        appClient.sendRequest(token, "ACTION")
                .then()
                .log().all()
                .body("result", equalTo(expectedResult));

        // Step 3: Проверка ответа
        Allure.step("Проверить ответ: результат " + expectedResult, () -> {
            Allure.addAttachment("Expected Response", "text/plain",
                    "Body: {\"result\": \"" + expectedResult + "\"}");
        });
    }

    @Test
    @DisplayName("Успешный LOGOUT пользователя")
    @Description("Проверка удаления токена из хранилища и невозможности ACTION после LOGOUT")
    @Severity(SeverityLevel.NORMAL)
    @Story("Аутентификация")
    @Tag("auth")
    @Tag("action")
    void testLogout() {
        // Arrange - Тестовые данные
        String token = generateValidToken();
        String authEndpoint = "/auth";
        String logoutEndpoint = "/auth/logout";
        String actionEndpoint = "/doAction";
        int expectedStatus = 200;
        String expectedResult = "OK";

        Allure.parameter("Token", token);
        Allure.parameter("Auth Endpoint", authEndpoint);
        Allure.parameter("Logout Endpoint", logoutEndpoint);
        Allure.parameter("Action Endpoint", actionEndpoint);
        Allure.parameter("Expected Status", String.valueOf(expectedStatus));
        Allure.parameter("Expected Result", expectedResult);

        // Step 1: LOGIN
        Allure.step("Шаг 1: Выполнить LOGIN с токеном " + token, () -> {
            setupMockEndpoint(authEndpoint, 200);
            appClient.sendRequest(token, "LOGIN")
                    .then()
                    .statusCode(200)
                    .body("result", equalTo("OK"));
        });

        // Step 2: LOGOUT
        Allure.step("Шаг 2: Выполнить LOGOUT с токеном " + token, () -> {
            appClient.sendRequest(token, "LOGOUT")
                    .then()
                    .statusCode(expectedStatus)
                    .body("result", equalTo(expectedResult));
        });

        // Step 3: ACTION после LOGOUT (должен вернуть ERROR)
        Allure.step("Шаг 3: Попробовать выполнить ACTION после LOGOUT с токеном " + token, () -> {
            appClient.sendRequest(token, "ACTION")
                    .then()
                    .body("result", equalTo("ERROR"));
        });
    }

    @Test
    @DisplayName("Ошибка авторизации при неверном X-Api-Key")
    @Description("Проверка доступа к эндпоинту с невалидным статическим ключом")
    @Severity(SeverityLevel.CRITICAL)
    @Story("Аутентификация")
    @Tag("auth")
    @Tag("negative")
    @Tag("security")
    void testInvalidApiKey() {
        // Arrange - Тестовые данные
        String token = generateValidToken();
        String wrongApiKey = "WRONG_KEY";
        int expectedStatus = 401;
        String expectedResult = "ERROR";

        Allure.parameter("Token", token);
        Allure.parameter("API Key", wrongApiKey);
        Allure.parameter("Expected Status", String.valueOf(expectedStatus));
        Allure.parameter("Expected Result", expectedResult);
        Allure.parameter("Test Type", "Безопасность");

        // Step 1: Создание клиента с неверным ключом
        Allure.step("Создать AppClient с неверным API-ключом: " + wrongApiKey, () -> {
            Allure.addAttachment("Request", "text/plain",
                    "Method: POST\nEndpoint: /auth\nToken: " + token + "\nAction: LOGIN\nAPI Key: " + wrongApiKey);
        });

        AppClient wrongClient = new AppClient(wrongApiKey);

        // Act & Assert
        wrongClient.sendRequest(token, "LOGIN")
                .then()
                .log().all()
                .statusCode(expectedStatus)
                .body("result", equalTo(expectedResult));

        // Step 2: Проверка ответа
        Allure.step("Проверить ответ: статус " + expectedStatus + ", результат " + expectedResult, () -> {
            Allure.addAttachment("Expected Response", "text/plain",
                    "Status: " + expectedStatus + "\nBody: {\"result\": \"" + expectedResult + "\"}");
        });
    }

    @Test
    @DisplayName("Ошибка при неверной длине токена")
    @Description("Передача токена длиной меньше 32 символов")
    @Severity(SeverityLevel.NORMAL)
    @Story("Валидация данных")
    @Tag("validation")
    @Tag("negative")
    void testInvalidTokenLength() {
        // Arrange - Тестовые данные
        String shortToken = "SHORT123";
        int tokenLength = shortToken.length();
        String expectedResult = "ERROR";

        Allure.parameter("Token", shortToken);
        Allure.parameter("Token Length", String.valueOf(tokenLength));
        Allure.parameter("Min Required Length", "32");
        Allure.parameter("Expected Result", expectedResult);
        Allure.parameter("Test Type", "Валидация");

        // Step 1: Отправка короткого токена
        Allure.step("Отправить запрос LOGIN с коротким токеном: " + shortToken
                + " (длина: " + tokenLength + ")", () -> {
            Allure.addAttachment("Request", "text/plain",
                    "Method: POST\nEndpoint: /auth\nToken: " + shortToken
                            + "\nAction: LOGIN\nToken Length: " + tokenLength);
        });

        // Act & Assert
        appClient.sendRequest(shortToken, "LOGIN")
                .then()
                .log().all()
                .body("result", equalTo(expectedResult));

        // Step 2: Проверка ответа
        Allure.step("Проверить ответ: результат " + expectedResult, () -> {
            Allure.addAttachment("Expected Response", "text/plain",
                    "Body: {\"result\": \"" + expectedResult + "\"}");
        });
    }

    @Test
    @DisplayName("Ошибка при запрещенных символах в токене")
    @Description("Передача токена с недопустимыми спецсимволами")
    @Severity(SeverityLevel.NORMAL)
    @Story("Валидация данных")
    @Tag("validation")
    @Tag("negative")
    void testInvalidTokenChars() {
        // Arrange - Тестовые данные
        String invalidToken = "ABCDEF1234567890ABCDEF123456789@#";
        String expectedResult = "ERROR";

        Allure.parameter("Token", invalidToken);
        Allure.parameter("Token Length", String.valueOf(invalidToken.length()));
        Allure.parameter("Invalid Characters", "@#");
        Allure.parameter("Expected Result", expectedResult);
        Allure.parameter("Test Type", "Валидация");

        // Step 1: Отправка токена с запрещенными символами
        Allure.step("Отправить запрос LOGIN с токеном содержащим спецсимволы: "
                + invalidToken, () -> {
            Allure.addAttachment("Request", "text/plain",
                    "Method: POST\nEndpoint: /auth\nToken: " + invalidToken
                            + "\nAction: LOGIN\nInvalid Characters: @#");
        });

        // Act & Assert
        appClient.sendRequest(invalidToken, "LOGIN")
                .then()
                .log().all()
                .body("result", equalTo(expectedResult));

        // Step 2: Проверка ответа
        Allure.step("Проверить ответ: результат " + expectedResult, () -> {
            Allure.addAttachment("Expected Response", "text/plain",
                    "Body: {\"result\": \"" + expectedResult + "\"}");
        });
    }

    @Test
    @DisplayName("Ошибка при неизвестном action")
    @Description("Передача экшена, которого нет в списке доступных")
    @Severity(SeverityLevel.NORMAL)
    @Story("Валидация данных")
    @Tag("validation")
    @Tag("negative")
    void testUnknownAction() {
        // Arrange - Тестовые данные
        String token = generateValidToken();
        String unknownAction = "DELETE_ALL";
        String expectedResult = "ERROR";

        Allure.parameter("Token", token);
        Allure.parameter("Action", unknownAction);
        Allure.parameter("Expected Result", expectedResult);
        Allure.parameter("Test Type", "Валидация");

        // Step 1: Отправка неизвестного action
        Allure.step("Отправить запрос с неизвестным action: " + unknownAction, () -> {
            Allure.addAttachment("Request", "text/plain",
                    "Method: POST\nEndpoint: /doAction\nToken: " + token
                            + "\nAction: " + unknownAction);
        });

        // Act & Assert
        appClient.sendRequest(token, unknownAction)
                .then()
                .log().all()
                .body("result", equalTo(expectedResult));

        // Step 2: Проверка ответа
        Allure.step("Проверить ответ: результат " + expectedResult, () -> {
            Allure.addAttachment("Expected Response", "text/plain",
                    "Body: {\"result\": \"" + expectedResult + "\"}");
        });
    }
}

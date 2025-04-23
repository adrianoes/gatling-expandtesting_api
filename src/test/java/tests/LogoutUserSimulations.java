package tests;

import static io.gatling.javaapi.core.CoreDsl.*;
import static io.gatling.javaapi.http.HttpDsl.*;

import io.gatling.javaapi.core.*;
import io.gatling.javaapi.http.*;

import java.time.Duration;
import java.util.*;

import utils.FakerUtils;

public class LogoutUserSimulations extends Simulation {

    private final int vu = Integer.getInteger("vu", 10);
    private final String testType = System.getProperty("testType", "smoke").toLowerCase();

    private final HttpProtocolBuilder httpProtocol = http.baseUrl("https://practice.expandtesting.com")
            .acceptHeader("application/json")
            .userAgentHeader("Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/134.0.0.0 Safari/537.36");

    private final ScenarioBuilder scenario = scenario("Create, Login, Logout, Re-Login and Delete User Scenario")
            .exec(session -> {
                // Gerar os dados únicos para cada VU diretamente dentro do Session
                Map<String, Object> userData = FakerUtils.generateUserData();

                // Armazenar os dados gerados no contexto da sessão (para uso posterior)
                return session.set("name", userData.get("name"))
                        .set("email", userData.get("email"))
                        .set("password", userData.get("password"));
            })
            .exec(http("Create User Request")
                    .post("/notes/api/users/register")
                    .formParam("name", "#{name}")
                    .formParam("email", "#{email}")
                    .formParam("password", "#{password}")
                    .check(
                            jsonPath("$.success").is("true"),
                            jsonPath("$.status").is("201"),
                            jsonPath("$.message").is("User account created successfully"),
                            jsonPath("$.data.id").exists(),
                            jsonPath("$.data.id").saveAs("userId")  // Salvando o userId gerado na criação
                    )
            )
            .exec(http("Login User Request")
                    .post("/notes/api/users/login")
                    .formParam("email", "#{email}")
                    .formParam("password", "#{password}")
                    .check(
                            jsonPath("$.success").is("true"),
                            jsonPath("$.status").is("200"),
                            jsonPath("$.message").is("Login successful"),
                            jsonPath("$.data.id").isEL("#{userId}"), // Usando #{userId} diretamente da sessão
                            jsonPath("$.data.token").exists(),
                            jsonPath("$.data.token").saveAs("authToken")
                    )
            )
            .exec(http("Logout User Request")
                    .delete("/notes/api/users/logout")
                    .header("x-auth-token", "#{authToken}")
                    .check(
                            jsonPath("$.success").is("true"),
                            jsonPath("$.status").is("200"),
                            jsonPath("$.message").is("User has been successfully logged out")
                    )
            )
            .exec(http("Re-login User Request")
                    .post("/notes/api/users/login")
                    .formParam("email", "#{email}")
                    .formParam("password", "#{password}")
                    .check(
                            jsonPath("$.success").is("true"),
                            jsonPath("$.status").is("200"),
                            jsonPath("$.message").is("Login successful"),
                            jsonPath("$.data.id").isEL("#{userId}"), // Usando #{userId} diretamente da sessão
                            jsonPath("$.data.token").exists(),
                            jsonPath("$.data.token").saveAs("authToken")
                    )
            )
            .exec(http("Delete User Request")
                    .delete("/notes/api/users/delete-account")
                    .header("x-auth-token", "#{authToken}")
                    .check(
                            jsonPath("$.success").is("true"),
                            jsonPath("$.status").is("200"),
                            jsonPath("$.message").is("Account successfully deleted")
                    )
            );

    private final Assertion assertion = global().failedRequests().count().lt(1L);

    {
        if (testType.equals("smoke")) {
            setUp(
                    scenario.injectOpen(
                            rampUsers(vu).during(Duration.ofSeconds(10))
                    )
            ).protocols(httpProtocol).assertions(assertion);

        } else if (testType.equals("load")) {
            setUp(
                    scenario.injectOpen(
                            rampUsers(10).during(Duration.ofSeconds(10)),
                            constantUsersPerSec(10).during(Duration.ofSeconds(10)).randomized(),
                            rampUsers(0).during(Duration.ofSeconds(10))
                    )
            ).protocols(httpProtocol).assertions(assertion);

        } else if (testType.equals("stress")) {
            setUp(
                    scenario.injectOpen(
                            rampUsers(1000).during(Duration.ofSeconds(10)),
                            constantUsersPerSec(1000).during(Duration.ofSeconds(30)),
                            rampUsers(0).during(Duration.ofSeconds(10))
                    )
            ).protocols(httpProtocol).assertions(assertion);

        } else if (testType.equals("spike")) {
            setUp(
                    scenario.injectOpen(
                            rampUsers(10000).during(Duration.ofMinutes(2)),
                            rampUsers(0).during(Duration.ofMinutes(1))
                    )
            ).protocols(httpProtocol).assertions(assertion);

        } else if (testType.equals("breakpoint")) {
            setUp(
                    scenario.injectOpen(
                            constantUsersPerSec(10000).during(Duration.ofHours(2))
                    )
            ).protocols(httpProtocol).assertions(assertion);

        } else if (testType.equals("soak")) {
            setUp(
                    scenario.injectOpen(
                            rampUsers(1000).during(Duration.ofMinutes(5)),
                            constantUsersPerSec(1000).during(Duration.ofHours(24)),
                            rampUsers(0).during(Duration.ofMinutes(5))
                    )
            ).protocols(httpProtocol).assertions(assertion);

        } else {
            throw new IllegalArgumentException("Unsupported test type: " + testType +
                    ". Use 'smoke', 'load', 'stress', 'spike', 'breakpoint' or 'soak'.");
        }
    }
}

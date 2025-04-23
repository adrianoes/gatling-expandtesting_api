package tests;

import static io.gatling.javaapi.core.CoreDsl.*;
import static io.gatling.javaapi.http.HttpDsl.*;

import io.gatling.javaapi.core.*;
import io.gatling.javaapi.http.*;

import java.time.Duration;
import java.util.*;

import utils.FakerUtils;

public class UpdateNoteStatusSimulations extends Simulation {

    private final int vu = Integer.getInteger("vu", 10);
    private final String testType = System.getProperty("testType", "smoke").toLowerCase();

    private final HttpProtocolBuilder httpProtocol = http.baseUrl("https://practice.expandtesting.com")
            .acceptHeader("application/json")
            .userAgentHeader("Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/134.0.0.0 Safari/537.36");

    private final ScenarioBuilder scenario = scenario("Update Note Status by ID Scenario")
            // Step 1: Create User
            .exec(session -> {
                Map<String, Object> userData = FakerUtils.generateUserData();
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
                            jsonPath("$.data.id").saveAs("userId")
                    )
            )

            // Step 2: Login User
            .exec(http("Login User Request")
                    .post("/notes/api/users/login")
                    .formParam("email", "#{email}")
                    .formParam("password", "#{password}")
                    .check(
                            jsonPath("$.success").is("true"),
                            jsonPath("$.status").is("200"),
                            jsonPath("$.message").is("Login successful"),
                            jsonPath("$.data.id").isEL("#{userId}"),
                            jsonPath("$.data.token").exists(),
                            jsonPath("$.data.token").saveAs("authToken")
                    )
            )

            // Step 3: Create Note
            .exec(session -> {
                Map<String, Object> noteData = FakerUtils.generateUserData();
                return session.set("noteTitle", noteData.get("noteTitle"))
                        .set("noteDescription", noteData.get("noteDescription"))
                        .set("noteCategory", noteData.get("noteCategory"));
            })
            .exec(http("Create Note Request")
                    .post("/notes/api/notes")
                    .header("x-auth-token", "#{authToken}")
                    .formParam("title", "#{noteTitle}")
                    .formParam("description", "#{noteDescription}")
                    .formParam("category", "#{noteCategory}")
                    .check(
                            jsonPath("$.success").is("true"),
                            jsonPath("$.status").is("200"),
                            jsonPath("$.message").is("Note successfully created"),
                            jsonPath("$.data.id").exists(),
                            jsonPath("$.data.created_at").exists(),
                            jsonPath("$.data.updated_at").exists(),
                            jsonPath("$.data.id").saveAs("noteId"),
                            jsonPath("$.data.created_at").saveAs("noteCreatedAt"),
                            jsonPath("$.data.updated_at").saveAs("noteUpdatedAt")
                    )
            )

            .exec(http("Update Note Status Request")
                    .patch("/notes/api/notes/#{noteId}")
                    .header("accept", "application/json")
                    .header("x-auth-token", "#{authToken}")
                    .header("Content-Type", "application/x-www-form-urlencoded")
                    .formParam("completed", "true") // Atualiza o status para "completo"
                    .check(
                            jsonPath("$.success").is("true"),
                            jsonPath("$.status").is("200"),
                            jsonPath("$.message").is("Note successfully Updated"),
                            jsonPath("$.data.id").is(session -> session.getString("noteId")), // Usando o valor da sessão
                            jsonPath("$.data.completed").is("true"),  // Valida o status "completed" atualizado
                            jsonPath("$.data.created_at").is(session -> session.getString("noteCreatedAt")),
                            jsonPath("$.data.title").is(session -> session.getString("noteTitle")),
                            jsonPath("$.data.description").is(session -> session.getString("noteDescription")),
                            jsonPath("$.data.category").is(session -> session.getString("noteCategory")),
                            jsonPath("$.data.updated_at").exists()
                    )
            )

            // Step 5: Delete User
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

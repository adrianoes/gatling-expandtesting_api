package tests;

import static io.gatling.javaapi.core.CoreDsl.*;
import static io.gatling.javaapi.http.HttpDsl.*;

import io.gatling.javaapi.core.*;
import io.gatling.javaapi.http.*;

import utils.FakerUtils;

import java.time.Duration;
import java.util.Map;

public class GetAllNotesSimulations extends Simulation {

    private final int vu = Integer.getInteger("vu", 10);
    private final String testType = System.getProperty("testType", "smoke").toLowerCase();

    Map<String, Object> fakerData = FakerUtils.generateUserData();

    private final HttpProtocolBuilder httpProtocol = http.baseUrl("https://practice.expandtesting.com")
            .acceptHeader("application/json")
            .userAgentHeader("Gatling Test");

    private final ScenarioBuilder scenario = scenario("Create Two Notes and Retrieve All Notes Scenario")
            .exec(session -> {
                // Gerar dados únicos para cada VU dentro da sessão
                Map<String, Object> userData = FakerUtils.generateUserData();

                // Armazenar os dados no contexto da sessão, isolando cada VU
                return session.set("name", userData.get("name"))
                        .set("email", userData.get("email"))
                        .set("password", userData.get("password"))
                        .set("userId", userData.get("userId"));
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
                            jsonPath("$.data.id").saveAs("userId") // Armazenando o userId para o VU atual
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
                            jsonPath("$.data.id").isEL("#{userId}"),  // Acessando o userId específico do VU
                            jsonPath("$.data.token").exists(),
                            jsonPath("$.data.token").saveAs("authToken")
                    )
            )

            // Create First Note
            .exec(http("Create First Note")
                    .post("/notes/api/notes")
                    .header("Content-Type", "application/x-www-form-urlencoded")
                    .header("x-auth-token", "#{authToken}")
                    .formParam("title", fakerData.get("noteTitle"))
                    .formParam("description", fakerData.get("noteDescription"))
                    .formParam("category", fakerData.get("noteCategory"))
                    .check(jsonPath("$.data.id").saveAs("noteId1"))
                    .check(jsonPath("$.data.title").saveAs("note_title_1"))
                    .check(jsonPath("$.data.description").saveAs("note_description_1"))
                    .check(jsonPath("$.data.category").saveAs("note_category_1"))
                    .check(jsonPath("$.data.updated_at").saveAs("note_updated_at_1"))
            )

            // Create Second Note
            .exec(http("Create Second Note")
                    .post("/notes/api/notes")
                    .header("Content-Type", "application/x-www-form-urlencoded")
                    .header("x-auth-token", "#{authToken}")
                    .formParam("title", fakerData.get("noteTitle2"))
                    .formParam("description", fakerData.get("noteDescription2"))
                    .formParam("category", fakerData.get("noteCategory2"))
                    .check(jsonPath("$.data.id").saveAs("noteId2"))
                    .check(jsonPath("$.data.title").saveAs("note_title_2"))
                    .check(jsonPath("$.data.description").saveAs("note_description_2"))
                    .check(jsonPath("$.data.category").saveAs("note_category_2"))
                    .check(jsonPath("$.data.updated_at").saveAs("note_updated_at_2"))
            )

            // Get All Notes
            .exec(http("Get All Notes")
                    .get("/notes/api/notes")
                    .header("x-auth-token", "#{authToken}")
                    .check(jsonPath("$.success").is("true"))
                    .check(jsonPath("$.status").is("200"))
                    .check(jsonPath("$.message").is("Notes successfully retrieved"))

                    // Check second note (index 0)
                    .check(jsonPath("$.data[0].title").is(session -> session.getString("note_title_2")))
                    .check(jsonPath("$.data[0].description").is(session -> session.getString("note_description_2")))
                    .check(jsonPath("$.data[0].category").is(session -> session.getString("note_category_2")))
                    .check(jsonPath("$.data[0].updated_at").is(session -> session.getString("note_updated_at_2")))

                    // Check first note (index 1)
                    .check(jsonPath("$.data[1].title").is(session -> session.getString("note_title_1")))
                    .check(jsonPath("$.data[1].description").is(session -> session.getString("note_description_1")))
                    .check(jsonPath("$.data[1].category").is(session -> session.getString("note_category_1")))
                    .check(jsonPath("$.data[1].updated_at").is(session -> session.getString("note_updated_at_1")))
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

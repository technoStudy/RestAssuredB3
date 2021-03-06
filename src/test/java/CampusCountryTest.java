import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import io.restassured.http.Cookies;
import io.restassured.response.ValidatableResponse;
import org.junit.Assert;
import org.testng.annotations.*;

import java.util.*;

import static io.restassured.RestAssured.*;
import static org.hamcrest.Matchers.*;

public class CampusCountryTest {

    private Cookies cookies;
    private ThreadLocal<List<String>> idsForCleanedUp = new ThreadLocal<>();
    private ThreadLocal<Map<String, String>> body = new ThreadLocal<>();

    @BeforeClass
    public void setUp() {
        RestAssured.baseURI = "https://test.campus.techno.study";


        Map<String, String> credentials = new HashMap<>();
        credentials.put("username", "daulet2030@gmail.com");
        credentials.put("password", "TechnoStudy123@");
        ValidatableResponse response = given()
                .body(credentials)
                .contentType(ContentType.JSON)
                .when()
                .post("/auth/login")
                .then();

        response.statusCode(200);

        cookies = response.extract().detailedCookies();
    }

    @BeforeMethod
    public void createCountry() {
        idsForCleanedUp.set(new ArrayList<>()); // must be emptied
        body.set(new HashMap<>());
        body.get().put("name", "New country " + new Random().nextInt(10000));

        ValidatableResponse response = given()
                .cookies(cookies)
                .body(body.get())
                .contentType(ContentType.JSON)
                .when()
                .post("/school-service/api/countries")
                .then();

        String id = response.statusCode(201).extract().jsonPath().getString("id");
        idsForCleanedUp.get().add(id);
    }

    @Test()
    public void getCountry() {
        given()
                .cookies(cookies)
                .when()
                .get("/school-service/api/countries/" + idsForCleanedUp.get().get(0))
                .then()
                .statusCode(200)
                .body("id", equalTo(idsForCleanedUp.get().get(0)))
                .body("name", equalTo(body.get().get("name")))
                .body("shortName", equalTo(body.get().get("shortName")))
        ;
    }

    @Test()
    public void duplicateCountry() {
        given()
                .cookies(cookies)
                .body(body.get())
                .contentType(ContentType.JSON)
                .when()
                .post("/school-service/api/countries")
                .then()
                .log().body()
                .statusCode(400)
                .body("message", allOf(
                        not(empty()),
                        containsString(body.get().get("name")),
                        containsString("already exists"))
                );


    }

    @Test()
    public void editTest() {
        HashMap<String, String> editedBody = new HashMap<>();
        editedBody.put("id", idsForCleanedUp.get().get(0));
        editedBody.put("name", "Edited country " + new Random().nextInt(500));

        given()
                .cookies(cookies)
                .body(editedBody)
                .contentType(ContentType.JSON)
                .when()
                .put("/school-service/api/countries")
                .then()
                .statusCode(200)
                .body("name", equalTo(editedBody.get("name")))
        ;
    }

    @Test
    public void doubleEditTest() {
        HashMap<String, String> editedBody = new HashMap<>();
        editedBody.put("id", idsForCleanedUp.get().get(0));
        editedBody.put("name", "Edited country " + new Random().nextInt(500));

        given()
                .cookies(cookies)
                .body(editedBody)
                .contentType(ContentType.JSON)
                .when()
                .put("/school-service/api/countries")
                .then()
                .statusCode(200)
                .body("name", equalTo(editedBody.get("name")))
        ;

        editedBody.put("name", "Double Edited country " + new Random().nextInt(500));
        given()
                .cookies(cookies)
                .body(editedBody)
                .contentType(ContentType.JSON)
                .when()
                .put("/school-service/api/countries")
                .then()
                .statusCode(200)
                .body("name", equalTo(editedBody.get("name")))
        ;
    }

    // a country is created
    @Test
    public void createAfterDeleteTest() {
        given()
                .cookies(cookies)
                .when()
                .delete("/school-service/api/countries/" + idsForCleanedUp.get().get(0))
                .then()
                .statusCode(200)
        ;
        idsForCleanedUp.get().remove(0);

        String newId = given()
                .cookies(cookies)
                .body(body.get())
                .contentType(ContentType.JSON)
                .when()
                .post("/school-service/api/countries")
                .then()
                .statusCode(201)
                .extract().jsonPath().getString("id");
        idsForCleanedUp.get().add(newId);
    }
    // a country is deleted

    @Test
    public void deleteAfterDeleting() {
        given()
                .cookies(cookies)
                .when()
                .delete("/school-service/api/countries/" + idsForCleanedUp.get().get(0))
                .then()
                .statusCode(200)
        ;

        given()
                .cookies(cookies)
                .when()
                .delete("/school-service/api/countries/" + idsForCleanedUp.get().get(0))
                .then()
                .statusCode(404)
        ;

        idsForCleanedUp.get().remove(0);
    }

    @Test
    public void editDuplicateTest() {
        HashMap<String, String> newBody = new HashMap<>();
        newBody.put("name", "Very New country " + new Random().nextInt(500));

        String newId = given()
                .cookies(cookies)
                .body(newBody)
                .contentType(ContentType.JSON)
                .when()
                .post("/school-service/api/countries")
                .then()
                .statusCode(201)
                .extract().jsonPath().getString("id");
        idsForCleanedUp.get().add(newId);

        HashMap<String, String> newEditBody = new HashMap<>();
        newEditBody.put("id", newId);  // editing newly created country
        newEditBody.put("name", body.get().get("name")); // it's a name that already exists
        given()
                .cookies(cookies)
                .body(newEditBody)
                .contentType(ContentType.JSON)
                .when()
                .put("/school-service/api/countries")
                .then()
                .statusCode(400)
                .body("message", allOf(
                        not(empty()),
                        containsString(body.get().get("name")),
                        containsString("already exists"))
                );
    }

    @Test
    public void searchTest() {
        Map<String, String> searchBody = new HashMap<>();
        searchBody.put("name", body.get().get("name"));

        given()
                .body(searchBody)
                .cookies(cookies)
                .contentType(ContentType.JSON)
                .when()
                .post("/school-service/api/countries/search")
                .then()
                .log().body()
                .statusCode(200)
                .body("name", contains(body.get().get("name")))
                .body("id", contains(idsForCleanedUp.get().get(0)))
                .body("[0].name", equalTo(body.get().get("name")))
                .body("[0].id", equalTo(idsForCleanedUp.get().get(0)))
        ;
    }

    @Test
    public void searchAfterDeletedTest() {
        given()
                .cookies(cookies)
                .when()
                .delete("/school-service/api/countries/" + idsForCleanedUp.get().get(0))
                .then()
                .statusCode(200)
        ;
        idsForCleanedUp.get().remove(0);

        Map<String, String> searchBody = new HashMap<>();
        searchBody.put("name", body.get().get("name"));

        given()
                .body(searchBody)
                .log().body()
                .cookies(cookies)
                .contentType(ContentType.JSON)
                .when()
                .post("/school-service/api/countries/search")
                .then()
                .statusCode(200)
                .body("$", empty())
        ;
    }

    @Test
    public void partialSearchAfterDeletedTest() {
        given()
                .cookies(cookies)
                .when()
                .delete("/school-service/api/countries/" + idsForCleanedUp.get().get(0))
                .then()
                .statusCode(200)
        ;
        idsForCleanedUp.get().remove(0);

        Map<String, String> searchBody = new HashMap<>();
        searchBody.put("name", body.get().get("name").substring(0, body.get().get("name").length()/2));

        List<Map<String, String>> list = given()
                .body(searchBody)
                .cookies(cookies)
                .contentType(ContentType.JSON)
                .when()
                .post("/school-service/api/countries/search")
                .then()
                .statusCode(200)
                .extract().as(List.class);

        boolean found = false;
        for (Map<String, String> o: list) {
            if(o.get("name").equals(body.get().get("name")) || o.get("id").equals(body.get().get("id"))) {
                found = true;
            }
        }

        Assert.assertFalse("I should not be able to find deleted country", found);
    }

    @AfterMethod
    public void cleanup() {
        for (String id : idsForCleanedUp.get()) {
            given()
                    .cookies(cookies)
                    .when()
                    .delete("/school-service/api/countries/" + id)
                    .then()
                    .statusCode(200)
            ;
        }
    }
}

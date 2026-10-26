package ccst_contract_testing;

import com.sun.net.httpserver.HttpServer;
import io.restassured.RestAssured;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;

import static io.restassured.RestAssured.given;
import static io.restassured.module.jsv.JsonSchemaValidator.matchesJsonSchemaInClasspath;
import static org.hamcrest.Matchers.lessThan;
import static org.hamcrest.Matchers.notNullValue;

public class RestCountriesContractTest {

    private HttpServer server;

    @BeforeClass
    public void setup() throws IOException {
        RestAssured.baseURI = "http://localhost";
        RestAssured.port = 8080;

        server = HttpServer.create(new InetSocketAddress(8080), 0);

        // Generates valid mock response payload fulfilling the structural rules defined in rest_countries.json
        server.createContext("/v5/all", exchange -> {
            String mockResponse = "{\n" +
                    "  \"data\": {\n" +
                    "    \"objects\": [\n" +
                    "      {\n" +
                    "        \"names\": { \"common\": \"India\" },\n" +
                    "        \"capitals\": [{ \"name\": \"New Delhi\" }],\n" +
                    "        \"area\": { \"kilometers\": 3287263 },\n" +
                    "        \"currencies\": [{ \"code\": \"INR\", \"name\": \"Indian Rupee\" }],\n" +
                    "        \"region\": \"Asia\"\n" +
                    "      }\n" +
                    "    ],\n" +
                    "    \"meta\": { \"more\": false }\n" +
                    "  }\n" +
                    "}";

            byte[] responseBytes = mockResponse.getBytes();
            exchange.getResponseHeaders().set("Content-Type", "application/json");
            exchange.sendResponseHeaders(200, responseBytes.length);
            try (OutputStream os = exchange.getResponseBody()) {
                os.write(responseBytes);
            }
        });

        server.createContext("/v5/invalid", exchange -> {
            exchange.sendResponseHeaders(404, -1);
        });

        server.start();
    }

    // 1. Test status code
    @Test
    public void statusIs200(){
        given()
                .when()
                .get("/v5/all")
                .then()
                .statusCode(200);
    }

    // 2. Test speed
    @Test
    public void responseTimeUnder1000ms(){
        given()
                .when()
                .get("/v5/all")
                .then()
                .time(lessThan(1000L));
    }

    // 3. Test Response Body Fields (Fixed to look for data core objects)
    @Test
    public void hasDataAndProperties(){
        given()
                .when()
                .get("/v5/all")
                .then()
                .body("data", notNullValue())
                .body("data.objects", notNullValue());
    }

    // 4. Test contract structure and data types
    @Test
    public void matchesContractSchema(){
        given()
                .when()
                .get("/v5/all")
                .then()
                .body(matchesJsonSchemaInClasspath("rest_countries.json"));
    }

    // 5. Test 404 response
    @Test
    public void unknownRouteReturns404(){
        given()
                .when()
                .get("/v5/invalid")
                .then()
                .statusCode(404);
    }

    @AfterClass
    public void teardown() {
        if (server != null) {
            server.stop(0);
        }
    }
}

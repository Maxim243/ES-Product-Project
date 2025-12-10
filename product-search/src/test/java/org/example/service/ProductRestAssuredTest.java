package org.example.service;

import io.restassured.response.Response;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;

import java.util.List;
import java.util.Map;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.assertEquals;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
public class ProductRestAssuredTest {

    @LocalServerPort
    private int port;

    private final String URL = "/v1/products";

    @Test
    void testEmptyRequestReturnsEmptyResponse() {
        given()
                .port(port)
                .contentType("application/json")
                .body("{}")
                .when()
                .post(URL)
                .then()
                .statusCode(200)
                .body("totalHits", equalTo(0))
                .body("productDTOList", hasSize(0))
                .body("facetDTO.facetBucketDTO", anEmptyMap());
    }

    @Test
    void testNonExistingTextQuery() {
        given()
                .port(port)
                .contentType("application/json")
                .body("{\"query\":\"nonexistingbrand nonexistingproduct\"}")
                .when()
                .post(URL)
                .then()
                .statusCode(200)
                .body("totalHits", equalTo(0));
    }

    @Test
    void testSearchProductsByTextAndSku() {
        String requestBody = """
                {
                    "queryText": "Calvin klein L blue ankle skinny jeans",
                    "size": "3"
                }
                """;

        given()
                .port(port)
                .contentType("application/json")
                .body(requestBody)
                .when()
                .post(URL)
                .then()
                .statusCode(200)
                .body("totalHits", equalTo(8))
                .body("productDTOList.size()", greaterThan(0))
                .body("productDTOList.brand", hasItems("Calvin Klein"))
                .body("productDTOList.name", hasItems(
                        "Women ankle skinny jeans, model 1282",
                        "Women ankle jeans, model 1272",
                        "Classic women jeans, model 1145"
                ))
                .body("productDTOList[0].skus", hasItem(allOf(
                        hasEntry("color", "Blue"),
                        hasEntry("size", "L")
                )));
    }


    @Test
    void testSearchProductsWithFacets() {
        Response response = given()
                .port(port)
                .contentType("application/json")
                .body("""
                        {
                            "queryText": "jeans",
                            "size": "3"
                        }
                        """)
                .when()
                .post(URL)
                .then()
                .statusCode(200)
                .extract()
                .response();

        List<Map<String, Object>> priceRanges = response.jsonPath().getList("facetDTO.facetBucketDTO.price_ranges");
        assertEquals(2, priceRanges.stream().filter(p -> "Cheap".equals(p.get("value"))).findFirst().get().get("count"));
        assertEquals(6, priceRanges.stream().filter(p -> "Average".equals(p.get("value"))).findFirst().get().get("count"));
        assertEquals(0, priceRanges.stream().filter(p -> "Expensive".equals(p.get("value"))).findFirst().get().get("count"));

        List<Map<String, Object>> brands = response.jsonPath().getList("facetDTO.facetBucketDTO.brand");
        assertEquals(4, brands.stream().filter(b -> "Calvin Klein".equals(b.get("value"))).findFirst().get().get("count"));
        assertEquals(4, brands.stream().filter(b -> "Levi's".equals(b.get("value"))).findFirst().get().get("count"));
    }


}

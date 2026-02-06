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
                    "queryText": "Levi's tommy jacket Blue L",
                    "size": "2"
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
                .body("totalHits", equalTo(2))
                .body("productDTOList.size()", greaterThan(0))
                .body("productDTOList.brand", hasItems("Levi's"))
                .body("productDTOList.name", hasItems(
                        "denim trucker jacket",
                        "sherpa lined jacket"
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
                            "queryText": "jacket"
                        }
                        """)
                .when()
                .post(URL)
                .then()
                .statusCode(200)
                .extract()
                .response();

        List<Map<String, Object>> priceRanges = response.jsonPath().getList("facetDTO.facetBucketDTO.price_ranges");
        assertEquals(4, priceRanges.stream().filter(p -> "Cheap".equals(p.get("value"))).findFirst().get().get("count"));
        assertEquals(5, priceRanges.stream().filter(p -> "Average".equals(p.get("value"))).findFirst().get().get("count"));
        assertEquals(0, priceRanges.stream().filter(p -> "Expensive".equals(p.get("value"))).findFirst().get().get("count"));

        List<Map<String, Object>> brands = response.jsonPath().getList("facetDTO.facetBucketDTO.brand");
        assertEquals(2, brands.stream().filter(b -> "Adidas".equals(b.get("value"))).findFirst().get().get("count"));
        assertEquals(2, brands.stream().filter(b -> "Levi's".equals(b.get("value"))).findFirst().get().get("count"));
        assertEquals(2, brands.stream().filter(b -> "Nike".equals(b.get("value"))).findFirst().get().get("count"));
    }


}

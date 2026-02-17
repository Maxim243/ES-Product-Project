package org.example.service;

import io.restassured.response.Response;
import org.example.enums.SearchMessage;
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
                .body("message", equalTo(SearchMessage.NO_RESULTS.getMessage()))
                .body("totalHits", equalTo(0))
                .body("productDTOList", hasSize(0))
                .body("facetDTO.facetBucketDTO", anEmptyMap())
                .body("facetDTO.facetBucketDTO", anEmptyMap());
    }

    @Test
    void testNonExistingTextQuery() {

        String requestBody = """
                {
                    "queryText": "nonexistingbrand nonexistingproduct"
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
                .body("message", equalTo(SearchMessage.NO_RESULTS.getMessage()))
                .body("totalHits", equalTo(0));
    }

    @Test
    void testStrictSearchByTextAndSku() {
        String requestBody = """
                {
                    "queryText": "puma shorts black L"
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
                .body("message", equalTo(SearchMessage.SEARCH_SUCCESS.getMessage()))
                .body("totalHits", equalTo(1))
                .body("productDTOList.size()", greaterThan(0))
                .body("productDTOList.brand", hasItems("Puma"))
                .body("productDTOList.name", hasItems(
                        "nylon hiking shorts"
                ))
                .body("productDTOList[0].skus", hasItem(allOf(
                        hasEntry("color", "Black"),
                        hasEntry("size", "L")
                )));
    }

    @Test
    void testNoFiltersSearchByTextAndSku() {
        String requestBody = """
                {
                    "queryText": "puma shorts blue L"
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
                .body("message", equalTo(SearchMessage.CATEGORY_ONLY_STRICT_SUCCESS.getMessage()))
                .body("totalHits", equalTo(4))
                .body("productDTOList.size()", greaterThan(0))
                .body("productDTOList.name", hasItems(
                        "nylon hiking shorts",
                        "dri-fit running shorts",
                        "cotton chino shorts",
                        "cotton sleep shorts"
                ));
    }

    @Test
    void testSearchOpenAIByText() {
        String requestBody = """
                {
                    "queryText": "Warm outerwear with soft interior for cold weather"
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
                .body("message", equalTo(SearchMessage.SEARCH_SUCCESS.getMessage()))
                .body("productDTOList.size()", greaterThan(0))
                .body("productDTOList.name", hasItems(
                        "windrunner hooded jacket",
                        "insulated winter jacket",
                        "wool oversized coat"
                ));
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

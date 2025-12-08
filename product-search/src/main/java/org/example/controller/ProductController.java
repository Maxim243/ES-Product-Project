package org.example.controller;

import lombok.RequiredArgsConstructor;
import org.example.dto.ProductRequestDTO;
import org.example.dto.ProductResponseDTO;
import org.example.service.ProductService;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;

@RestController
@RequestMapping(value = "v1/products")
@RequiredArgsConstructor
public class ProductController {

    private final ProductService productService;

    @PostMapping
    public ProductResponseDTO getSearchProductsResponse(@RequestBody ProductRequestDTO productRequestDTO) throws IOException {
        return productService.getSearchProductResponse(productRequestDTO);
    }
}


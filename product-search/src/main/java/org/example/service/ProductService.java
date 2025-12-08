package org.example.service;

import org.example.dto.ProductRequestDTO;
import org.example.dto.ProductResponseDTO;

import java.io.IOException;

public interface ProductService {
  ProductResponseDTO getSearchProductResponse(ProductRequestDTO productRequestDTO) throws IOException;
}

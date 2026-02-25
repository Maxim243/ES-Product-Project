package org.example.controller;

import lombok.RequiredArgsConstructor;
import org.example.dto.AutoPartRequestDTO;
import org.example.dto.AutoPartResponseDTO;
import org.example.service.impl.AutoPartServiceImpl;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class AutoPartController {

    private final AutoPartServiceImpl autoPartServiceImpl;

    @PostMapping("v1/searchPart")
    public AutoPartResponseDTO checkPartFit(@RequestBody AutoPartRequestDTO autoPartRequestDTO) {
        return autoPartServiceImpl.getAutoParts(autoPartRequestDTO);
    }
}

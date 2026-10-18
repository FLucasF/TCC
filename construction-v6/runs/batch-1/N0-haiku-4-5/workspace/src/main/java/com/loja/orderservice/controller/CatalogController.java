package com.loja.orderservice.controller;

import com.loja.orderservice.dto.ErrorResponse;
import com.loja.orderservice.dto.ProductResponse;
import com.loja.orderservice.exception.OrderServiceException;
import com.loja.orderservice.service.CatalogService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/products")
@RequiredArgsConstructor
public class CatalogController {
    private final CatalogService catalogService;

    @GetMapping("/{sku}")
    public ResponseEntity<?> getProduct(@PathVariable String sku) {
        try {
            ProductResponse response = catalogService.getProduct(sku);
            return ResponseEntity.ok(response);
        } catch (OrderServiceException e) {
            return ResponseEntity.status(e.getHttpStatus())
                    .body(ErrorResponse.builder().error(e.getErrorCode()).build());
        }
    }
}

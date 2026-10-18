package br.com.loja.catalog;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

import br.com.loja.common.ApiException;
import br.com.loja.common.ErrorCode;

@RestController
class ProductController {

    private final ProductRepository products;

    ProductController(ProductRepository products) {
        this.products = products;
    }

    @GetMapping("/products/{sku}")
    Product get(@PathVariable String sku) {
        return products.findBySku(sku).orElseThrow(() -> new ApiException(ErrorCode.PRODUCT_NOT_FOUND));
    }
}

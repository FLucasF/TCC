package br.com.loja.pedidos.web;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

import br.com.loja.pedidos.catalog.Product;
import br.com.loja.pedidos.catalog.ProductRepository;
import br.com.loja.pedidos.common.DomainException;

@RestController
class CatalogController {

    private final ProductRepository products;

    CatalogController(ProductRepository products) {
        this.products = products;
    }

    @GetMapping("/products/{sku}")
    Product get(@PathVariable String sku) {
        return products.find(sku).orElseThrow(() -> DomainException.notFound("PRODUCT_NOT_FOUND"));
    }
}

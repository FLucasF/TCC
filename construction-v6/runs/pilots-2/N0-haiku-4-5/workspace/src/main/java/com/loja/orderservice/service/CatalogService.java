package com.loja.orderservice.service;

import com.loja.orderservice.entity.Product;
import com.loja.orderservice.repository.ProductRepository;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.List;

@Service
public class CatalogService {
    private final ProductRepository productRepository;

    public CatalogService(ProductRepository productRepository) {
        this.productRepository = productRepository;
    }

    public void initializeCatalog() {
        if (productRepository.count() == 0) {
            List<Product> products = Arrays.asList(
                new Product("CAM-001", "Camiseta básica", new BigDecimal("79.90"), new BigDecimal("0.30"), 50, false),
                new Product("TEN-001", "Tênis corrida", new BigDecimal("249.90"), new BigDecimal("1.20"), 10, false),
                new Product("FON-001", "Fone sem fio", new BigDecimal("199.90"), new BigDecimal("0.25"), 5, false),
                new Product("MEI-001", "Meia esportiva", new BigDecimal("19.90"), new BigDecimal("0.10"), 100, false),
                new Product("JAQ-001", "Jaqueta corta-vento", new BigDecimal("389.90"), new BigDecimal("0.80"), 3, false),
                new Product("MOC-001", "Mochila urbana", new BigDecimal("159.90"), new BigDecimal("0.90"), 1, false),
                new Product("BON-001", "Boné aba reta", new BigDecimal("59.90"), new BigDecimal("0.15"), 0, false),
                new Product("REL-001", "Relógio digital", new BigDecimal("129.90"), new BigDecimal("0.20"), 8, true)
            );

            productRepository.saveAll(products);
        }
    }
}

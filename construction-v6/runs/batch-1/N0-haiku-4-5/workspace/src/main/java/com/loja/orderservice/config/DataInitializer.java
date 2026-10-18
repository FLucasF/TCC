package com.loja.orderservice.config;

import com.loja.orderservice.domain.entity.Product;
import com.loja.orderservice.repository.ProductRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;
import java.math.BigDecimal;
import java.util.List;

@Component
@RequiredArgsConstructor
public class DataInitializer implements CommandLineRunner {
    private final ProductRepository productRepository;

    @Override
    public void run(String... args) throws Exception {
        if (productRepository.count() > 0) {
            return;
        }

        List<Product> products = List.of(
                Product.builder()
                        .sku("CAM-001")
                        .name("Camiseta básica")
                        .price(new BigDecimal("79.90"))
                        .weightKg(new BigDecimal("0.30"))
                        .stock(50L)
                        .discontinued(false)
                        .build(),
                Product.builder()
                        .sku("TEN-001")
                        .name("Tênis corrida")
                        .price(new BigDecimal("249.90"))
                        .weightKg(new BigDecimal("1.20"))
                        .stock(10L)
                        .discontinued(false)
                        .build(),
                Product.builder()
                        .sku("FON-001")
                        .name("Fone sem fio")
                        .price(new BigDecimal("199.90"))
                        .weightKg(new BigDecimal("0.25"))
                        .stock(5L)
                        .discontinued(false)
                        .build(),
                Product.builder()
                        .sku("MEI-001")
                        .name("Meia esportiva")
                        .price(new BigDecimal("19.90"))
                        .weightKg(new BigDecimal("0.10"))
                        .stock(100L)
                        .discontinued(false)
                        .build(),
                Product.builder()
                        .sku("JAQ-001")
                        .name("Jaqueta corta-vento")
                        .price(new BigDecimal("389.90"))
                        .weightKg(new BigDecimal("0.80"))
                        .stock(3L)
                        .discontinued(false)
                        .build(),
                Product.builder()
                        .sku("MOC-001")
                        .name("Mochila urbana")
                        .price(new BigDecimal("159.90"))
                        .weightKg(new BigDecimal("0.90"))
                        .stock(1L)
                        .discontinued(false)
                        .build(),
                Product.builder()
                        .sku("BON-001")
                        .name("Boné aba reta")
                        .price(new BigDecimal("59.90"))
                        .weightKg(new BigDecimal("0.15"))
                        .stock(0L)
                        .discontinued(false)
                        .build(),
                Product.builder()
                        .sku("REL-001")
                        .name("Relógio digital")
                        .price(new BigDecimal("129.90"))
                        .weightKg(new BigDecimal("0.20"))
                        .stock(8L)
                        .discontinued(true)
                        .build()
        );

        productRepository.saveAll(products);
    }
}

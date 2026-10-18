package com.example.infrastructure;

import com.example.domain.entity.Product;
import com.example.domain.repository.ProductRepository;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import java.math.BigDecimal;

@Component
public class DataInitializer {

    private final ProductRepository productRepository;

    public DataInitializer(ProductRepository productRepository) {
        this.productRepository = productRepository;
    }

    @EventListener(ApplicationReadyEvent.class)
    public void initializeData() {
        if (productRepository.count() > 0) {
            return;
        }

        productRepository.save(new Product("CAM-001", "Camiseta básica", new BigDecimal("79.90"), new BigDecimal("0.30"), 50, false));
        productRepository.save(new Product("TEN-001", "Tênis corrida", new BigDecimal("249.90"), new BigDecimal("1.20"), 10, false));
        productRepository.save(new Product("FON-001", "Fone sem fio", new BigDecimal("199.90"), new BigDecimal("0.25"), 5, false));
        productRepository.save(new Product("MEI-001", "Meia esportiva", new BigDecimal("19.90"), new BigDecimal("0.10"), 100, false));
        productRepository.save(new Product("JAQ-001", "Jaqueta corta-vento", new BigDecimal("389.90"), new BigDecimal("0.80"), 3, false));
        productRepository.save(new Product("MOC-001", "Mochila urbana", new BigDecimal("159.90"), new BigDecimal("0.90"), 1, false));
        productRepository.save(new Product("BON-001", "Boné aba reta", new BigDecimal("59.90"), new BigDecimal("0.15"), 0, false));
        productRepository.save(new Product("REL-001", "Relógio digital", new BigDecimal("129.90"), new BigDecimal("0.20"), 8, true));
    }
}

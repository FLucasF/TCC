package com.loja.pedidos.config;

import com.loja.pedidos.entity.Produto;
import com.loja.pedidos.repository.ProdutoRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.Arrays;

@Component
@RequiredArgsConstructor
public class DataInitializer {
    private final ProdutoRepository produtoRepository;

    @EventListener(ApplicationReadyEvent.class)
    public void initializeData() {
        if (produtoRepository.count() == 0) {
            var produtos = Arrays.asList(
                Produto.builder()
                    .sku("CAM-001")
                    .nome("Camiseta básica")
                    .preco(new BigDecimal("79.90"))
                    .pesoKg(new BigDecimal("0.30"))
                    .estoque(50)
                    .foraDeLinha(false)
                    .build(),
                Produto.builder()
                    .sku("TEN-001")
                    .nome("Tênis corrida")
                    .preco(new BigDecimal("249.90"))
                    .pesoKg(new BigDecimal("1.20"))
                    .estoque(10)
                    .foraDeLinha(false)
                    .build(),
                Produto.builder()
                    .sku("FON-001")
                    .nome("Fone sem fio")
                    .preco(new BigDecimal("199.90"))
                    .pesoKg(new BigDecimal("0.25"))
                    .estoque(5)
                    .foraDeLinha(false)
                    .build(),
                Produto.builder()
                    .sku("MEI-001")
                    .nome("Meia esportiva")
                    .preco(new BigDecimal("19.90"))
                    .pesoKg(new BigDecimal("0.10"))
                    .estoque(100)
                    .foraDeLinha(false)
                    .build(),
                Produto.builder()
                    .sku("JAQ-001")
                    .nome("Jaqueta corta-vento")
                    .preco(new BigDecimal("389.90"))
                    .pesoKg(new BigDecimal("0.80"))
                    .estoque(3)
                    .foraDeLinha(false)
                    .build(),
                Produto.builder()
                    .sku("MOC-001")
                    .nome("Mochila urbana")
                    .preco(new BigDecimal("159.90"))
                    .pesoKg(new BigDecimal("0.90"))
                    .estoque(1)
                    .foraDeLinha(false)
                    .build(),
                Produto.builder()
                    .sku("BON-001")
                    .nome("Boné aba reta")
                    .preco(new BigDecimal("59.90"))
                    .pesoKg(new BigDecimal("0.15"))
                    .estoque(0)
                    .foraDeLinha(false)
                    .build(),
                Produto.builder()
                    .sku("REL-001")
                    .nome("Relógio digital")
                    .preco(new BigDecimal("129.90"))
                    .pesoKg(new BigDecimal("0.20"))
                    .estoque(8)
                    .foraDeLinha(true)
                    .build()
            );

            produtoRepository.saveAll(produtos);
        }
    }
}

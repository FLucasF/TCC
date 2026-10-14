package com.loja.pedidos.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.Instant;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RespostaPedido {
    private Long id;
    private String situacao;
    private Instant criadoEm;
    private Resumo resumo;
    private List<Item> itens;
    private String codigoRastreio;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Item {
        private String sku;
        private Integer quantidade;
    }
}

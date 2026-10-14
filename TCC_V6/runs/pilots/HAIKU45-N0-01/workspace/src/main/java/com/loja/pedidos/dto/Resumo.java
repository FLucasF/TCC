package com.loja.pedidos.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Resumo {
    private BigDecimal subtotalProdutos;
    private BigDecimal descontoCupom;
    private BigDecimal frete;
    private Integer prazoEntregaDias;
    private BigDecimal seguro;
    private BigDecimal ajustePagamento;
    private BigDecimal totalFinal;
    private Integer parcelas;
    private BigDecimal valorParcela;
    private BigDecimal creditoProximaCompra;
    private boolean brinde;
}

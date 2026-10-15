package com.loja.strategy.pagamento;

import java.math.BigDecimal;

public class PagamentoResult {
  private BigDecimal totalFinal;
  private Integer parcelas;
  private BigDecimal valorParcela;
  private BigDecimal ajuste;

  public PagamentoResult(BigDecimal totalFinal, Integer parcelas, BigDecimal valorParcela, BigDecimal totalPedido) {
    this.totalFinal = totalFinal;
    this.parcelas = parcelas;
    this.valorParcela = valorParcela;
    this.ajuste = totalFinal.subtract(totalPedido);
  }

  public BigDecimal getTotalFinal() {
    return totalFinal;
  }

  public Integer getParcelas() {
    return parcelas;
  }

  public BigDecimal getValorParcela() {
    return valorParcela;
  }

  public BigDecimal getAjuste() {
    return ajuste;
  }
}

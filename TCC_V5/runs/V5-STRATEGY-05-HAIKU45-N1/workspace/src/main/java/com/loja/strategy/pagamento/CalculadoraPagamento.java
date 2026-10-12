package com.loja.strategy.pagamento;

import java.math.BigDecimal;

public interface CalculadoraPagamento {
  PagamentoResult calcular(BigDecimal totalPedido, Integer parcelas);

  boolean podeAplicar(BigDecimal totalPedido, Integer parcelas);
}

package com.loja.checkout.pagamento;

import java.math.BigDecimal;

public interface CalculadoraPagamento {
    PagamentoInfo calcular(BigDecimal total, int parcelas);
    boolean podeParcelar(int parcelas);
    boolean podeUsar(BigDecimal total);
}

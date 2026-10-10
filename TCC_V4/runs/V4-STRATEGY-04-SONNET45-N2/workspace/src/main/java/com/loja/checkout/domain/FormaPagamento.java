package com.loja.checkout.domain;

import java.math.BigDecimal;

public interface FormaPagamento {
    BigDecimal calcularValorFinal(BigDecimal totalPedido, int parcelas);
    boolean aceitaParcelas(int parcelas);
    boolean aceita(BigDecimal totalPedido);
}

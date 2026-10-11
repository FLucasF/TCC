package com.loja.checkout.domain.pagamento;

import java.math.BigDecimal;

public interface FormaPagamento {
    boolean aceitaParcelas(int numeroParcelas);
    boolean estaDisponivel(BigDecimal totalPedido);
    BigDecimal calcularAjuste(BigDecimal totalPedido, int numeroParcelas);
    BigDecimal calcularValorFinal(BigDecimal totalPedido, int numeroParcelas);
    BigDecimal calcularValorParcela(BigDecimal totalPedido, int numeroParcelas);
}

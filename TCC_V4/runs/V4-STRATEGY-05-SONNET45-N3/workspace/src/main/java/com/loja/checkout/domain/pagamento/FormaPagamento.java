package com.loja.checkout.domain.pagamento;

import java.math.BigDecimal;

public interface FormaPagamento {
    boolean estaDisponivel(BigDecimal totalPedido);
    boolean parcelamentoValido(int numeroParcelas);
    BigDecimal calcularAjuste(BigDecimal totalPedido, int numeroParcelas);
    BigDecimal calcularValorParcela(BigDecimal totalFinal, int numeroParcelas);
}

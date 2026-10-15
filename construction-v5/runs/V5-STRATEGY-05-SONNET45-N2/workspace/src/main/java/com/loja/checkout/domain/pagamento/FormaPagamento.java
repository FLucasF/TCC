package com.loja.checkout.domain.pagamento;

import java.math.BigDecimal;

public interface FormaPagamento {
    boolean estaDisponivel(BigDecimal totalPedido);
    boolean aceitaParcelas(int numeroParcelas);
    BigDecimal calcularAjuste(BigDecimal totalPedido, int parcelas);
    BigDecimal calcularValorParcela(BigDecimal totalPedido, int parcelas);
}

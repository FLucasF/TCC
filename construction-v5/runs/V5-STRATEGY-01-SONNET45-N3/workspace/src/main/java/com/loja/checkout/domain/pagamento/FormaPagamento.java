package com.loja.checkout.domain.pagamento;

import java.math.BigDecimal;

public interface FormaPagamento {

    BigDecimal calcularAjuste(BigDecimal totalPedido, int parcelas);

    BigDecimal calcularValorParcela(BigDecimal totalPedido, BigDecimal ajuste, int parcelas);

    boolean aceitaParcelamento(int parcelas);

    boolean aceitaPedido(BigDecimal totalPedido);
}

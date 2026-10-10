package com.loja.checkout.pagamento;

import java.math.BigDecimal;

public interface FormaPagamento {

    String codigo();

    boolean isParcelasValido(int parcelas);

    boolean isDisponivel(BigDecimal totalPedido);

    ResultadoPagamento calcular(BigDecimal totalPedido, int parcelas);

    record ResultadoPagamento(BigDecimal totalFinal, BigDecimal valorParcela, int parcelas) {}
}

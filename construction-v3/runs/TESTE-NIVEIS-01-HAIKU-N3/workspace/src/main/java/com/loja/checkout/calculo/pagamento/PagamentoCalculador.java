package com.loja.checkout.calculo.pagamento;

import java.math.BigDecimal;

public interface PagamentoCalculador {
    BigDecimal calcularAjuste(BigDecimal totalPedido);
    BigDecimal calcularValorParcela(BigDecimal totalFinal, int parcelas);
    int getParcelasMaximas();
}

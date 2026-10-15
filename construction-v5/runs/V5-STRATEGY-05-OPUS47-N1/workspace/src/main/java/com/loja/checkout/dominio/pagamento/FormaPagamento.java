package com.loja.checkout.dominio.pagamento;

import java.math.BigDecimal;

public interface FormaPagamento {
    boolean parcelasValidas(int parcelas);

    boolean atende(BigDecimal totalPedido);

    Ajuste calcular(BigDecimal totalPedido, int parcelas);
}

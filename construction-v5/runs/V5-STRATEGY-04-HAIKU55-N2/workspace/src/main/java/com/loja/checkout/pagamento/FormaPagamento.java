package com.loja.checkout.pagamento;

import java.math.BigDecimal;

public interface FormaPagamento {

    String codigo();

    int parcelasMaximas();

    Pagamento calcular(BigDecimal totalPedido, int parcelas);

    default boolean disponivel(BigDecimal totalPedido) {
        return true;
    }
}

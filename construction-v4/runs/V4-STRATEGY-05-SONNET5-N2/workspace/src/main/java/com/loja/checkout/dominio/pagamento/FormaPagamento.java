package com.loja.checkout.dominio.pagamento;

import java.math.BigDecimal;

public interface FormaPagamento {

    String codigo();

    boolean parcelamentoPermitido(int parcelas);

    default boolean disponivel(BigDecimal totalPedido) {
        return true;
    }

    ResultadoPagamento calcular(BigDecimal totalPedido, int parcelas);
}

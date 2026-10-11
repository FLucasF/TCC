package com.loja.checkout.dominio.pagamento;

import java.math.BigDecimal;

public interface FormaPagamento {

    String codigo();

    default void validarParcelas(int parcelas) {
    }

    default void validarDisponibilidade(BigDecimal totalPedido) {
    }

    AjustePagamento calcular(BigDecimal totalPedido, int parcelas);
}

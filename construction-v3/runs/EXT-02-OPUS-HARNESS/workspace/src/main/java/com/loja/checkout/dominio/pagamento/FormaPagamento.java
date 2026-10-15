package com.loja.checkout.dominio.pagamento;

import com.loja.checkout.dominio.Codificavel;
import com.loja.checkout.dominio.ValoresPedido;

public interface FormaPagamento extends Codificavel {

    boolean aceitaParcelas(int parcelas);

    /** Se a forma de pagamento atende este pedido (limites de valor). */
    default boolean atende(ValoresPedido valores) {
        return true;
    }

    Parcelamento calcular(ValoresPedido valores, int parcelas);
}

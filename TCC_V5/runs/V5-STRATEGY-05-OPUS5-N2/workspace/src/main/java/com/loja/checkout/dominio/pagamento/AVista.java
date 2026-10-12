package com.loja.checkout.dominio.pagamento;

/** Formas que nao parcelam: sempre 1 vez. */
public interface AVista extends FormaPagamento {

    @Override
    default boolean aceitaParcelas(int parcelas) {
        return parcelas == 1;
    }
}

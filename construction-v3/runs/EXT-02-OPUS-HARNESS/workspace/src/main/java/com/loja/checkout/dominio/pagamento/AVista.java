package com.loja.checkout.dominio.pagamento;

/** Formas que não parcelam: sempre 1x. */
public interface AVista extends FormaPagamento {

    @Override
    default boolean aceitaParcelas(int parcelas) {
        return parcelas == 1;
    }
}

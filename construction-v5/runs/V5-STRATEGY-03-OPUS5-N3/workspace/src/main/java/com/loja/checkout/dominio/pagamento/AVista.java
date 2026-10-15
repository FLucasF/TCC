package com.loja.checkout.dominio.pagamento;

/** Parte comum das formas que não parcelam. */
abstract class AVista implements FormaPagamento {

    @Override
    public boolean parcelasPermitidas(int parcelas) {
        return parcelas == 1;
    }
}

package com.loja.checkout.pagamento;

import com.loja.checkout.dominio.Registro;

/** As formas de pagamento que a loja aceita. */
public final class FormasPagamento {

    public static final Registro<FormaPagamento> REGISTRO = new Registro<>(
            new Pix(),
            new Cartao(),
            new Boleto());

    private FormasPagamento() {
    }
}

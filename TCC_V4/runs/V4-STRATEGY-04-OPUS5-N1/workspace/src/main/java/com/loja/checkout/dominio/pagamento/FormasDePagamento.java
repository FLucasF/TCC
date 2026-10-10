package com.loja.checkout.dominio.pagamento;

import com.loja.checkout.dominio.Catalogo;
import com.loja.checkout.dominio.CodigoErro;
import java.util.List;

/** As formas de pagamento que a loja aceita. */
public final class FormasDePagamento {

    public static final Catalogo<FormaPagamento> CATALOGO = Catalogo.de(
            CodigoErro.FORMA_PAGAMENTO_INVALIDA,
            FormaPagamento::codigo,
            List.of(new Pix(), new Cartao(), new Boleto()));

    private FormasDePagamento() {
    }
}

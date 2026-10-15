package com.loja.checkout.pagamento;

import com.loja.checkout.dominio.Registro;

import java.util.List;
import java.util.Optional;

/** As formas de pagamento aceitas hoje. */
public final class FormasPagamento {

    private static final List<FormaPagamento> CASOS = List.of(new Pix(), new Cartao(), new Boleto());

    private FormasPagamento() {
    }

    public static Optional<FormaPagamento> porCodigo(String codigo) {
        return Registro.busca(CASOS, FormaPagamento::codigo, codigo);
    }
}

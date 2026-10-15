package br.com.loja.pedidos.checkout.cupom;

import br.com.loja.pedidos.comum.ErroNegocio;

final class NaoAplicavel {

    private NaoAplicavel() {
    }

    static ErroNegocio erro() {
        return ErroNegocio.naoProcessavel("CUPOM_NAO_APLICAVEL");
    }
}

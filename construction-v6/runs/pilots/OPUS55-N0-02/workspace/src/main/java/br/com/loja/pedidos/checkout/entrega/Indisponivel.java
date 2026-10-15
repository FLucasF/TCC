package br.com.loja.pedidos.checkout.entrega;

import br.com.loja.pedidos.comum.ErroNegocio;

final class Indisponivel {

    private Indisponivel() {
    }

    static ErroNegocio modalidade() {
        return ErroNegocio.naoProcessavel("MODALIDADE_INDISPONIVEL");
    }

    static ErroNegocio transportadora() {
        return new ErroNegocio(503, "TRANSPORTADORA_INDISPONIVEL");
    }
}

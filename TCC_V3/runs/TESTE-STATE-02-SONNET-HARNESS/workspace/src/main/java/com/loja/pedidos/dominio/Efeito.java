package com.loja.pedidos.dominio;

import java.math.BigDecimal;

public record Efeito(Situacao novaSituacao, BigDecimal reembolso, boolean estoqueDevolvido, boolean coletaAgendada) {

    static Efeito transicaoSimples(Situacao novaSituacao) {
        return new Efeito(novaSituacao, BigDecimal.ZERO, false, false);
    }

    static Efeito cancelamento(BigDecimal reembolso, boolean estoqueDevolvido) {
        return new Efeito(Situacao.CANCELADO, reembolso, estoqueDevolvido, false);
    }

    static Efeito devolucao(BigDecimal reembolso) {
        return new Efeito(Situacao.DEVOLVIDO, reembolso, false, true);
    }
}

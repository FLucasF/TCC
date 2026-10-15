package com.loja.pedidos.dominio;

import java.math.BigDecimal;

/**
 * Resultado de aplicar uma ação a um pedido: para onde ele vai e o que muda
 * junto (reembolso, estoque, coleta). Cada situação monta o seu próprio
 * Efeito para cada ação que aceita.
 */
public record Efeito(
        Situacao novaSituacao,
        BigDecimal reembolso,
        boolean estoqueDevolvido,
        boolean coletaAgendada) {

    public static Efeito mudarPara(Situacao novaSituacao) {
        return new Efeito(novaSituacao, BigDecimal.ZERO.setScale(2), false, false);
    }

    public Efeito comReembolso(BigDecimal valor) {
        return new Efeito(novaSituacao, valor.setScale(2), estoqueDevolvido, coletaAgendada);
    }

    public Efeito comEstoqueDevolvido() {
        return new Efeito(novaSituacao, reembolso, true, coletaAgendada);
    }

    public Efeito comColetaAgendada() {
        return new Efeito(novaSituacao, reembolso, estoqueDevolvido, true);
    }
}

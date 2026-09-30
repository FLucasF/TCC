package com.loja.pedidos.dominio;

import java.math.BigDecimal;
import java.util.function.Function;

/**
 * O que acontece com o pedido quando uma acao permitida e aplicada:
 * a nova situacao, quanto o cliente recebe de volta, se o estoque volta na
 * hora e se uma coleta e agendada.
 */
public record Transicao(
        Situacao destino,
        Function<Pedido, BigDecimal> reembolso,
        boolean devolveEstoque,
        boolean agendaColeta) {

    /** Transicao que so muda a situacao: sem reembolso, sem estoque, sem coleta. */
    public static Transicao para(Situacao destino) {
        return new Transicao(destino, pedido -> Dinheiro.ZERO, false, false);
    }

    public Transicao comReembolso(Function<Pedido, BigDecimal> calculo) {
        return new Transicao(destino, calculo, devolveEstoque, agendaColeta);
    }

    public Transicao devolvendoEstoque() {
        return new Transicao(destino, reembolso, true, agendaColeta);
    }

    public Transicao agendandoColeta() {
        return new Transicao(destino, reembolso, devolveEstoque, true);
    }
}

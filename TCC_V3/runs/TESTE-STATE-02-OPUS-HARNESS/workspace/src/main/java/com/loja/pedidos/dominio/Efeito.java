package com.loja.pedidos.dominio;

import java.math.BigDecimal;

/** O que acontece com o pedido, alem da mudanca de situacao, quando uma acao e aplicada. */
@FunctionalInterface
public interface Efeito {

    BigDecimal TAXA_DE_SEPARACAO = new BigDecimal("15.00");

    Efeito NENHUM = pedido -> {
    };

    Efeito REEMBOLSA_O_TOTAL = pedido -> pedido.reembolsar(pedido.valorTotal());

    Efeito REEMBOLSA_O_TOTAL_MENOS_A_TAXA_DE_SEPARACAO =
            pedido -> pedido.reembolsar(pedido.valorTotal().subtract(TAXA_DE_SEPARACAO).max(Dinheiro.ZERO));

    Efeito REEMBOLSA_SO_OS_PRODUTOS = pedido -> pedido.reembolsar(pedido.valorProdutos());

    Efeito DEVOLVE_O_ESTOQUE = Pedido::devolverEstoque;

    Efeito AGENDA_A_COLETA = Pedido::agendarColeta;

    void aplicar(Pedido pedido);

    default Efeito e(Efeito outro) {
        return pedido -> {
            aplicar(pedido);
            outro.aplicar(pedido);
        };
    }
}

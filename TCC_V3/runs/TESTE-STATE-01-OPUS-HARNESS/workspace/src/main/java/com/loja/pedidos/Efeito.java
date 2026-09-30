package com.loja.pedidos;

/** O que acontece com o pedido, além da troca de situação, quando uma ação é aplicada. */
@FunctionalInterface
public interface Efeito {

    Efeito NENHUM = pedido -> { };

    Efeito REEMBOLSAR_TOTAL = pedido -> pedido.reembolsar(pedido.getValorTotal());

    Efeito REEMBOLSAR_TOTAL_MENOS_TAXA_DE_SEPARACAO = pedido -> {
        pedido.reembolsar(pedido.getValorTotal().subtract(Pedido.TAXA_DE_SEPARACAO).max(Pedido.ZERO));
        pedido.devolverEstoque();
    };

    Efeito REEMBOLSAR_PRODUTOS_E_AGENDAR_COLETA = pedido -> {
        pedido.reembolsar(pedido.getValorProdutos());
        pedido.agendarColeta();
    };

    void aplicar(Pedido pedido);
}

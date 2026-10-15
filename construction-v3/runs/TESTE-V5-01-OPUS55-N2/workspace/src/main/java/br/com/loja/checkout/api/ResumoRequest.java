package br.com.loja.checkout.api;

import br.com.loja.checkout.Carrinho;
import br.com.loja.checkout.Erro;
import br.com.loja.checkout.Item;
import br.com.loja.checkout.Pedido;
import br.com.loja.checkout.PedidoRecusadoException;
import java.math.BigDecimal;
import java.util.List;

public record ResumoRequest(
        List<ItemRequest> itens,
        String modalidadeEntrega,
        String cupom,
        String formaPagamento,
        Integer parcelas,
        String nivelClube,
        String regiao) {

    public Pedido paraPedido() {
        return new Pedido(carrinho(), modalidadeEntrega, cupom, formaPagamento,
                parcelas == null ? 1 : parcelas, nivelClube, regiao);
    }

    private Carrinho carrinho() {
        if (itens == null || itens.isEmpty()) {
            throw new PedidoRecusadoException(Erro.PEDIDO_INVALIDO);
        }
        return new Carrinho(itens.stream().map(ResumoRequest::item).toList());
    }

    private static Item item(ItemRequest item) {
        if (item == null || !positivo(item.precoUnitario()) || item.quantidade() == null
                || item.quantidade() <= 0 || !positivo(item.pesoKg())) {
            throw new PedidoRecusadoException(Erro.PEDIDO_INVALIDO);
        }
        return new Item(item.nome(), item.precoUnitario(), item.quantidade(), item.pesoKg());
    }

    private static boolean positivo(BigDecimal valor) {
        return valor != null && valor.signum() > 0;
    }
}

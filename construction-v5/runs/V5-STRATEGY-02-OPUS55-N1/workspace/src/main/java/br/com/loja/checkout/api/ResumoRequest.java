package br.com.loja.checkout.api;

import br.com.loja.checkout.dominio.Carrinho;
import br.com.loja.checkout.dominio.CheckoutException;
import br.com.loja.checkout.dominio.Item;
import br.com.loja.checkout.dominio.Pedido;
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

    public record ItemRequest(String nome, BigDecimal precoUnitario, Integer quantidade, BigDecimal pesoKg) {

        boolean valido() {
            return positivo(precoUnitario) && quantidade != null && quantidade > 0 && positivo(pesoKg);
        }

        Item paraItem() {
            return new Item(nome, precoUnitario, quantidade, pesoKg);
        }

        private static boolean positivo(BigDecimal valor) {
            return valor != null && valor.signum() > 0;
        }
    }

    public Pedido paraPedido() {
        if (itens == null || itens.isEmpty() || !itens.stream().allMatch(i -> i != null && i.valido())) {
            throw new CheckoutException("PEDIDO_INVALIDO");
        }
        Carrinho carrinho = new Carrinho(itens.stream().map(ItemRequest::paraItem).toList());
        return new Pedido(carrinho, modalidadeEntrega, cupom, formaPagamento,
                parcelas == null ? 1 : parcelas, nivelClube, regiao);
    }
}

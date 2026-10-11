package br.com.loja.checkout.dominio;

import java.math.BigDecimal;
import java.util.List;

/** O carrinho já conferido: itens válidos, com subtotal e peso prontos. */
public record Pedido(List<Item> itens, BigDecimal subtotal, BigDecimal pesoKg) {

    public static Pedido de(List<Item> itens) {
        if (itens == null || itens.isEmpty()) {
            throw new PedidoRecusadoException(MotivoRecusa.PEDIDO_INVALIDO);
        }
        BigDecimal subtotal = Dinheiro.ZERO;
        BigDecimal peso = BigDecimal.ZERO;
        for (Item item : itens) {
            if (!valido(item)) {
                throw new PedidoRecusadoException(MotivoRecusa.PEDIDO_INVALIDO);
            }
            subtotal = subtotal.add(item.total());
            peso = peso.add(item.peso());
        }
        return new Pedido(List.copyOf(itens), Dinheiro.centavos(subtotal), peso);
    }

    private static boolean valido(Item item) {
        return item != null
                && positivo(item.precoUnitario())
                && item.quantidade() > 0
                && positivo(item.pesoKg());
    }

    private static boolean positivo(BigDecimal valor) {
        return valor != null && valor.signum() > 0;
    }
}

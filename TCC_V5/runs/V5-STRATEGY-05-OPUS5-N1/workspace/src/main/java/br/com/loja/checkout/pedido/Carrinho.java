package br.com.loja.checkout.pedido;

import br.com.loja.checkout.comum.CodigoErro;
import br.com.loja.checkout.comum.PedidoRecusadoException;
import java.math.BigDecimal;
import java.util.List;

/** O carrinho conferido: itens válidos, com subtotal e peso do pedido. */
public record Carrinho(List<Item> itens) {

    public static Carrinho conferir(List<ItemBruto> brutos) {
        if (brutos == null || brutos.isEmpty()) {
            throw new PedidoRecusadoException(CodigoErro.PEDIDO_INVALIDO);
        }
        return new Carrinho(brutos.stream().map(Carrinho::conferir).toList());
    }

    private static Item conferir(ItemBruto bruto) {
        if (bruto == null || !positivo(bruto.precoUnitario()) || !positivo(bruto.pesoKg())
                || bruto.quantidade() == null || bruto.quantidade() <= 0) {
            throw new PedidoRecusadoException(CodigoErro.PEDIDO_INVALIDO);
        }
        return new Item(bruto.nome(), bruto.precoUnitario(), bruto.quantidade(), bruto.pesoKg());
    }

    private static boolean positivo(BigDecimal valor) {
        return valor != null && valor.compareTo(BigDecimal.ZERO) > 0;
    }

    public BigDecimal subtotalProdutos() {
        return itens.stream().map(Item::total).reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    public BigDecimal pesoKg() {
        return itens.stream().map(Item::peso).reduce(BigDecimal.ZERO, BigDecimal::add);
    }
}

package com.loja.checkout.dominio;

import com.loja.checkout.dominio.Solicitacao.ItemInformado;

import java.math.BigDecimal;
import java.util.List;

/** Transforma os itens informados pelo site num pedido, ou recusa a compra. */
public final class Carrinho {

    private Carrinho() {
    }

    public static Pedido validar(List<ItemInformado> informados) {
        if (informados == null || informados.isEmpty()) {
            throw new ErroPedido("PEDIDO_INVALIDO");
        }
        return new Pedido(informados.stream().map(Carrinho::item).toList());
    }

    private static Item item(ItemInformado informado) {
        if (informado == null
                || !positivo(informado.precoUnitario())
                || !positivo(informado.quantidade())
                || !positivo(informado.pesoKg())) {
            throw new ErroPedido("PEDIDO_INVALIDO");
        }
        return new Item(
                informado.nome(),
                informado.precoUnitario(),
                informado.quantidade(),
                informado.pesoKg());
    }

    private static boolean positivo(BigDecimal valor) {
        return valor != null && valor.compareTo(BigDecimal.ZERO) > 0;
    }

    private static boolean positivo(Integer valor) {
        return valor != null && valor > 0;
    }
}

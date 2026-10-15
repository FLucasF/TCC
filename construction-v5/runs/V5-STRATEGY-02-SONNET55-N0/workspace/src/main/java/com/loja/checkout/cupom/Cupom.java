package com.loja.checkout.cupom;

import java.math.BigDecimal;
import java.util.List;
import com.loja.checkout.dominio.Item;

/** Nova promoção = nova classe @Component implementando esta interface. */
public interface Cupom {
    String codigo();

    /** Se o pedido cumpre a condição do cupom. */
    default boolean aplicavel(ContextoCupom ctx) {
        return true;
    }

    /** Desconto já arredondado para centavos. */
    BigDecimal desconto(ContextoCupom ctx);

    record ContextoCupom(List<Item> itens, BigDecimal subtotalProdutos, BigDecimal frete) {
    }
}

package br.com.loja.pedidos.club;

import java.math.BigDecimal;

import br.com.loja.pedidos.common.Coded;
import br.com.loja.pedidos.common.Money;
import br.com.loja.pedidos.shipping.ShippingOption;

/** Um nível do clube e suas vantagens. Por padrão, nenhuma. */
public interface ClubTier extends Coded {

    default BigDecimal nextPurchaseCredit(BigDecimal productsSubtotal) {
        return Money.ZERO;
    }

    default boolean freeShipping(ShippingOption shipping) {
        return false;
    }

    default boolean gift(BigDecimal productsSubtotal) {
        return false;
    }
}

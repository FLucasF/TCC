package br.com.loja.club;

import java.math.BigDecimal;

/** Um nível do clube e suas vantagens. Novo nível = novo bean que implemente esta interface. */
public interface ClubTier {

    String code();

    /** Crédito para a próxima compra, sobre o valor dos produtos. */
    BigDecimal credit(BigDecimal productsSubtotal);

    boolean waivesFreight();

    boolean gift(BigDecimal productsSubtotal);
}

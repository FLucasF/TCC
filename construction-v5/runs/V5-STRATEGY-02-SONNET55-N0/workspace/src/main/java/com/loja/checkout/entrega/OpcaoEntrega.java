package com.loja.checkout.entrega;

import java.math.BigDecimal;

/** Nova opção de entrega = nova classe @Component implementando esta interface. */
public interface OpcaoEntrega {
    String codigo();

    int prazoDias();

    /** Se a opção atende um pedido com este peso total. */
    default boolean atende(BigDecimal pesoKg) {
        return true;
    }

    /** Frete já arredondado para centavos. */
    BigDecimal frete(BigDecimal pesoKg);
}

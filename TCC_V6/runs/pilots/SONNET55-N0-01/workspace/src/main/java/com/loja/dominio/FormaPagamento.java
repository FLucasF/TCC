package com.loja.dominio;

import java.math.BigDecimal;

/** Uma forma de pagamento. Para criar uma nova, basta uma nova classe @Component. */
public interface FormaPagamento {

    record Resultado(BigDecimal totalFinal, BigDecimal valorParcela) {
    }

    String codigo();

    boolean parcelasPermitidas(int parcelas);

    /** Se a forma de pagamento atende um pedido com este total. */
    default boolean disponivel(BigDecimal totalPedido) {
        return true;
    }

    Resultado calcular(BigDecimal totalPedido, int parcelas, boolean usouCupom);
}

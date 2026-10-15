package com.loja.checkout.pagamento;

import java.math.BigDecimal;

/** Nova forma de pagamento = nova classe @Component implementando esta interface. */
public interface FormaPagamento {
    String codigo();

    boolean parcelasPermitidas(int parcelas);

    /** Se a forma atende um pedido com este total (antes do ajuste). */
    default boolean atende(BigDecimal totalPedido) {
        return true;
    }

    Resultado calcular(BigDecimal totalPedido, int parcelas);

    /** valorFinal e valorParcela já arredondados para centavos. */
    record Resultado(BigDecimal valorFinal, BigDecimal valorParcela) {
    }
}

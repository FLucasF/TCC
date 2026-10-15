package br.loja.checkout.pagamento;

import java.math.BigDecimal;

/** Uma forma de pagamento. Para criar uma nova, basta implementar e anotar com @Component. */
public interface FormaPagamento {
    String codigo();

    boolean parcelasPermitidas(int parcelas);

    /** Se a forma de pagamento atende o pedido (ex.: limite de valor). */
    default boolean atende(BigDecimal totalPedido) {
        return true;
    }

    Cobranca cobrar(BigDecimal totalPedido, int parcelas);

    record Cobranca(BigDecimal totalFinal, BigDecimal valorParcela) {
    }
}

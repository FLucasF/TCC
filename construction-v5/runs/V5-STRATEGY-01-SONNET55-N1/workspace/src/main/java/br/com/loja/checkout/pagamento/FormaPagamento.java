package br.com.loja.checkout.pagamento;

import java.math.BigDecimal;

public interface FormaPagamento {

    String codigo();

    boolean parcelasPermitidas(int parcelas);

    default boolean atende(BigDecimal totalPedido) {
        return true;
    }

    Cobranca cobrar(BigDecimal totalPedido, int parcelas);
}

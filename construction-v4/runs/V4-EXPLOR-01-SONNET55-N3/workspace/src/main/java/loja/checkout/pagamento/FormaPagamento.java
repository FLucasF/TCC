package loja.checkout.pagamento;

import java.math.BigDecimal;

import loja.checkout.comum.Codigo;

public interface FormaPagamento extends Codigo {
    boolean parcelasPermitidas(int parcelas);

    Cobranca cobrar(BigDecimal total, int parcelas);

    default boolean atende(BigDecimal total) {
        return true;
    }
}

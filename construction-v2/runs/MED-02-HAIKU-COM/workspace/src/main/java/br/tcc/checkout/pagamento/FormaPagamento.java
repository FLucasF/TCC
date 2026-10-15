package br.tcc.checkout.pagamento;

import java.math.BigDecimal;

public interface FormaPagamento {
    BigDecimal calcularTotalFinal(BigDecimal total, Integer parcelas);

    BigDecimal calcularValorParcela(BigDecimal total, Integer parcelas);

    boolean parcelavalida(Integer parcelas);
}

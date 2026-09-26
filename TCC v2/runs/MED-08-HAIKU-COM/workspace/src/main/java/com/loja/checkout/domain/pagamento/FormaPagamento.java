package com.loja.checkout.domain.pagamento;

import java.math.BigDecimal;

public interface FormaPagamento {
    BigDecimal calcularAjuste(BigDecimal total, int parcelas);
    BigDecimal calcularValorParcela(BigDecimal totalFinal, int parcelas);
    boolean podeAplicar(BigDecimal total, int parcelas);
    String obterCodigo();
}

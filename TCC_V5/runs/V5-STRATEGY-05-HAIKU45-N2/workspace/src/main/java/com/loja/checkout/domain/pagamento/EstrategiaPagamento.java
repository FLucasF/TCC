package com.loja.checkout.domain.pagamento;

import java.math.BigDecimal;
import com.loja.checkout.domain.FormaPagamento;

public interface EstrategiaPagamento {
    boolean disponivel(BigDecimal total, int parcelas);
    int parcelasPermitidas(BigDecimal total);
    BigDecimal calcularAjuste(BigDecimal total, int parcelas);
    BigDecimal calcularParcela(BigDecimal total, int parcelas);
    FormaPagamento forma();
}

package com.loja.checkout.domain.pagamento;

import com.loja.checkout.utils.Arredondamento;
import java.math.BigDecimal;

public class FormaPagamentoPix implements FormaPagamento {
    private static final BigDecimal PERCENTUAL_DESCONTO = new BigDecimal("0.05");

    @Override
    public BigDecimal calcularAjuste(BigDecimal total, int parcelas) {
        BigDecimal desconto = total.multiply(PERCENTUAL_DESCONTO);
        desconto = Arredondamento.arredondarParaCentavos(desconto);
        return desconto.negate();
    }

    @Override
    public BigDecimal calcularValorParcela(BigDecimal totalFinal, int parcelas) {
        return Arredondamento.arredondarParaCentavos(totalFinal);
    }

    @Override
    public boolean podeAplicar(BigDecimal total, int parcelas) {
        return parcelas == 1;
    }

    @Override
    public String obterCodigo() {
        return "PIX";
    }
}

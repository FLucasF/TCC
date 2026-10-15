package com.loja.checkout.domain.pagamento;

import java.math.BigDecimal;
import com.loja.checkout.domain.FormaPagamento;
import com.loja.checkout.domain.Arredondamento;

public class PagamentoPix implements EstrategiaPagamento {
    @Override
    public boolean disponivel(BigDecimal total, int parcelas) {
        return parcelas == 1;
    }

    @Override
    public int parcelasPermitidas(BigDecimal total) {
        return 1;
    }

    @Override
    public BigDecimal calcularAjuste(BigDecimal total, int parcelas) {
        if (parcelas != 1) {
            return BigDecimal.ZERO;
        }
        BigDecimal desconto = Arredondamento.arredondar(total.multiply(new BigDecimal("0.05")));
        return desconto.negate();
    }

    @Override
    public BigDecimal calcularParcela(BigDecimal total, int parcelas) {
        return Arredondamento.arredondar(total.divide(new BigDecimal(parcelas), 2, java.math.RoundingMode.HALF_EVEN));
    }

    @Override
    public FormaPagamento forma() {
        return FormaPagamento.PIX;
    }
}

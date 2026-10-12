package com.loja.checkout.domain.pagamento;

import java.math.BigDecimal;
import com.loja.checkout.domain.FormaPagamento;
import com.loja.checkout.domain.Arredondamento;

public class PagamentoBoleto implements EstrategiaPagamento {
    private static final BigDecimal TARIFA = new BigDecimal("3.49");

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
        return TARIFA;
    }

    @Override
    public BigDecimal calcularParcela(BigDecimal total, int parcelas) {
        return Arredondamento.arredondar(total.divide(new BigDecimal(parcelas), 2, java.math.RoundingMode.HALF_EVEN));
    }

    @Override
    public FormaPagamento forma() {
        return FormaPagamento.BOLETO;
    }
}

package com.loja.checkout.strategy.pagamento;

import com.loja.checkout.exception.CheckoutException;
import com.loja.checkout.service.MoneyRounder;
import java.math.BigDecimal;

public class CartaoStrategy implements FormaPagamentoStrategy {
    private static final double TAXA_JUROS_MENSAL = 0.0199;

    @Override
    public BigDecimal calcularAjuste(BigDecimal total, int parcelas) {
        if (parcelas <= 3) {
            return BigDecimal.ZERO;
        }

        BigDecimal taxa = BigDecimal.valueOf(TAXA_JUROS_MENSAL);
        BigDecimal umMaisTaxa = BigDecimal.ONE.add(taxa);

        BigDecimal potencia = umMaisTaxa.pow(parcelas);
        BigDecimal denominador = BigDecimal.ONE.subtract(BigDecimal.ONE.divide(potencia, 10, java.math.RoundingMode.HALF_EVEN));

        BigDecimal numerador = total.multiply(taxa);
        BigDecimal parcela = MoneyRounder.round(numerador.divide(denominador, 10, java.math.RoundingMode.HALF_EVEN));
        BigDecimal totalComJuros = parcela.multiply(BigDecimal.valueOf(parcelas));
        BigDecimal ajuste = totalComJuros.subtract(total);

        return MoneyRounder.round(ajuste);
    }

    @Override
    public int getParcelasPermitidas() {
        return 12;
    }

    @Override
    public void validar(BigDecimal total, int parcelas) {
        if (parcelas < 1 || parcelas > 12) {
            throw new CheckoutException("PARCELAMENTO_INVALIDO");
        }
    }
}

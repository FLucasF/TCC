package com.loja.checkout.domain.pagamento;

import com.loja.checkout.service.CheckoutException;
import com.loja.checkout.service.CodigoErro;

import java.math.BigDecimal;
import java.math.MathContext;
import java.math.RoundingMode;

public class Cartao implements FormaPagamento {

    private static final BigDecimal TAXA_MENSAL = new BigDecimal("0.0199");
    private static final int PARCELAS_MINIMAS = 1;
    private static final int PARCELAS_MAXIMAS = 12;
    private static final int PARCELAS_SEM_JUROS = 3;

    @Override
    public void validar(int parcelas, BigDecimal totalPedido) {
        if (parcelas < PARCELAS_MINIMAS || parcelas > PARCELAS_MAXIMAS) {
            throw new CheckoutException(CodigoErro.PARCELAMENTO_INVALIDO);
        }
    }

    @Override
    public ResultadoPagamento calcular(BigDecimal totalPedido, int parcelas) {
        if (parcelas <= PARCELAS_SEM_JUROS) {
            return semJuros(totalPedido, parcelas);
        } else {
            return comJurosPrice(totalPedido, parcelas);
        }
    }

    private ResultadoPagamento semJuros(BigDecimal totalPedido, int parcelas) {
        BigDecimal valorParcela = totalPedido
                .divide(BigDecimal.valueOf(parcelas), 2, RoundingMode.HALF_EVEN);
        BigDecimal ajuste = BigDecimal.ZERO.setScale(2);
        return new ResultadoPagamento(totalPedido, valorParcela, ajuste, parcelas);
    }

    private ResultadoPagamento comJurosPrice(BigDecimal totalPedido, int parcelas) {
        // parcela = total × taxa / (1 − (1+taxa)^(−n))
        MathContext mc = MathContext.DECIMAL128;

        BigDecimal taxa = TAXA_MENSAL;
        BigDecimal um = BigDecimal.ONE;
        BigDecimal umMaisTaxa = um.add(taxa, mc);

        // (1+taxa)^(-n) = 1 / (1+taxa)^n
        BigDecimal potencia = umMaisTaxa.pow(parcelas, mc);
        BigDecimal fatorDesconto = um.divide(potencia, mc);

        BigDecimal denominador = um.subtract(fatorDesconto, mc);
        BigDecimal numerador = totalPedido.multiply(taxa, mc);
        BigDecimal valorParcela = numerador.divide(denominador, 2, RoundingMode.HALF_EVEN);

        BigDecimal totalFinal = valorParcela.multiply(BigDecimal.valueOf(parcelas))
                .setScale(2, RoundingMode.HALF_EVEN);
        BigDecimal ajuste = totalFinal.subtract(totalPedido).setScale(2, RoundingMode.HALF_EVEN);

        return new ResultadoPagamento(totalFinal, valorParcela, ajuste, parcelas);
    }
}

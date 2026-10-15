package com.loja.service;

import com.loja.enums.FormaPagamento;
import com.loja.util.ArredondadorMeioParaPar;

import java.math.BigDecimal;
import java.math.RoundingMode;
import static com.loja.util.ArredondadorMeioParaPar.arredondar;

public class CalculadorAjustePagamento {

    private static final BigDecimal TAXA_JUROS_MENSAL = new BigDecimal("0.0199");
    private static final BigDecimal TARIFA_BOLETO = new BigDecimal("3.49");
    private static final BigDecimal DESCONTO_PIX = new BigDecimal("0.05");

    public static class ResultadoPagamento {
        public final BigDecimal ajustePagamento;
        public final BigDecimal totalFinal;
        public final BigDecimal valorParcela;

        public ResultadoPagamento(BigDecimal ajustePagamento, BigDecimal totalFinal, BigDecimal valorParcela) {
            this.ajustePagamento = ajustePagamento;
            this.totalFinal = totalFinal;
            this.valorParcela = valorParcela;
        }
    }

    public static ResultadoPagamento calcular(FormaPagamento forma, BigDecimal total, Integer parcelas) {
        int numParcelas = parcelas != null ? parcelas : 1;

        return switch (forma) {
            case PIX -> calcularPix(total, numParcelas);
            case CARTAO -> calcularCartao(total, numParcelas);
            case BOLETO -> calcularBoleto(total, numParcelas);
        };
    }

    private static ResultadoPagamento calcularPix(BigDecimal total, int parcelas) {
        BigDecimal desconto = total.multiply(DESCONTO_PIX);
        desconto = ArredondadorMeioParaPar.arredondar(desconto);

        BigDecimal totalFinal = total.subtract(desconto);
        totalFinal = ArredondadorMeioParaPar.arredondar(totalFinal);

        BigDecimal ajuste = arredondar(desconto.negate());
        BigDecimal valorParcela = arredondar(totalFinal.divide(new BigDecimal(parcelas), 2, RoundingMode.HALF_EVEN));

        return new ResultadoPagamento(ajuste, totalFinal, valorParcela);
    }

    private static ResultadoPagamento calcularCartao(BigDecimal total, int parcelas) {
        if (parcelas <= 3) {
            BigDecimal valorParcela = arredondar(total.divide(new BigDecimal(parcelas), 2, RoundingMode.HALF_EVEN));
            return new ResultadoPagamento(arredondar(BigDecimal.ZERO), total, valorParcela);
        } else {
            BigDecimal valorParcela = calcularParcelaTabePrice(total, parcelas);
            BigDecimal totalFinal = arredondar(valorParcela.multiply(new BigDecimal(parcelas)));
            BigDecimal ajuste = arredondar(totalFinal.subtract(total));

            return new ResultadoPagamento(ajuste, totalFinal, valorParcela);
        }
    }

    private static BigDecimal calcularParcelaTabePrice(BigDecimal total, int parcelas) {
        BigDecimal um = BigDecimal.ONE;
        BigDecimal taxa = TAXA_JUROS_MENSAL;

        BigDecimal base = um.add(taxa);
        BigDecimal baseAoPoder = base.pow(parcelas);
        BigDecimal denominador = um.subtract(um.divide(baseAoPoder, 10, RoundingMode.HALF_EVEN));

        BigDecimal parcela = total.multiply(taxa).divide(denominador, 10, RoundingMode.HALF_EVEN);
        return arredondar(parcela);
    }

    private static ResultadoPagamento calcularBoleto(BigDecimal total, int parcelas) {
        BigDecimal totalFinal = total.add(TARIFA_BOLETO);
        totalFinal = arredondar(totalFinal);

        BigDecimal valorParcela = arredondar(totalFinal.divide(new BigDecimal(parcelas), 2, RoundingMode.HALF_EVEN));
        BigDecimal ajuste = arredondar(TARIFA_BOLETO);

        return new ResultadoPagamento(ajuste, totalFinal, valorParcela);
    }
}

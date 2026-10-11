package com.loja.checkout.calculator;

import com.loja.checkout.enums.FormaPagamento;
import com.loja.checkout.util.Arredondamento;

import java.math.BigDecimal;

public class PagamentoCalculator {

    public static class Resultado {
        public BigDecimal ajuste;
        public BigDecimal totalComAjuste;
        public BigDecimal valorParcela;
        public Integer parcelas;

        public Resultado(BigDecimal ajuste, BigDecimal totalComAjuste, BigDecimal valorParcela, Integer parcelas) {
            this.ajuste = ajuste;
            this.totalComAjuste = totalComAjuste;
            this.valorParcela = valorParcela;
            this.parcelas = parcelas;
        }
    }

    public Resultado calcular(FormaPagamento forma, BigDecimal total, Integer parcelas) {
        if (parcelas == null || parcelas == 0) {
            parcelas = 1;
        }

        return switch (forma) {
            case PIX -> calcularPix(total, parcelas);
            case CARTAO -> calcularCartao(total, parcelas);
            case BOLETO -> calcularBoleto(total, parcelas);
        };
    }

    private Resultado calcularPix(BigDecimal total, Integer parcelas) {
        if (parcelas != 1) {
            throw new IllegalArgumentException("PIX não pode ser parcelado");
        }

        BigDecimal desconto = total.multiply(new BigDecimal("0.05"));
        desconto = Arredondamento.paraCentavos(desconto);

        BigDecimal totalComAjuste = total.subtract(desconto);
        totalComAjuste = Arredondamento.paraCentavos(totalComAjuste);

        return new Resultado(desconto.negate(), totalComAjuste, totalComAjuste, 1);
    }

    private Resultado calcularCartao(BigDecimal total, Integer parcelas) {
        BigDecimal valorParcela;
        BigDecimal totalComAjuste;
        BigDecimal ajuste;

        if (parcelas <= 3) {
            valorParcela = total.divide(new BigDecimal(parcelas), 10, java.math.RoundingMode.HALF_EVEN);
            valorParcela = Arredondamento.paraCentavos(valorParcela);
            totalComAjuste = valorParcela.multiply(new BigDecimal(parcelas));
            totalComAjuste = Arredondamento.paraCentavos(totalComAjuste);
            ajuste = totalComAjuste.subtract(total);
        } else {
            BigDecimal taxaMensal = new BigDecimal("0.0199");
            BigDecimal fator = BigDecimal.ONE.add(taxaMensal).pow(parcelas);
            BigDecimal denominador = fator.subtract(BigDecimal.ONE).divide(taxaMensal.multiply(fator), 10, java.math.RoundingMode.HALF_EVEN);

            valorParcela = total.divide(denominador, 10, java.math.RoundingMode.HALF_EVEN);
            valorParcela = Arredondamento.paraCentavos(valorParcela);

            totalComAjuste = valorParcela.multiply(new BigDecimal(parcelas));
            totalComAjuste = Arredondamento.paraCentavos(totalComAjuste);

            ajuste = totalComAjuste.subtract(total);
        }

        return new Resultado(ajuste, totalComAjuste, valorParcela, parcelas);
    }

    private Resultado calcularBoleto(BigDecimal total, Integer parcelas) {
        if (parcelas != 1) {
            throw new IllegalArgumentException("Boleto não pode ser parcelado");
        }

        BigDecimal tarifa = new BigDecimal("3.49");
        BigDecimal totalComAjuste = total.add(tarifa);
        totalComAjuste = Arredondamento.paraCentavos(totalComAjuste);

        return new Resultado(tarifa, totalComAjuste, totalComAjuste, 1);
    }
}

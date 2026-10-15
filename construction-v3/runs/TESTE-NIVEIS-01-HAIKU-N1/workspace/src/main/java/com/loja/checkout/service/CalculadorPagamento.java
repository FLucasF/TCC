package com.loja.checkout.service;

import com.loja.checkout.model.FormaPagamento;
import com.loja.checkout.util.Arredondador;
import java.math.BigDecimal;

public class CalculadorPagamento {

    public record ResultadoPagamento(BigDecimal ajuste, BigDecimal totalComAjuste, Integer parcelas, BigDecimal valorParcela) {}

    public ResultadoPagamento calcular(BigDecimal total, FormaPagamento formaPagamento, Integer parcelas) {
        return switch (formaPagamento) {
            case PIX -> calcularPix(total, parcelas);
            case CARTAO -> calcularCartao(total, parcelas);
            case BOLETO -> calcularBoleto(total, parcelas);
        };
    }

    private ResultadoPagamento calcularPix(BigDecimal total, Integer parcelas) {
        BigDecimal desconto = total.multiply(new BigDecimal("0.05"));
        desconto = Arredondador.arredondar(desconto);

        BigDecimal totalComAjuste = total.subtract(desconto);
        BigDecimal ajuste = desconto.negate();

        BigDecimal valorParcela = Arredondador.arredondar(totalComAjuste.divide(new BigDecimal(parcelas), 10, java.math.RoundingMode.HALF_EVEN));

        return new ResultadoPagamento(ajuste, totalComAjuste, parcelas, valorParcela);
    }

    private ResultadoPagamento calcularCartao(BigDecimal total, Integer parcelas) {
        if (parcelas <= 3) {
            BigDecimal valorParcela = Arredondador.arredondar(total.divide(new BigDecimal(parcelas), 10, java.math.RoundingMode.HALF_EVEN));
            return new ResultadoPagamento(BigDecimal.ZERO, total, parcelas, valorParcela);
        }

        BigDecimal taxa = new BigDecimal("0.0199");
        BigDecimal umMaisTaxa = BigDecimal.ONE.add(taxa);
        BigDecimal divisor = BigDecimal.ONE.subtract(
            BigDecimal.ONE.divide(umMaisTaxa.pow(parcelas), 10, java.math.RoundingMode.HALF_EVEN)
        );

        BigDecimal parcela = total.multiply(taxa).divide(divisor, 10, java.math.RoundingMode.HALF_EVEN);
        parcela = Arredondador.arredondar(parcela);

        BigDecimal totalComJuros = parcela.multiply(new BigDecimal(parcelas));
        BigDecimal ajuste = totalComJuros.subtract(total);

        return new ResultadoPagamento(ajuste, totalComJuros, parcelas, parcela);
    }

    private ResultadoPagamento calcularBoleto(BigDecimal total, Integer parcelas) {
        BigDecimal tarifa = new BigDecimal("3.49");
        BigDecimal totalComAjuste = total.add(tarifa);

        BigDecimal valorParcela = Arredondador.arredondar(totalComAjuste.divide(new BigDecimal(parcelas), 10, java.math.RoundingMode.HALF_EVEN));

        return new ResultadoPagamento(tarifa, totalComAjuste, parcelas, valorParcela);
    }
}

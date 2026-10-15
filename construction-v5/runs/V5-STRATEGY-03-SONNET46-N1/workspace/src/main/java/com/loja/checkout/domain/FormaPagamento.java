package com.loja.checkout.domain;

import java.math.BigDecimal;
import java.math.RoundingMode;

import static com.loja.checkout.domain.Arredondamento.*;

public enum FormaPagamento {

    PIX {
        @Override public void validarParcelas(int parcelas) {
            if (parcelas != 1) throw new CheckoutException("PARCELAMENTO_INVALIDO");
        }

        @Override public void validarDisponibilidade(BigDecimal total) {}

        @Override public ResultadoPagamento calcular(BigDecimal total, int parcelas) {
            BigDecimal desconto = r(total.multiply(new BigDecimal("0.05")));
            BigDecimal totalFinal = total.subtract(desconto);
            return new ResultadoPagamento(totalFinal, desconto.negate(), 1, totalFinal);
        }
    },

    BOLETO {
        @Override public void validarParcelas(int parcelas) {
            if (parcelas != 1) throw new CheckoutException("PARCELAMENTO_INVALIDO");
        }

        @Override public void validarDisponibilidade(BigDecimal total) {
            if (total.compareTo(new BigDecimal("1000")) > 0)
                throw new CheckoutException("FORMA_PAGAMENTO_INDISPONIVEL");
        }

        @Override public ResultadoPagamento calcular(BigDecimal total, int parcelas) {
            BigDecimal tarifa = new BigDecimal("3.49");
            BigDecimal totalFinal = total.add(tarifa);
            return new ResultadoPagamento(totalFinal, tarifa, 1, totalFinal);
        }
    },

    CARTAO {
        @Override public void validarParcelas(int parcelas) {
            if (parcelas < 1 || parcelas > 12) throw new CheckoutException("PARCELAMENTO_INVALIDO");
        }

        @Override public void validarDisponibilidade(BigDecimal total) {}

        @Override public ResultadoPagamento calcular(BigDecimal total, int parcelas) {
            if (parcelas <= 3) {
                BigDecimal valorParcela = r(total.divide(BigDecimal.valueOf(parcelas), 10, RoundingMode.HALF_EVEN));
                return new ResultadoPagamento(total, ZERO, parcelas, valorParcela);
            }
            BigDecimal taxa = new BigDecimal("0.0199");
            BigDecimal fator = BigDecimal.ONE.add(taxa).pow(parcelas);
            BigDecimal inverseFator = BigDecimal.ONE.divide(fator, 15, RoundingMode.HALF_EVEN);
            BigDecimal denominador = BigDecimal.ONE.subtract(inverseFator);
            BigDecimal valorParcela = r(total.multiply(taxa).divide(denominador, 15, RoundingMode.HALF_EVEN));
            BigDecimal totalFinal = valorParcela.multiply(BigDecimal.valueOf(parcelas));
            return new ResultadoPagamento(totalFinal, totalFinal.subtract(total), parcelas, valorParcela);
        }
    };

    public abstract void validarParcelas(int parcelas);

    public abstract void validarDisponibilidade(BigDecimal total);

    public abstract ResultadoPagamento calcular(BigDecimal total, int parcelas);
}

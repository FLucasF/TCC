package com.loja.checkout.dominio;

import java.math.BigDecimal;
import java.math.MathContext;
import java.math.RoundingMode;

import static com.loja.checkout.dominio.CodigoErro.FORMA_PAGAMENTO_INDISPONIVEL;
import static com.loja.checkout.dominio.CodigoErro.PARCELAMENTO_INVALIDO;

public enum FormaPagamento {

    PIX {
        @Override
        public void validarParcelas(int parcelas) {
            if (parcelas != 1) throw new CheckoutException(PARCELAMENTO_INVALIDO);
        }

        @Override
        public void validarDisponibilidade(BigDecimal totalPedido) {}

        @Override
        public ResultadoPagamento calcular(BigDecimal totalPedido, int parcelas) {
            BigDecimal desconto = totalPedido.multiply(new BigDecimal("0.05")).setScale(2, RoundingMode.HALF_EVEN);
            BigDecimal totalFinal = totalPedido.subtract(desconto);
            return new ResultadoPagamento(totalFinal, desconto.negate(), 1, totalFinal);
        }
    },

    BOLETO {
        @Override
        public void validarParcelas(int parcelas) {
            if (parcelas != 1) throw new CheckoutException(PARCELAMENTO_INVALIDO);
        }

        @Override
        public void validarDisponibilidade(BigDecimal totalPedido) {
            if (totalPedido.compareTo(new BigDecimal("1000")) > 0)
                throw new CheckoutException(FORMA_PAGAMENTO_INDISPONIVEL);
        }

        @Override
        public ResultadoPagamento calcular(BigDecimal totalPedido, int parcelas) {
            BigDecimal tarifa = new BigDecimal("3.49");
            BigDecimal totalFinal = totalPedido.add(tarifa);
            return new ResultadoPagamento(totalFinal, tarifa, 1, totalFinal);
        }
    },

    CARTAO {
        @Override
        public void validarParcelas(int parcelas) {
            if (parcelas < 1 || parcelas > 12) throw new CheckoutException(PARCELAMENTO_INVALIDO);
        }

        @Override
        public void validarDisponibilidade(BigDecimal totalPedido) {}

        @Override
        public ResultadoPagamento calcular(BigDecimal totalPedido, int parcelas) {
            if (parcelas <= 3) {
                BigDecimal valorParcela = totalPedido.divide(new BigDecimal(parcelas), 2, RoundingMode.HALF_EVEN);
                return new ResultadoPagamento(totalPedido, BigDecimal.ZERO.setScale(2), parcelas, valorParcela);
            }
            BigDecimal taxa = new BigDecimal("0.0199");
            BigDecimal fator = BigDecimal.ONE.add(taxa);
            MathContext ctx = MathContext.DECIMAL128;
            BigDecimal potencia = fator.pow(parcelas, ctx);
            BigDecimal inversoPotencia = BigDecimal.ONE.divide(potencia, ctx);
            BigDecimal denominador = BigDecimal.ONE.subtract(inversoPotencia);
            BigDecimal numerador = totalPedido.multiply(taxa);
            BigDecimal valorParcela = numerador.divide(denominador, 2, RoundingMode.HALF_EVEN);
            BigDecimal totalFinal = valorParcela.multiply(new BigDecimal(parcelas));
            BigDecimal ajuste = totalFinal.subtract(totalPedido);
            return new ResultadoPagamento(totalFinal, ajuste, parcelas, valorParcela);
        }
    };

    public abstract void validarParcelas(int parcelas);

    public abstract void validarDisponibilidade(BigDecimal totalPedido);

    public abstract ResultadoPagamento calcular(BigDecimal totalPedido, int parcelas);
}

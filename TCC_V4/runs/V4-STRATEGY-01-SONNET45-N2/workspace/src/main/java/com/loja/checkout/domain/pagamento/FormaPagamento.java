package com.loja.checkout.domain.pagamento;

import com.loja.checkout.exception.CheckoutException;

import java.math.BigDecimal;
import java.math.RoundingMode;

import static com.loja.checkout.util.Moeda.arredondar;

public enum FormaPagamento {
    PIX {
        @Override
        public boolean aceitaPedido(BigDecimal totalPedido, Integer parcelas) {
            return parcelas == null || parcelas == 1;
        }

        @Override
        public ResultadoPagamento calcular(BigDecimal totalPedido, Integer numeroParcelas) {
            if (!aceitaPedido(totalPedido, numeroParcelas)) {
                throw new CheckoutException("PARCELAMENTO_INVALIDO");
            }
            BigDecimal desconto = arredondar(totalPedido.multiply(new BigDecimal("0.05")));
            BigDecimal totalFinal = arredondar(totalPedido.subtract(desconto));
            BigDecimal ajuste = totalFinal.subtract(totalPedido);
            return new ResultadoPagamento(ajuste, totalFinal, 1, totalFinal);
        }
    },
    CARTAO {
        @Override
        public boolean aceitaPedido(BigDecimal totalPedido, Integer parcelas) {
            int p = parcelas == null ? 1 : parcelas;
            return p >= 1 && p <= 12;
        }

        @Override
        public ResultadoPagamento calcular(BigDecimal totalPedido, Integer numeroParcelas) {
            int p = numeroParcelas == null ? 1 : numeroParcelas;
            if (!aceitaPedido(totalPedido, p)) {
                throw new CheckoutException("PARCELAMENTO_INVALIDO");
            }

            if (p <= 3) {
                BigDecimal valorParcela = arredondar(totalPedido.divide(BigDecimal.valueOf(p), 10, RoundingMode.HALF_EVEN));
                BigDecimal totalFinal = totalPedido;
                BigDecimal ajuste = BigDecimal.ZERO.setScale(2);
                return new ResultadoPagamento(ajuste, totalFinal, p, valorParcela);
            } else {
                BigDecimal taxa = new BigDecimal("0.0199");
                BigDecimal fator = BigDecimal.ONE.add(taxa).pow(p);
                BigDecimal denominador = fator.subtract(BigDecimal.ONE);
                BigDecimal numerador = totalPedido.multiply(taxa).multiply(fator);
                BigDecimal valorParcela = arredondar(numerador.divide(denominador, 10, RoundingMode.HALF_EVEN));
                BigDecimal totalFinal = arredondar(valorParcela.multiply(BigDecimal.valueOf(p)));
                BigDecimal ajuste = totalFinal.subtract(totalPedido);
                return new ResultadoPagamento(ajuste, totalFinal, p, valorParcela);
            }
        }
    },
    BOLETO {
        @Override
        public boolean aceitaPedido(BigDecimal totalPedido, Integer parcelas) {
            if (parcelas != null && parcelas != 1) {
                return false;
            }
            return totalPedido.compareTo(new BigDecimal("1000.00")) <= 0;
        }

        @Override
        public ResultadoPagamento calcular(BigDecimal totalPedido, Integer numeroParcelas) {
            if (numeroParcelas != null && numeroParcelas != 1) {
                throw new CheckoutException("PARCELAMENTO_INVALIDO");
            }
            if (totalPedido.compareTo(new BigDecimal("1000.00")) > 0) {
                throw new CheckoutException("FORMA_PAGAMENTO_INDISPONIVEL");
            }
            BigDecimal tarifa = new BigDecimal("3.49");
            BigDecimal totalFinal = arredondar(totalPedido.add(tarifa));
            BigDecimal ajuste = totalFinal.subtract(totalPedido);
            return new ResultadoPagamento(ajuste, totalFinal, 1, totalFinal);
        }
    };

    public abstract boolean aceitaPedido(BigDecimal totalPedido, Integer parcelas);
    public abstract ResultadoPagamento calcular(BigDecimal totalPedido, Integer numeroParcelas);
}

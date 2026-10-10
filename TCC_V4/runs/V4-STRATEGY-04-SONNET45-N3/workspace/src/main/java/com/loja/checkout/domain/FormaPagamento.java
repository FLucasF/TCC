package com.loja.checkout.domain;

import com.loja.checkout.exception.CheckoutException;
import java.math.BigDecimal;

public enum FormaPagamento {
    PIX {
        @Override
        public void validarParcelas(Integer parcelas) {
            if (parcelas != null && parcelas != 1) {
                throw new CheckoutException("PARCELAMENTO_INVALIDO");
            }
        }

        @Override
        public void validarDisponibilidade(BigDecimal totalPedido) {
        }

        @Override
        public BigDecimal calcularAjuste(BigDecimal totalPedido, int parcelas) {
            BigDecimal desconto = totalPedido.multiply(new BigDecimal("0.05"))
                .setScale(2, java.math.RoundingMode.HALF_EVEN);
            return desconto.negate();
        }

        @Override
        public BigDecimal calcularTotalFinal(BigDecimal totalPedido, int parcelas) {
            return totalPedido.add(calcularAjuste(totalPedido, parcelas))
                .setScale(2, java.math.RoundingMode.HALF_EVEN);
        }

        @Override
        public int getParcelas(Integer parcelasRequisitadas) {
            return 1;
        }
    },
    BOLETO {
        @Override
        public void validarParcelas(Integer parcelas) {
            if (parcelas != null && parcelas != 1) {
                throw new CheckoutException("PARCELAMENTO_INVALIDO");
            }
        }

        @Override
        public void validarDisponibilidade(BigDecimal totalPedido) {
            if (totalPedido.compareTo(new BigDecimal("1000.00")) > 0) {
                throw new CheckoutException("FORMA_PAGAMENTO_INDISPONIVEL");
            }
        }

        @Override
        public BigDecimal calcularAjuste(BigDecimal totalPedido, int parcelas) {
            return new BigDecimal("3.49").setScale(2, java.math.RoundingMode.HALF_EVEN);
        }

        @Override
        public BigDecimal calcularTotalFinal(BigDecimal totalPedido, int parcelas) {
            return totalPedido.add(calcularAjuste(totalPedido, parcelas))
                .setScale(2, java.math.RoundingMode.HALF_EVEN);
        }

        @Override
        public int getParcelas(Integer parcelasRequisitadas) {
            return 1;
        }
    },
    CARTAO {
        @Override
        public void validarParcelas(Integer parcelas) {
            int p = parcelas == null ? 1 : parcelas;
            if (p < 1 || p > 12) {
                throw new CheckoutException("PARCELAMENTO_INVALIDO");
            }
        }

        @Override
        public void validarDisponibilidade(BigDecimal totalPedido) {
        }

        @Override
        public BigDecimal calcularAjuste(BigDecimal totalPedido, int parcelas) {
            return calcularTotalFinal(totalPedido, parcelas).subtract(totalPedido)
                .setScale(2, java.math.RoundingMode.HALF_EVEN);
        }

        @Override
        public BigDecimal calcularTotalFinal(BigDecimal totalPedido, int parcelas) {
            if (parcelas <= 3) {
                return totalPedido;
            }

            BigDecimal taxa = new BigDecimal("0.0199");
            BigDecimal um = BigDecimal.ONE;
            BigDecimal umMaisTaxa = um.add(taxa);
            BigDecimal umMaisTaxaElevado = umMaisTaxa.pow(parcelas);
            BigDecimal numerador = totalPedido.multiply(taxa);
            BigDecimal denominador = um.subtract(um.divide(umMaisTaxaElevado, 10, java.math.RoundingMode.HALF_EVEN));

            BigDecimal valorParcela = numerador.divide(denominador, 2, java.math.RoundingMode.HALF_EVEN);
            return valorParcela.multiply(BigDecimal.valueOf(parcelas))
                .setScale(2, java.math.RoundingMode.HALF_EVEN);
        }

        @Override
        public int getParcelas(Integer parcelasRequisitadas) {
            return parcelasRequisitadas == null ? 1 : parcelasRequisitadas;
        }
    };

    public abstract void validarParcelas(Integer parcelas);
    public abstract void validarDisponibilidade(BigDecimal totalPedido);
    public abstract BigDecimal calcularAjuste(BigDecimal totalPedido, int parcelas);
    public abstract BigDecimal calcularTotalFinal(BigDecimal totalPedido, int parcelas);
    public abstract int getParcelas(Integer parcelasRequisitadas);

    public BigDecimal calcularValorParcela(BigDecimal totalFinal, int parcelas) {
        return totalFinal.divide(BigDecimal.valueOf(parcelas), 2, java.math.RoundingMode.HALF_EVEN);
    }

    public static FormaPagamento fromString(String valor) {
        if (valor == null) {
            throw new CheckoutException("FORMA_PAGAMENTO_INVALIDA");
        }
        try {
            return FormaPagamento.valueOf(valor);
        } catch (IllegalArgumentException e) {
            throw new CheckoutException("FORMA_PAGAMENTO_INVALIDA");
        }
    }
}

package com.lojaroupas.enums;

import java.math.BigDecimal;
import java.math.RoundingMode;

public enum FormaPagamento {
    PIX {
        @Override
        public boolean validaParcelamento(Integer parcelas) {
            return parcelas == 1;
        }

        @Override
        public boolean validaParaPedido(Double total) {
            return true;
        }

        @Override
        public Double calcularTotalFinal(Double totalBase, Integer parcelas) {
            BigDecimal base = new BigDecimal(totalBase);
            BigDecimal desconto = base.multiply(new BigDecimal("0.05"));
            BigDecimal total = base.subtract(desconto);
            return total.setScale(2, RoundingMode.HALF_EVEN).doubleValue();
        }

        @Override
        public Double calcularValorParcela(Double totalFinal, Integer parcelas) {
            return totalFinal;
        }
    },
    CARTAO {
        @Override
        public boolean validaParcelamento(Integer parcelas) {
            return parcelas >= 1 && parcelas <= 12;
        }

        @Override
        public boolean validaParaPedido(Double total) {
            return true;
        }

        @Override
        public Double calcularTotalFinal(Double totalBase, Integer parcelas) {
            if (parcelas <= 3) {
                return totalBase;
            }
            Double taxaMensal = 0.0199;
            Double parcela = totalBase * taxaMensal / (1.0 - Math.pow(1.0 + taxaMensal, -parcelas));
            BigDecimal parcelaBD = new BigDecimal(parcela);
            BigDecimal parcelaArredondada = parcelaBD.setScale(2, RoundingMode.HALF_EVEN);
            BigDecimal totalFinal = parcelaArredondada.multiply(new BigDecimal(parcelas));
            return totalFinal.doubleValue();
        }

        @Override
        public Double calcularValorParcela(Double totalFinal, Integer parcelas) {
            BigDecimal total = new BigDecimal(totalFinal);
            BigDecimal parcelaBD = total.divide(new BigDecimal(parcelas), 2, RoundingMode.HALF_EVEN);
            return parcelaBD.doubleValue();
        }
    },
    BOLETO {
        @Override
        public boolean validaParcelamento(Integer parcelas) {
            return parcelas == 1;
        }

        @Override
        public boolean validaParaPedido(Double total) {
            return total <= 1000.00;
        }

        @Override
        public Double calcularTotalFinal(Double totalBase, Integer parcelas) {
            BigDecimal base = new BigDecimal(totalBase);
            BigDecimal tarifa = new BigDecimal("3.49");
            BigDecimal total = base.add(tarifa);
            return total.setScale(2, RoundingMode.HALF_EVEN).doubleValue();
        }

        @Override
        public Double calcularValorParcela(Double totalFinal, Integer parcelas) {
            return totalFinal;
        }
    };

    public abstract boolean validaParcelamento(Integer parcelas);
    public abstract boolean validaParaPedido(Double total);
    public abstract Double calcularTotalFinal(Double totalBase, Integer parcelas);
    public abstract Double calcularValorParcela(Double totalFinal, Integer parcelas);

    public static FormaPagamento fromString(String value) {
        try {
            return FormaPagamento.valueOf(value);
        } catch (IllegalArgumentException e) {
            return null;
        }
    }
}

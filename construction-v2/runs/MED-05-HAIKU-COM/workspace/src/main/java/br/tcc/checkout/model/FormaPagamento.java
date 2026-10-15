package br.tcc.checkout.model;

import java.math.BigDecimal;
import java.math.RoundingMode;

public enum FormaPagamento {
    PIX {
        @Override
        public Double calcularAjuste(Double total, Integer parcelas) {
            return -(total * 0.05);
        }

        @Override
        public boolean ehParcelamentoValido(Integer parcelas) {
            return parcelas == 1;
        }
    },
    CARTAO {
        @Override
        public Double calcularAjuste(Double total, Integer parcelas) {
            if (parcelas <= 3) {
                return 0.0;
            }
            Double taxaMensal = 0.0199;
            Double numerador = total * taxaMensal;
            Double denominador = 1.0 - Math.pow(1.0 + taxaMensal, -parcelas);
            Double valorParcela = numerador / denominador;
            Double valorParcelaArredondado = arredondarMeioParaPar(valorParcela);
            return (valorParcelaArredondado * parcelas) - total;
        }

        @Override
        public boolean ehParcelamentoValido(Integer parcelas) {
            return parcelas >= 1 && parcelas <= 12;
        }
    },
    BOLETO {
        @Override
        public Double calcularAjuste(Double total, Integer parcelas) {
            return 3.49;
        }

        @Override
        public boolean ehParcelamentoValido(Integer parcelas) {
            return parcelas == 1;
        }

        @Override
        public boolean ehDisponivel(Double totalPedido) {
            return totalPedido <= 1000.00;
        }
    };

    public abstract Double calcularAjuste(Double total, Integer parcelas);

    public abstract boolean ehParcelamentoValido(Integer parcelas);

    public boolean ehDisponivel(Double totalPedido) {
        return true;
    }

    protected static Double arredondarMeioParaPar(Double valor) {
        BigDecimal bd = new BigDecimal(valor.toString());
        bd = bd.setScale(2, RoundingMode.HALF_EVEN);
        return bd.doubleValue();
    }
}


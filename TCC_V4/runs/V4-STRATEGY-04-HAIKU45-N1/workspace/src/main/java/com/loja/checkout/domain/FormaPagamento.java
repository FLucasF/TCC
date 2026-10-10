package com.loja.checkout.domain;

public enum FormaPagamento {
    PIX {
        @Override
        public Double calcularAjuste(Double total, Integer parcelas) {
            return -total * 0.05;
        }

        @Override
        public boolean permiteParcelas(Integer parcelas) {
            return parcelas == 1;
        }

        @Override
        public boolean verificaLimiteTotal(Double total) {
            return true;
        }
    },
    CARTAO {
        @Override
        public Double calcularAjuste(Double total, Integer parcelas) {
            if (parcelas <= 3) {
                return 0.0;
            }
            double taxaMensal = 0.0199;
            double parcela = total * taxaMensal / (1.0 - Math.pow(1.0 + taxaMensal, -parcelas));
            long parcelaRounded = Math.round(parcela * 100);
            double parcelaArredondada = parcelaRounded / 100.0;
            return (parcelaArredondada * parcelas) - total;
        }

        @Override
        public boolean permiteParcelas(Integer parcelas) {
            return parcelas >= 1 && parcelas <= 12;
        }

        @Override
        public boolean verificaLimiteTotal(Double total) {
            return true;
        }
    },
    BOLETO {
        @Override
        public Double calcularAjuste(Double total, Integer parcelas) {
            return 3.49;
        }

        @Override
        public boolean permiteParcelas(Integer parcelas) {
            return parcelas == 1;
        }

        @Override
        public boolean verificaLimiteTotal(Double total) {
            return total <= 1000.0;
        }
    };

    public abstract Double calcularAjuste(Double total, Integer parcelas);
    public abstract boolean permiteParcelas(Integer parcelas);
    public abstract boolean verificaLimiteTotal(Double total);

    public static FormaPagamento parse(String valor) {
        if (valor == null) return null;
        try {
            return FormaPagamento.valueOf(valor);
        } catch (IllegalArgumentException e) {
            return null;
        }
    }
}

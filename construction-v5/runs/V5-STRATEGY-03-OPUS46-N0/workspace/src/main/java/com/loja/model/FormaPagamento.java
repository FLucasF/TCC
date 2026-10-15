package com.loja.model;

public enum FormaPagamento {

    PIX {
        @Override
        public boolean isParcelamentoValido(int parcelas) {
            return parcelas == 1;
        }
    },

    CARTAO {
        @Override
        public boolean isParcelamentoValido(int parcelas) {
            return parcelas >= 1 && parcelas <= 12;
        }
    },

    BOLETO {
        @Override
        public boolean isParcelamentoValido(int parcelas) {
            return parcelas == 1;
        }
    };

    public abstract boolean isParcelamentoValido(int parcelas);
}

package com.loja.checkout.enums;

import java.math.BigDecimal;

public enum FormaPagamento {

    PIX {
        @Override
        public boolean parcelamentoValido(int parcelas) {
            return parcelas == 1;
        }

        @Override
        public boolean disponivel(BigDecimal totalPedido) {
            return true;
        }
    },
    BOLETO {
        private static final BigDecimal LIMITE_TOTAL = new BigDecimal("1000.00");

        @Override
        public boolean parcelamentoValido(int parcelas) {
            return parcelas == 1;
        }

        @Override
        public boolean disponivel(BigDecimal totalPedido) {
            return totalPedido.compareTo(LIMITE_TOTAL) <= 0;
        }
    },
    CARTAO {
        @Override
        public boolean parcelamentoValido(int parcelas) {
            return parcelas >= 1 && parcelas <= 12;
        }

        @Override
        public boolean disponivel(BigDecimal totalPedido) {
            return true;
        }
    };

    public abstract boolean parcelamentoValido(int parcelas);

    public abstract boolean disponivel(BigDecimal totalPedido);
}

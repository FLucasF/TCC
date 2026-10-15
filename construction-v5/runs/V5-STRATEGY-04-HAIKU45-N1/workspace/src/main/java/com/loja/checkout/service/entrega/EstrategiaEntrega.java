package com.loja.checkout.service.entrega;

import java.math.BigDecimal;

public sealed interface EstrategiaEntrega {
    InfoEntrega calcular(BigDecimal pesoTotal);

    boolean valida(BigDecimal pesoTotal);

    record Economica() implements EstrategiaEntrega {
        @Override
        public InfoEntrega calcular(BigDecimal pesoTotal) {
            BigDecimal frete = new BigDecimal("12.00")
                .add(pesoTotal.multiply(new BigDecimal("2.00")));
            return new InfoEntrega(frete, 7);
        }

        @Override
        public boolean valida(BigDecimal pesoTotal) {
            return true;
        }
    }

    record Expressa() implements EstrategiaEntrega {
        @Override
        public InfoEntrega calcular(BigDecimal pesoTotal) {
            BigDecimal frete = new BigDecimal("25.00")
                .add(pesoTotal.multiply(new BigDecimal("4.50")));
            return new InfoEntrega(frete, 2);
        }

        @Override
        public boolean valida(BigDecimal pesoTotal) {
            return true;
        }
    }

    record RetiraLoja() implements EstrategiaEntrega {
        @Override
        public InfoEntrega calcular(BigDecimal pesoTotal) {
            return new InfoEntrega(BigDecimal.ZERO, 1);
        }

        @Override
        public boolean valida(BigDecimal pesoTotal) {
            return true;
        }
    }

    record Motoboy() implements EstrategiaEntrega {
        private static final BigDecimal PESO_MAX = new BigDecimal("5.00");

        @Override
        public InfoEntrega calcular(BigDecimal pesoTotal) {
            return new InfoEntrega(new BigDecimal("18.00"), 0);
        }

        @Override
        public boolean valida(BigDecimal pesoTotal) {
            return pesoTotal.compareTo(PESO_MAX) <= 0;
        }
    }
}

package com.lojaroupas.enums;

public enum ModalidadeEntrega {
    ECONOMICA(12.00, 2.00, 7) {
        @Override
        public Double calcularFrete(Double pesoTotal) {
            return 12.00 + (2.00 * pesoTotal);
        }

        @Override
        public boolean validaParaPedido(Double pesoTotal) {
            return true;
        }
    },
    EXPRESSA(25.00, 4.50, 2) {
        @Override
        public Double calcularFrete(Double pesoTotal) {
            return 25.00 + (4.50 * pesoTotal);
        }

        @Override
        public boolean validaParaPedido(Double pesoTotal) {
            return true;
        }
    },
    RETIRADA_LOJA(0.00, 0.00, 1) {
        @Override
        public Double calcularFrete(Double pesoTotal) {
            return 0.00;
        }

        @Override
        public boolean validaParaPedido(Double pesoTotal) {
            return true;
        }
    },
    MOTOBOY(18.00, 0.00, 0) {
        @Override
        public Double calcularFrete(Double pesoTotal) {
            return 18.00;
        }

        @Override
        public boolean validaParaPedido(Double pesoTotal) {
            return pesoTotal <= 5.0;
        }
    };

    private final Double baseValue;
    private final Double perKgValue;
    private final Integer prazo;

    ModalidadeEntrega(Double baseValue, Double perKgValue, Integer prazo) {
        this.baseValue = baseValue;
        this.perKgValue = perKgValue;
        this.prazo = prazo;
    }

    public abstract Double calcularFrete(Double pesoTotal);
    public abstract boolean validaParaPedido(Double pesoTotal);

    public Integer getPrazo() {
        return prazo;
    }

    public static ModalidadeEntrega fromString(String value) {
        try {
            return ModalidadeEntrega.valueOf(value);
        } catch (IllegalArgumentException e) {
            return null;
        }
    }
}

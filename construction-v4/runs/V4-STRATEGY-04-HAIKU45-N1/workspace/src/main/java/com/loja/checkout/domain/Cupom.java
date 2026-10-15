package com.loja.checkout.domain;

import java.util.List;

public enum Cupom {
    BEMVINDO10("BEMVINDO10") {
        @Override
        public Double calcular(Double subtotalProdutos, Double pesoTotal, ModalidadeEntrega modalidade) {
            return subtotalProdutos * 0.10;
        }

        @Override
        public boolean validar(Double subtotalProdutos, Double pesoTotal, ModalidadeEntrega modalidade) {
            return true;
        }
    },
    MENOS50("MENOS50") {
        @Override
        public Double calcular(Double subtotalProdutos, Double pesoTotal, ModalidadeEntrega modalidade) {
            return 50.0;
        }

        @Override
        public boolean validar(Double subtotalProdutos, Double pesoTotal, ModalidadeEntrega modalidade) {
            return subtotalProdutos >= 300.0;
        }
    },
    FRETEGRATIS("FRETEGRATIS") {
        @Override
        public Double calcular(Double subtotalProdutos, Double pesoTotal, ModalidadeEntrega modalidade) {
            return modalidade.calcularFrete(pesoTotal);
        }

        @Override
        public boolean validar(Double subtotalProdutos, Double pesoTotal, ModalidadeEntrega modalidade) {
            return true;
        }

        @Override
        public boolean ehFretegratis() {
            return true;
        }
    },
    LEVE3PAGUE2("LEVE3PAGUE2") {
        @Override
        public Double calcular(Double subtotalProdutos, Double pesoTotal, ModalidadeEntrega modalidade) {
            // Este cupom tem lógica especial durante o cálculo do subtotal
            return 0.0;
        }

        @Override
        public boolean validar(Double subtotalProdutos, Double pesoTotal, ModalidadeEntrega modalidade) {
            return true;
        }
    };

    private final String codigo;

    Cupom(String codigo) {
        this.codigo = codigo;
    }

    public String getCodigo() {
        return codigo;
    }

    public abstract Double calcular(Double subtotalProdutos, Double pesoTotal, ModalidadeEntrega modalidade);
    public abstract boolean validar(Double subtotalProdutos, Double pesoTotal, ModalidadeEntrega modalidade);

    public boolean ehFretegratis() {
        return false;
    }

    public static Cupom parse(String valor) {
        if (valor == null) return null;
        try {
            return Cupom.valueOf(valor);
        } catch (IllegalArgumentException e) {
            return null;
        }
    }
}

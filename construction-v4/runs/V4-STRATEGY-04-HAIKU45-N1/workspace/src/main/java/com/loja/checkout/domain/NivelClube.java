package com.loja.checkout.domain;

public enum NivelClube {
    BRONZE {
        @Override
        public Double calcularCredito(Double subtotalProdutos) {
            return 0.0;
        }

        @Override
        public boolean isentaFrete() {
            return false;
        }

        @Override
        public boolean daBrinde(Double subtotalProdutos) {
            return false;
        }
    },
    PRATA {
        @Override
        public Double calcularCredito(Double subtotalProdutos) {
            return subtotalProdutos * 0.02;
        }

        @Override
        public boolean isentaFrete() {
            return false;
        }

        @Override
        public boolean daBrinde(Double subtotalProdutos) {
            return false;
        }
    },
    OURO {
        @Override
        public Double calcularCredito(Double subtotalProdutos) {
            return subtotalProdutos * 0.05;
        }

        @Override
        public boolean isentaFrete() {
            return true;
        }

        @Override
        public boolean daBrinde(Double subtotalProdutos) {
            return subtotalProdutos > 500.0;
        }
    };

    public abstract Double calcularCredito(Double subtotalProdutos);
    public abstract boolean isentaFrete();
    public abstract boolean daBrinde(Double subtotalProdutos);

    public static NivelClube parse(String valor) {
        if (valor == null) return null;
        try {
            return NivelClube.valueOf(valor);
        } catch (IllegalArgumentException e) {
            return null;
        }
    }
}

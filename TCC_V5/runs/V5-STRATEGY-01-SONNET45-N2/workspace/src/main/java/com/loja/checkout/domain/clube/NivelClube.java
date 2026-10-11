package com.loja.checkout.domain.clube;

public enum NivelClube {
    BRONZE {
        @Override
        public double calcularCredito(double valorProdutos) {
            return 0.0;
        }

        @Override
        public boolean isentaFrete() {
            return false;
        }

        @Override
        public boolean ganhaBrinde(double valorProdutos) {
            return false;
        }
    },

    PRATA {
        @Override
        public double calcularCredito(double valorProdutos) {
            return arredondar(valorProdutos * 0.02);
        }

        @Override
        public boolean isentaFrete() {
            return false;
        }

        @Override
        public boolean ganhaBrinde(double valorProdutos) {
            return false;
        }
    },

    OURO {
        @Override
        public double calcularCredito(double valorProdutos) {
            return arredondar(valorProdutos * 0.05);
        }

        @Override
        public boolean isentaFrete() {
            return true;
        }

        @Override
        public boolean ganhaBrinde(double valorProdutos) {
            return valorProdutos > 500.0;
        }
    };

    public abstract double calcularCredito(double valorProdutos);
    public abstract boolean isentaFrete();
    public abstract boolean ganhaBrinde(double valorProdutos);

    protected static double arredondar(double valor) {
        return Math.rint(valor * 100) / 100;
    }
}

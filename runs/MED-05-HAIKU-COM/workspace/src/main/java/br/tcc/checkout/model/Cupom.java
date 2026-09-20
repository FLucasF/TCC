package br.tcc.checkout.model;

import java.util.List;

public enum Cupom {
    BEMVINDO10 {
        @Override
        public Double calcularDesconto(Double subtotalProdutos, Double frete, List<Item> itens) {
            return subtotalProdutos * 0.10;
        }

        @Override
        public boolean ehAplicavel(Double subtotalProdutos, Double frete, List<Item> itens) {
            return true;
        }
    },
    MENOS50 {
        @Override
        public Double calcularDesconto(Double subtotalProdutos, Double frete, List<Item> itens) {
            return 50.00;
        }

        @Override
        public boolean ehAplicavel(Double subtotalProdutos, Double frete, List<Item> itens) {
            return subtotalProdutos >= 300.00;
        }
    },
    FRETEGRATIS {
        @Override
        public Double calcularDesconto(Double subtotalProdutos, Double frete, List<Item> itens) {
            return frete;
        }

        @Override
        public boolean ehAplicavel(Double subtotalProdutos, Double frete, List<Item> itens) {
            return true;
        }
    },
    LEVE3PAGUE2 {
        @Override
        public Double calcularDesconto(Double subtotalProdutos, Double frete, List<Item> itens) {
            Double desconto = 0.0;
            for (Item item : itens) {
                int unidadesGratis = item.getQuantidade() / 3;
                desconto += unidadesGratis * item.getPrecoUnitario();
            }
            return desconto;
        }

        @Override
        public boolean ehAplicavel(Double subtotalProdutos, Double frete, List<Item> itens) {
            return true;
        }
    };

    public abstract Double calcularDesconto(Double subtotalProdutos, Double frete, List<Item> itens);

    public abstract boolean ehAplicavel(Double subtotalProdutos, Double frete, List<Item> itens);
}

package com.loja.checkout.dominio;

import java.math.BigDecimal;

/**
 * Nivel do cliente no clube da loja. Cada nivel define as vantagens que da:
 * credito para a proxima compra, isencao de frete e brinde.
 */
public enum NivelClube {

    /** So o cadastro: fica com as vantagens vazias do nivel base. */
    BRONZE,

    /** Ganha 2% do valor dos produtos de volta em credito. */
    PRATA {
        @Override
        public BigDecimal creditoProximaCompra(BigDecimal subtotalProdutos) {
            return Dinheiro.percentual(subtotalProdutos, new BigDecimal("0.02"));
        }
    },

    /** Ganha 5% em credito, nao paga frete nunca e leva brinde acima de R$ 500,00. */
    OURO {
        @Override
        public BigDecimal creditoProximaCompra(BigDecimal subtotalProdutos) {
            return Dinheiro.percentual(subtotalProdutos, new BigDecimal("0.05"));
        }

        @Override
        public BigDecimal freteCobrado(BigDecimal freteDaModalidade) {
            return Dinheiro.ZERO;
        }

        @Override
        public boolean temBrinde(BigDecimal subtotalProdutos) {
            return subtotalProdutos.compareTo(new BigDecimal("500.00")) > 0;
        }
    };

    /** Credito guardado para a proxima compra, sobre os produtos sem desconto e sem frete. */
    public BigDecimal creditoProximaCompra(BigDecimal subtotalProdutos) {
        return Dinheiro.ZERO;
    }

    /** Quanto do frete da modalidade o cliente desse nivel paga. */
    public BigDecimal freteCobrado(BigDecimal freteDaModalidade) {
        return freteDaModalidade;
    }

    /** Se a loja manda um brinde junto com o pedido. */
    public boolean temBrinde(BigDecimal subtotalProdutos) {
        return false;
    }
}

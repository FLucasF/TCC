package com.loja.checkout.dominio;

import java.math.BigDecimal;

/** Cada cupom diz quando pode ser usado e quanto desconta. */
public enum Cupom {

    BEMVINDO10 {
        private static final BigDecimal PERCENTUAL = new BigDecimal("0.10");

        @Override
        public BigDecimal desconto(Pedido pedido, BigDecimal frete) {
            return Dinheiro.emCentavos(pedido.subtotalProdutos().multiply(PERCENTUAL));
        }
    },

    MENOS50 {
        private static final BigDecimal MINIMO_EM_PRODUTOS = new BigDecimal("300.00");
        private static final BigDecimal VALOR = new BigDecimal("50.00");

        @Override
        public boolean aplicavel(Pedido pedido) {
            return pedido.subtotalProdutos().compareTo(MINIMO_EM_PRODUTOS) >= 0;
        }

        @Override
        public BigDecimal desconto(Pedido pedido, BigDecimal frete) {
            return Dinheiro.emCentavos(VALOR);
        }
    },

    FRETEGRATIS {
        @Override
        public BigDecimal desconto(Pedido pedido, BigDecimal frete) {
            return Dinheiro.emCentavos(frete);
        }
    },

    LEVE3PAGUE2 {
        @Override
        public BigDecimal desconto(Pedido pedido, BigDecimal frete) {
            BigDecimal gratuitos = pedido.itens().stream()
                    .map(item -> item.precoUnitario().multiply(BigDecimal.valueOf(item.quantidade() / 3)))
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
            return Dinheiro.emCentavos(gratuitos);
        }
    };

    public static Cupom de(String codigo) {
        return Catalogo.resolver(Cupom.class, codigo, CodigoErro.CUPOM_INVALIDO);
    }

    public boolean aplicavel(Pedido pedido) {
        return true;
    }

    public abstract BigDecimal desconto(Pedido pedido, BigDecimal frete);
}

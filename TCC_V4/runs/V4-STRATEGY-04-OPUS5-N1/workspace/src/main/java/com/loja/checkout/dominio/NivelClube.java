package com.loja.checkout.dominio;

import java.math.BigDecimal;
import java.util.List;

/** Nivel do cliente no clube da loja: cada nivel tem seu conjunto de vantagens. */
public enum NivelClube {

    BRONZE {
        @Override
        public BigDecimal creditoProximaCompra(BigDecimal subtotalProdutos) {
            return Centavos.ZERO;
        }

        @Override
        public BigDecimal fretePago(BigDecimal frete) {
            return Centavos.arredondar(frete);
        }

        @Override
        public boolean temBrinde(BigDecimal subtotalProdutos) {
            return false;
        }
    },

    PRATA {
        @Override
        public BigDecimal creditoProximaCompra(BigDecimal subtotalProdutos) {
            return Centavos.percentual(subtotalProdutos, new BigDecimal("0.02"));
        }

        @Override
        public BigDecimal fretePago(BigDecimal frete) {
            return Centavos.arredondar(frete);
        }

        @Override
        public boolean temBrinde(BigDecimal subtotalProdutos) {
            return false;
        }
    },

    OURO {
        @Override
        public BigDecimal creditoProximaCompra(BigDecimal subtotalProdutos) {
            return Centavos.percentual(subtotalProdutos, new BigDecimal("0.05"));
        }

        /** OURO nao paga frete nunca. */
        @Override
        public BigDecimal fretePago(BigDecimal frete) {
            return Centavos.ZERO;
        }

        @Override
        public boolean temBrinde(BigDecimal subtotalProdutos) {
            return subtotalProdutos.compareTo(new BigDecimal("500.00")) > 0;
        }
    };

    /** Credito guardado para a proxima compra, sobre o valor dos produtos. */
    public abstract BigDecimal creditoProximaCompra(BigDecimal subtotalProdutos);

    /** Quanto o cliente desse nivel paga do frete cobrado pela entrega. */
    public abstract BigDecimal fretePago(BigDecimal frete);

    /** Se o pedido vai com brinde. */
    public abstract boolean temBrinde(BigDecimal subtotalProdutos);

    public static final Catalogo<NivelClube> CATALOGO =
            Catalogo.de(CodigoErro.NIVEL_CLUBE_INVALIDO, NivelClube::name, List.of(values()));
}

package com.loja.checkout.dominio.clube;

import com.loja.checkout.dominio.Codigos;
import com.loja.checkout.dominio.Dinheiro;

import java.math.BigDecimal;
import java.util.Optional;

/** O nivel do cliente no clube da loja e as vantagens que ele da. */
public enum NivelClube {

    /** So o cadastro, nao ganha nada. */
    BRONZE {
        @Override
        public BigDecimal creditoProximaCompra(BigDecimal subtotalProdutos) {
            return Dinheiro.ZERO;
        }
    },

    /** Ganha 2% dos produtos de volta em credito. */
    PRATA {
        @Override
        public BigDecimal creditoProximaCompra(BigDecimal subtotalProdutos) {
            return Dinheiro.percentual(subtotalProdutos, new BigDecimal("0.02"));
        }
    },

    /** Ganha 5% em credito, nao paga frete nunca e ganha brinde acima de R$ 500,00. */
    OURO {
        @Override
        public BigDecimal creditoProximaCompra(BigDecimal subtotalProdutos) {
            return Dinheiro.percentual(subtotalProdutos, new BigDecimal("0.05"));
        }

        @Override
        public BigDecimal freteCobrado(BigDecimal frete) {
            return Dinheiro.ZERO;
        }

        @Override
        public boolean temBrinde(BigDecimal subtotalProdutos) {
            return subtotalProdutos.compareTo(new BigDecimal("500.00")) > 0;
        }
    };

    public abstract BigDecimal creditoProximaCompra(BigDecimal subtotalProdutos);

    public BigDecimal freteCobrado(BigDecimal frete) {
        return frete;
    }

    public boolean temBrinde(BigDecimal subtotalProdutos) {
        return false;
    }

    public static Optional<NivelClube> porCodigo(String codigo) {
        return Codigos.buscar(NivelClube.class, codigo);
    }
}

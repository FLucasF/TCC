package com.loja.checkout.service.clube;

import java.math.BigDecimal;

import com.loja.checkout.service.Arredondamento;

public sealed interface EstrategiaClube {
    ResultadoClube aplicar(BigDecimal subtotal);

    record Bronze() implements EstrategiaClube {
        @Override
        public ResultadoClube aplicar(BigDecimal subtotal) {
            return new ResultadoClube(BigDecimal.ZERO, BigDecimal.ZERO, false);
        }
    }

    record Prata() implements EstrategiaClube {
        @Override
        public ResultadoClube aplicar(BigDecimal subtotal) {
            BigDecimal credito = Arredondamento.arredondar(subtotal.multiply(new BigDecimal("0.02")));
            return new ResultadoClube(BigDecimal.ZERO, credito, false);
        }
    }

    record Ouro() implements EstrategiaClube {
        private static final BigDecimal MINIMO_BRINDE = new BigDecimal("500.00");

        @Override
        public ResultadoClube aplicar(BigDecimal subtotal) {
            BigDecimal credito = Arredondamento.arredondar(subtotal.multiply(new BigDecimal("0.05")));
            boolean brinde = subtotal.compareTo(MINIMO_BRINDE) > 0;
            return new ResultadoClube(BigDecimal.ZERO, credito, brinde);
        }
    }
}

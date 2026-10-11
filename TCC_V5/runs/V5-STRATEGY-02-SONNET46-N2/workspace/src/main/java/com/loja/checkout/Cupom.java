package com.loja.checkout;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

public enum Cupom {
    BEMVINDO10 {
        @Override
        public BigDecimal calcularDesconto(BigDecimal subtotal, BigDecimal frete, List<PedidoRequest.Item> itens) {
            return arr(subtotal.multiply(new BigDecimal("0.10")));
        }
        @Override
        public void verificarAplicabilidade(BigDecimal subtotal) {}
    },
    MENOS50 {
        private static final BigDecimal MINIMO = new BigDecimal("300.00");

        @Override
        public BigDecimal calcularDesconto(BigDecimal subtotal, BigDecimal frete, List<PedidoRequest.Item> itens) {
            return new BigDecimal("50.00");
        }
        @Override
        public void verificarAplicabilidade(BigDecimal subtotal) {
            if (subtotal.compareTo(MINIMO) < 0) throw new CheckoutException("CUPOM_NAO_APLICAVEL");
        }
    },
    FRETEGRATIS {
        @Override
        public BigDecimal calcularDesconto(BigDecimal subtotal, BigDecimal frete, List<PedidoRequest.Item> itens) {
            return frete;
        }
        @Override
        public void verificarAplicabilidade(BigDecimal subtotal) {}
    },
    LEVE3PAGUE2 {
        @Override
        public BigDecimal calcularDesconto(BigDecimal subtotal, BigDecimal frete, List<PedidoRequest.Item> itens) {
            BigDecimal total = BigDecimal.ZERO;
            for (var item : itens) {
                int gratis = item.quantidade() / 3;
                if (gratis > 0) {
                    total = total.add(arr(item.precoUnitario().multiply(BigDecimal.valueOf(gratis))));
                }
            }
            return total;
        }
        @Override
        public void verificarAplicabilidade(BigDecimal subtotal) {}
    };

    public abstract BigDecimal calcularDesconto(BigDecimal subtotal, BigDecimal frete, List<PedidoRequest.Item> itens);
    public abstract void verificarAplicabilidade(BigDecimal subtotal);

    private static BigDecimal arr(BigDecimal v) {
        return v.setScale(2, RoundingMode.HALF_EVEN);
    }
}

package com.loja.checkout.domain;

import java.math.BigDecimal;
import java.util.List;

public interface Cupom {
    String getCodigo();
    boolean ehValido(BigDecimal subtotal, List<ItemPedido> itens);
    BigDecimal calcularDesconto(BigDecimal subtotal, List<ItemPedido> itens);
    BigDecimal calcularDescontoNoFrete(BigDecimal frete);

    static Cupom porCodigo(String codigo) {
        if (codigo == null) {
            return null;
        }
        return switch (codigo) {
            case "BEMVINDO10" -> new CupomBemVindo10();
            case "MENOS50" -> new CupomMenos50();
            case "FRETEGRATIS" -> new CupomFreteGratis();
            case "LEVE3PAGUE2" -> new CupomLeve3Pague2();
            default -> null;
        };
    }

    class CupomBemVindo10 implements Cupom {
        @Override
        public String getCodigo() {
            return "BEMVINDO10";
        }

        @Override
        public boolean ehValido(BigDecimal subtotal, List<ItemPedido> itens) {
            return true;
        }

        @Override
        public BigDecimal calcularDesconto(BigDecimal subtotal, List<ItemPedido> itens) {
            return subtotal.multiply(BigDecimal.valueOf(0.10));
        }

        @Override
        public BigDecimal calcularDescontoNoFrete(BigDecimal frete) {
            return BigDecimal.ZERO;
        }
    }

    class CupomMenos50 implements Cupom {
        @Override
        public String getCodigo() {
            return "MENOS50";
        }

        @Override
        public boolean ehValido(BigDecimal subtotal, List<ItemPedido> itens) {
            return subtotal.compareTo(BigDecimal.valueOf(300)) >= 0;
        }

        @Override
        public BigDecimal calcularDesconto(BigDecimal subtotal, List<ItemPedido> itens) {
            return BigDecimal.valueOf(50);
        }

        @Override
        public BigDecimal calcularDescontoNoFrete(BigDecimal frete) {
            return BigDecimal.ZERO;
        }
    }

    class CupomFreteGratis implements Cupom {
        @Override
        public String getCodigo() {
            return "FRETEGRATIS";
        }

        @Override
        public boolean ehValido(BigDecimal subtotal, List<ItemPedido> itens) {
            return true;
        }

        @Override
        public BigDecimal calcularDesconto(BigDecimal subtotal, List<ItemPedido> itens) {
            return BigDecimal.ZERO;
        }

        @Override
        public BigDecimal calcularDescontoNoFrete(BigDecimal frete) {
            return frete;
        }
    }

    class CupomLeve3Pague2 implements Cupom {
        @Override
        public String getCodigo() {
            return "LEVE3PAGUE2";
        }

        @Override
        public boolean ehValido(BigDecimal subtotal, List<ItemPedido> itens) {
            return true;
        }

        @Override
        public BigDecimal calcularDesconto(BigDecimal subtotal, List<ItemPedido> itens) {
            BigDecimal desconto = BigDecimal.ZERO;
            for (ItemPedido item : itens) {
                int unidadesGratis = item.quantidade() / 3;
                desconto = desconto.add(item.precoUnitario().multiply(BigDecimal.valueOf(unidadesGratis)));
            }
            return desconto;
        }

        @Override
        public BigDecimal calcularDescontoNoFrete(BigDecimal frete) {
            return BigDecimal.ZERO;
        }
    }
}

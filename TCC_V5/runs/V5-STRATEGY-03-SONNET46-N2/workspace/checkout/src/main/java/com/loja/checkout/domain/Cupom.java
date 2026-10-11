package com.loja.checkout.domain;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public sealed interface Cupom permits Cupom.BemVindo10, Cupom.Menos50, Cupom.FreteGratis, Cupom.Leve3Pague2 {

    boolean aplicavel(BigDecimal subtotalProdutos, BigDecimal frete);

    BigDecimal calcularDesconto(BigDecimal subtotalProdutos, BigDecimal frete, List<ItemCarrinho> itens);

    record ItemCarrinho(BigDecimal precoUnitario, int quantidade) {}

    record BemVindo10() implements Cupom {
        @Override
        public boolean aplicavel(BigDecimal subtotalProdutos, BigDecimal frete) {
            return true;
        }

        @Override
        public BigDecimal calcularDesconto(BigDecimal subtotalProdutos, BigDecimal frete, List<ItemCarrinho> itens) {
            return subtotalProdutos.multiply(new BigDecimal("0.10")).setScale(2, RoundingMode.HALF_EVEN);
        }
    }

    record Menos50() implements Cupom {
        private static final BigDecimal MINIMO = new BigDecimal("300.00");

        @Override
        public boolean aplicavel(BigDecimal subtotalProdutos, BigDecimal frete) {
            return subtotalProdutos.compareTo(MINIMO) >= 0;
        }

        @Override
        public BigDecimal calcularDesconto(BigDecimal subtotalProdutos, BigDecimal frete, List<ItemCarrinho> itens) {
            return new BigDecimal("50.00");
        }
    }

    record FreteGratis() implements Cupom {
        @Override
        public boolean aplicavel(BigDecimal subtotalProdutos, BigDecimal frete) {
            return true;
        }

        @Override
        public BigDecimal calcularDesconto(BigDecimal subtotalProdutos, BigDecimal frete, List<ItemCarrinho> itens) {
            return frete;
        }
    }

    record Leve3Pague2() implements Cupom {
        @Override
        public boolean aplicavel(BigDecimal subtotalProdutos, BigDecimal frete) {
            return true;
        }

        @Override
        public BigDecimal calcularDesconto(BigDecimal subtotalProdutos, BigDecimal frete, List<ItemCarrinho> itens) {
            return itens.stream()
                    .map(item -> item.precoUnitario().multiply(BigDecimal.valueOf(item.quantidade() / 3)))
                    .reduce(BigDecimal.ZERO, BigDecimal::add)
                    .setScale(2, RoundingMode.HALF_EVEN);
        }
    }

    Map<String, Cupom> REGISTRO = Map.of(
            "BEMVINDO10", new BemVindo10(),
            "MENOS50", new Menos50(),
            "FRETEGRATIS", new FreteGratis(),
            "LEVE3PAGUE2", new Leve3Pague2()
    );

    static Optional<Cupom> buscar(String codigo) {
        if (codigo == null) return Optional.empty();
        return Optional.ofNullable(REGISTRO.get(codigo));
    }
}

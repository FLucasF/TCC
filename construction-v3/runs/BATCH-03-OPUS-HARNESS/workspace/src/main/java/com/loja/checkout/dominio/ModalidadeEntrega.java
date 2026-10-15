package com.loja.checkout.dominio;

import java.math.BigDecimal;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/** Cada opcao de entrega define seu frete, seu prazo e suas limitacoes. */
public enum ModalidadeEntrega {

    ECONOMICA(7) {
        @Override
        public BigDecimal frete(Pedido pedido) {
            return Dinheiro.centavos(new BigDecimal("12.00")
                    .add(new BigDecimal("2.00").multiply(pedido.pesoKg())));
        }
    },

    EXPRESSA(2) {
        @Override
        public BigDecimal frete(Pedido pedido) {
            return Dinheiro.centavos(new BigDecimal("25.00")
                    .add(new BigDecimal("4.50").multiply(pedido.pesoKg())));
        }
    },

    RETIRADA_LOJA(1) {
        @Override
        public BigDecimal frete(Pedido pedido) {
            return Dinheiro.ZERO;
        }
    },

    MOTOBOY(0) {
        @Override
        public BigDecimal frete(Pedido pedido) {
            return Dinheiro.centavos(new BigDecimal("18.00"));
        }

        @Override
        public boolean atende(Pedido pedido) {
            return pedido.pesoKg().compareTo(PESO_MAXIMO_MOTOBOY) <= 0;
        }
    };

    private static final BigDecimal PESO_MAXIMO_MOTOBOY = new BigDecimal("5");

    private static final Map<String, ModalidadeEntrega> POR_CODIGO = Stream.of(values())
            .collect(Collectors.toMap(Enum::name, Function.identity()));

    private final int prazoEntregaDias;

    ModalidadeEntrega(int prazoEntregaDias) {
        this.prazoEntregaDias = prazoEntregaDias;
    }

    public static Optional<ModalidadeEntrega> porCodigo(String codigo) {
        return Optional.ofNullable(codigo).map(POR_CODIGO::get);
    }

    public abstract BigDecimal frete(Pedido pedido);

    public boolean atende(Pedido pedido) {
        return true;
    }

    public int prazoEntregaDias() {
        return prazoEntregaDias;
    }
}

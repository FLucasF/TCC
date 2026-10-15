package com.loja.checkout.dominio;

import com.loja.checkout.erro.Codigo;
import com.loja.checkout.erro.PedidoRecusado;

import java.math.BigDecimal;

/**
 * Como o cliente quer receber o pedido. Cada modalidade define o proprio jeito
 * de cobrar o frete, o proprio prazo e as proprias limitacoes.
 */
public enum ModalidadeEntrega {

    /** R$ 12,00 mais R$ 2,00 por kg do pedido. */
    ECONOMICA(7) {
        @Override
        public BigDecimal frete(Pedido pedido) {
            return porKg("12.00", "2.00", pedido);
        }
    },

    /** R$ 25,00 mais R$ 4,50 por kg do pedido. */
    EXPRESSA(2) {
        @Override
        public BigDecimal frete(Pedido pedido) {
            return porKg("25.00", "4.50", pedido);
        }
    },

    /** O cliente busca na loja, nao paga frete. */
    RETIRADA_LOJA(1) {
        @Override
        public BigDecimal frete(Pedido pedido) {
            return Dinheiro.ZERO;
        }
    },

    /** Valor fixo de R$ 18,00, e so leva pedidos de ate 5 kg. */
    MOTOBOY(0) {
        private static final BigDecimal PESO_MAXIMO_KG = new BigDecimal("5");

        @Override
        public BigDecimal frete(Pedido pedido) {
            return Dinheiro.centavos(new BigDecimal("18.00"));
        }

        @Override
        public boolean atende(Pedido pedido) {
            return pedido.pesoKg().compareTo(PESO_MAXIMO_KG) <= 0;
        }
    };

    private final int prazoEntregaDias;

    ModalidadeEntrega(int prazoEntregaDias) {
        this.prazoEntregaDias = prazoEntregaDias;
    }

    /** O frete que essa modalidade cobra pelo pedido. */
    public abstract BigDecimal frete(Pedido pedido);

    /** Se essa modalidade atende o pedido; por padrao atende qualquer um. */
    public boolean atende(Pedido pedido) {
        return true;
    }

    public int prazoEntregaDias() {
        return prazoEntregaDias;
    }

    public void exigirQueAtenda(Pedido pedido) {
        if (!atende(pedido)) {
            throw new PedidoRecusado(Codigo.MODALIDADE_INDISPONIVEL);
        }
    }

    private static BigDecimal porKg(String fixo, String porKg, Pedido pedido) {
        return Dinheiro.centavos(new BigDecimal(fixo).add(new BigDecimal(porKg).multiply(pedido.pesoKg())));
    }
}

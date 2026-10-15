package com.loja.checkout.dominio;

import com.loja.checkout.erro.Codigo;
import com.loja.checkout.erro.PedidoRecusado;

import java.math.BigDecimal;
import java.util.Optional;

/**
 * As promocoes que valem hoje, uma por pedido. Cada cupom define o proprio
 * desconto e a propria condicao para valer.
 */
public enum Cupom {

    /** 10% de desconto no valor dos produtos. */
    BEMVINDO10 {
        @Override
        public BigDecimal desconto(Pedido pedido, BigDecimal frete) {
            return Dinheiro.percentual(pedido.subtotalProdutos(), new BigDecimal("0.10"));
        }
    },

    /** R$ 50,00 nos produtos, a partir de R$ 300,00 em produtos. */
    MENOS50 {
        private static final BigDecimal MINIMO_EM_PRODUTOS = new BigDecimal("300.00");

        @Override
        public BigDecimal desconto(Pedido pedido, BigDecimal frete) {
            return Dinheiro.centavos(new BigDecimal("50.00"));
        }

        @Override
        public boolean seAplica(Pedido pedido) {
            return pedido.subtotalProdutos().compareTo(MINIMO_EM_PRODUTOS) >= 0;
        }
    },

    /** O cliente nao paga o frete: o desconto fica igual ao valor do frete. */
    FRETEGRATIS {
        @Override
        public BigDecimal desconto(Pedido pedido, BigDecimal frete) {
            return Dinheiro.centavos(frete);
        }
    },

    /** A cada 3 unidades de um mesmo item, uma sai de graca. */
    LEVE3PAGUE2 {
        private static final int UNIDADES_PARA_GANHAR_UMA = 3;

        @Override
        public BigDecimal desconto(Pedido pedido, BigDecimal frete) {
            return Dinheiro.centavos(pedido.itens().stream()
                    .map(item -> unidadesDeGraca(item))
                    .reduce(BigDecimal.ZERO, BigDecimal::add));
        }

        private static BigDecimal unidadesDeGraca(Item item) {
            long gratuitas = item.quantidade() / UNIDADES_PARA_GANHAR_UMA;
            return item.precoUnitario().multiply(BigDecimal.valueOf(gratuitas));
        }
    };

    /** Quanto esse cupom desconta do pedido. */
    public abstract BigDecimal desconto(Pedido pedido, BigDecimal frete);

    /** Se o pedido cumpre a condicao do cupom; por padrao nao tem condicao. */
    public boolean seAplica(Pedido pedido) {
        return true;
    }

    public void exigirQueSeAplique(Pedido pedido) {
        if (!seAplica(pedido)) {
            throw new PedidoRecusado(Codigo.CUPOM_NAO_APLICAVEL);
        }
    }

    /** O cupom que o cliente informou, quando informou algum. */
    public static Optional<Cupom> informado(String codigo) {
        return Optional.ofNullable(codigo)
                .map(informado -> Opcao.exigir(Cupom.class, informado, Codigo.CUPOM_INVALIDO));
    }
}

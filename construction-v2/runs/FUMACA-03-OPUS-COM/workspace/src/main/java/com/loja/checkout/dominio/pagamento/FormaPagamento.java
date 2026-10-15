package com.loja.checkout.dominio.pagamento;

import com.loja.checkout.dominio.Dinheiro;
import java.math.BigDecimal;
import java.util.Optional;

/**
 * Formas de pagamento aceitas. Cada uma define sozinha quantas parcelas aceita,
 * quando não atende o pedido e como o total do pedido vira o valor final.
 */
public enum FormaPagamento {

    PIX {
        @Override
        public Cobranca cobranca(BigDecimal totalPedido, int parcelas) {
            BigDecimal desconto = Dinheiro.percentual(totalPedido, "0.05");
            return aVista(totalPedido.subtract(desconto));
        }
    },

    BOLETO {
        private static final BigDecimal TARIFA = new BigDecimal("3.49");
        private static final BigDecimal TETO = new BigDecimal("1000.00");

        @Override
        public boolean atende(BigDecimal totalPedido) {
            return totalPedido.compareTo(TETO) <= 0;
        }

        @Override
        public Cobranca cobranca(BigDecimal totalPedido, int parcelas) {
            return aVista(totalPedido.add(TARIFA));
        }
    },

    CARTAO {
        private static final int MAXIMO_PARCELAS = 12;
        private static final int PARCELAS_SEM_JUROS = 3;
        private static final BigDecimal TAXA_MENSAL = new BigDecimal("0.0199");

        @Override
        public boolean parcelasPermitidas(int parcelas) {
            return parcelas >= 1 && parcelas <= MAXIMO_PARCELAS;
        }

        @Override
        public Cobranca cobranca(BigDecimal totalPedido, int parcelas) {
            if (parcelas <= PARCELAS_SEM_JUROS) {
                BigDecimal parcela = totalPedido.divide(BigDecimal.valueOf(parcelas), Dinheiro.CALCULO);
                return new Cobranca(Dinheiro.valor(totalPedido), Dinheiro.valor(parcela));
            }
            BigDecimal parcela = Dinheiro.valor(parcelaPrice(totalPedido, parcelas));
            return new Cobranca(Dinheiro.valor(parcela.multiply(BigDecimal.valueOf(parcelas))), parcela);
        }

        /** Tabela Price: total x taxa / (1 - (1 + taxa)^-parcelas). */
        private BigDecimal parcelaPrice(BigDecimal totalPedido, int parcelas) {
            BigDecimal fator = BigDecimal.ONE.add(TAXA_MENSAL).pow(parcelas);
            BigDecimal divisor = BigDecimal.ONE.subtract(BigDecimal.ONE.divide(fator, Dinheiro.CALCULO));
            return totalPedido.multiply(TAXA_MENSAL).divide(divisor, Dinheiro.CALCULO);
        }
    };

    public static Optional<FormaPagamento> porCodigo(String codigo) {
        if (codigo == null) {
            return Optional.empty();
        }
        for (FormaPagamento forma : values()) {
            if (forma.name().equals(codigo)) {
                return Optional.of(forma);
            }
        }
        return Optional.empty();
    }

    public abstract Cobranca cobranca(BigDecimal totalPedido, int parcelas);

    /** Por padrão a forma é sempre à vista; quem parcela sobrescreve. */
    public boolean parcelasPermitidas(int parcelas) {
        return parcelas == 1;
    }

    /** Por padrão a forma atende qualquer pedido; quem tem limite sobrescreve. */
    public boolean atende(BigDecimal totalPedido) {
        return true;
    }

    static Cobranca aVista(BigDecimal totalFinal) {
        BigDecimal valor = Dinheiro.valor(totalFinal);
        return new Cobranca(valor, valor);
    }
}

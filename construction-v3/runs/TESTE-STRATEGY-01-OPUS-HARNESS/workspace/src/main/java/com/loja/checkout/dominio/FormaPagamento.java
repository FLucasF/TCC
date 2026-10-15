package com.loja.checkout.dominio;

import java.math.BigDecimal;

/**
 * Cada forma de pagamento guarda no seu proprio corpo o parcelamento que aceita,
 * os pedidos que atende e o ajuste que faz sobre o total.
 */
public enum FormaPagamento {

    PIX {
        private static final BigDecimal DESCONTO = new BigDecimal("0.05");

        @Override
        public boolean permite(int parcelas) {
            return parcelas == 1;
        }

        @Override
        public Cobranca cobrar(BigDecimal totalPedido, int parcelas) {
            BigDecimal totalFinal = totalPedido.subtract(Dinheiro.percentual(totalPedido, DESCONTO));
            return aVista(totalFinal);
        }
    },

    BOLETO {
        private static final BigDecimal TARIFA = new BigDecimal("3.49");
        private static final BigDecimal TETO = new BigDecimal("1000.00");

        @Override
        public boolean permite(int parcelas) {
            return parcelas == 1;
        }

        @Override
        public boolean disponivel(BigDecimal totalPedidoSemImposto) {
            return totalPedidoSemImposto.compareTo(TETO) <= 0;
        }

        @Override
        public Cobranca cobrar(BigDecimal totalPedido, int parcelas) {
            return aVista(totalPedido.add(TARIFA));
        }
    },

    CARTAO {
        private static final int MAXIMO_PARCELAS = 12;
        private static final int PARCELAS_SEM_JUROS = 3;
        private static final BigDecimal JUROS_MENSAL = new BigDecimal("0.0199");

        @Override
        public boolean permite(int parcelas) {
            return parcelas >= 1 && parcelas <= MAXIMO_PARCELAS;
        }

        @Override
        public Cobranca cobrar(BigDecimal totalPedido, int parcelas) {
            if (parcelas <= PARCELAS_SEM_JUROS) {
                return new Cobranca(Dinheiro.centavos(totalPedido), parcelas, dividir(totalPedido, parcelas));
            }
            BigDecimal parcela = Dinheiro.centavos(price(totalPedido, JUROS_MENSAL, parcelas));
            return new Cobranca(Dinheiro.centavos(parcela.multiply(BigDecimal.valueOf(parcelas))), parcelas, parcela);
        }

        /** Tabela Price: total x taxa / (1 - (1 + taxa)^-parcelas). */
        private BigDecimal price(BigDecimal total, BigDecimal taxa, int parcelas) {
            BigDecimal acumulado = BigDecimal.ONE.add(taxa).pow(parcelas, Dinheiro.CALCULO);
            BigDecimal fator = BigDecimal.ONE.subtract(BigDecimal.ONE.divide(acumulado, Dinheiro.CALCULO));
            return total.multiply(taxa).divide(fator, Dinheiro.CALCULO);
        }
    };

    public abstract boolean permite(int parcelas);

    public abstract Cobranca cobrar(BigDecimal totalPedido, int parcelas);

    public boolean disponivel(BigDecimal totalPedidoSemImposto) {
        return true;
    }

    protected static Cobranca aVista(BigDecimal totalFinal) {
        BigDecimal valor = Dinheiro.centavos(totalFinal);
        return new Cobranca(valor, 1, valor);
    }

    protected static BigDecimal dividir(BigDecimal total, int parcelas) {
        return Dinheiro.centavos(total.divide(BigDecimal.valueOf(parcelas), Dinheiro.CALCULO));
    }
}

package com.loja.checkout.dominio;

import java.math.BigDecimal;
import java.math.MathContext;

/**
 * Cada forma de pagamento diz em quantas vezes aceita ser paga, se atende o
 * pedido e quanto o cliente paga no fim.
 */
public enum FormaPagamento {

    PIX(1) {
        private static final BigDecimal DESCONTO = new BigDecimal("0.05");

        @Override
        public ResultadoPagamento cobranca(BigDecimal totalPedido, int parcelas) {
            BigDecimal totalFinal = Dinheiro.emCentavos(
                    totalPedido.subtract(Dinheiro.emCentavos(totalPedido.multiply(DESCONTO))));
            return new ResultadoPagamento(totalFinal, totalFinal);
        }
    },

    CARTAO(12) {
        private static final int PARCELAS_SEM_JUROS = 3;
        private static final BigDecimal TAXA_MENSAL = new BigDecimal("0.0199");

        @Override
        public ResultadoPagamento cobranca(BigDecimal totalPedido, int parcelas) {
            if (parcelas <= PARCELAS_SEM_JUROS) {
                BigDecimal valorParcela = Dinheiro.emCentavos(
                        totalPedido.divide(BigDecimal.valueOf(parcelas), MathContext.DECIMAL64));
                return new ResultadoPagamento(Dinheiro.emCentavos(totalPedido), valorParcela);
            }
            BigDecimal valorParcela = Dinheiro.emCentavos(parcelaPrice(totalPedido, parcelas));
            return new ResultadoPagamento(
                    Dinheiro.emCentavos(valorParcela.multiply(BigDecimal.valueOf(parcelas))), valorParcela);
        }

        private BigDecimal parcelaPrice(BigDecimal totalPedido, int parcelas) {
            BigDecimal fator = BigDecimal.ONE.add(TAXA_MENSAL).pow(parcelas, MathContext.DECIMAL64);
            BigDecimal divisor = BigDecimal.ONE.subtract(BigDecimal.ONE.divide(fator, MathContext.DECIMAL64));
            return totalPedido.multiply(TAXA_MENSAL).divide(divisor, MathContext.DECIMAL64);
        }
    },

    BOLETO(1) {
        private static final BigDecimal TARIFA = new BigDecimal("3.49");
        private static final BigDecimal TOTAL_MAXIMO = new BigDecimal("1000.00");

        @Override
        public boolean atende(BigDecimal totalPedido) {
            return totalPedido.compareTo(TOTAL_MAXIMO) <= 0;
        }

        @Override
        public ResultadoPagamento cobranca(BigDecimal totalPedido, int parcelas) {
            BigDecimal totalFinal = Dinheiro.emCentavos(totalPedido.add(TARIFA));
            return new ResultadoPagamento(totalFinal, totalFinal);
        }
    };

    private final int maximoDeParcelas;

    FormaPagamento(int maximoDeParcelas) {
        this.maximoDeParcelas = maximoDeParcelas;
    }

    public static FormaPagamento de(String codigo) {
        return Catalogo.resolver(FormaPagamento.class, codigo, CodigoErro.FORMA_PAGAMENTO_INVALIDA);
    }

    public boolean aceitaParcelas(int parcelas) {
        return parcelas >= 1 && parcelas <= maximoDeParcelas;
    }

    public boolean atende(BigDecimal totalPedido) {
        return true;
    }

    public abstract ResultadoPagamento cobranca(BigDecimal totalPedido, int parcelas);
}

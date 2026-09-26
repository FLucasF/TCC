package com.loja.checkout.dominio.pagamento;

import com.loja.checkout.dominio.Codigos;
import com.loja.checkout.dominio.Dinheiro;

import java.math.BigDecimal;
import java.math.MathContext;
import java.util.Optional;

/** Como o cliente paga, e o ajuste que isso provoca no total do pedido. */
public enum FormaPagamento {

    /** 5% de desconto no total do pedido, sempre a vista. */
    PIX {
        @Override
        public boolean aceitaParcelas(int parcelas) {
            return parcelas == 1;
        }

        @Override
        public ResultadoPagamento aplicar(BigDecimal totalPedido, int parcelas) {
            BigDecimal desconto = Dinheiro.percentual(totalPedido, new BigDecimal("0.05"));
            return aVista(totalPedido.subtract(desconto));
        }
    },

    /** Ate 3x sem juros; de 4x a 12x com juros de 1,99% ao mes pela tabela Price. */
    CARTAO {
        @Override
        public boolean aceitaParcelas(int parcelas) {
            return parcelas >= 1 && parcelas <= 12;
        }

        @Override
        public ResultadoPagamento aplicar(BigDecimal totalPedido, int parcelas) {
            if (parcelas <= PARCELAS_SEM_JUROS) {
                BigDecimal parcela = Dinheiro.centavos(
                        totalPedido.divide(BigDecimal.valueOf(parcelas), CONTA));
                return new ResultadoPagamento(Dinheiro.centavos(totalPedido), parcela);
            }
            BigDecimal parcela = Dinheiro.centavos(parcelaPrice(totalPedido, parcelas));
            return new ResultadoPagamento(
                    Dinheiro.centavos(parcela.multiply(BigDecimal.valueOf(parcelas))), parcela);
        }

        /** parcela = total x taxa / (1 - (1 + taxa)^-parcelas) */
        private BigDecimal parcelaPrice(BigDecimal totalPedido, int parcelas) {
            BigDecimal umMaisTaxa = BigDecimal.ONE.add(TAXA_MENSAL);
            BigDecimal fatorInverso = BigDecimal.ONE.divide(umMaisTaxa.pow(parcelas, CONTA), CONTA);
            return totalPedido.multiply(TAXA_MENSAL, CONTA)
                    .divide(BigDecimal.ONE.subtract(fatorInverso), CONTA);
        }
    },

    /** Tarifa bancaria de R$ 3,49, sempre a vista, e nao vale acima de R$ 1.000,00. */
    BOLETO {
        @Override
        public boolean aceitaParcelas(int parcelas) {
            return parcelas == 1;
        }

        @Override
        public boolean atende(ContextoPagamento contexto) {
            return contexto.totalSemImposto().compareTo(TETO_BOLETO) <= 0;
        }

        @Override
        public ResultadoPagamento aplicar(BigDecimal totalPedido, int parcelas) {
            return aVista(totalPedido.add(TARIFA_BOLETO));
        }
    };

    private static final int PARCELAS_SEM_JUROS = 3;
    private static final BigDecimal TAXA_MENSAL = new BigDecimal("0.0199");
    private static final BigDecimal TARIFA_BOLETO = new BigDecimal("3.49");
    private static final BigDecimal TETO_BOLETO = new BigDecimal("1000.00");
    private static final MathContext CONTA = MathContext.DECIMAL128;

    public abstract boolean aceitaParcelas(int parcelas);

    public abstract ResultadoPagamento aplicar(BigDecimal totalPedido, int parcelas);

    /** Se a forma de pagamento atende este pedido (ex.: teto do boleto). */
    public boolean atende(ContextoPagamento contexto) {
        return true;
    }

    static ResultadoPagamento aVista(BigDecimal totalFinal) {
        BigDecimal valor = Dinheiro.centavos(totalFinal);
        return new ResultadoPagamento(valor, valor);
    }

    public static Optional<FormaPagamento> porCodigo(String codigo) {
        return Codigos.buscar(FormaPagamento.class, codigo);
    }
}

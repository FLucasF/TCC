package com.loja.checkout.pagamento;

import static com.loja.checkout.dominio.Dinheiro.arredondar;
import static com.loja.checkout.dominio.Dinheiro.reais;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * Cada forma de pagamento guarda num lugar so o seu parcelamento permitido, a
 * sua limitacao e o seu ajuste sobre o total do pedido.
 */
public enum FormaPagamento {

    PIX {
        @Override
        public boolean permiteParcelas(int parcelas) {
            return parcelas == 1;
        }

        @Override
        public ResultadoPagamento calcular(BigDecimal totalPedido, int parcelas) {
            BigDecimal desconto = arredondar(totalPedido.multiply(reais("0.05")));
            return aVista(arredondar(totalPedido.subtract(desconto)));
        }
    },

    BOLETO {
        @Override
        public boolean permiteParcelas(int parcelas) {
            return parcelas == 1;
        }

        @Override
        public boolean atende(BigDecimal totalPedido) {
            return totalPedido.compareTo(reais("1000.00")) <= 0;
        }

        @Override
        public ResultadoPagamento calcular(BigDecimal totalPedido, int parcelas) {
            return aVista(arredondar(totalPedido.add(reais("3.49"))));
        }
    },

    CARTAO {
        private static final int PARCELAS_SEM_JUROS = 3;
        private static final BigDecimal TAXA_AO_MES = reais("0.0199");

        @Override
        public boolean permiteParcelas(int parcelas) {
            return parcelas >= 1 && parcelas <= 12;
        }

        @Override
        public ResultadoPagamento calcular(BigDecimal totalPedido, int parcelas) {
            if (parcelas <= PARCELAS_SEM_JUROS) {
                return new ResultadoPagamento(totalPedido, dividir(totalPedido, parcelas));
            }
            BigDecimal parcela = arredondar(price(totalPedido, parcelas));
            return new ResultadoPagamento(
                    arredondar(parcela.multiply(BigDecimal.valueOf(parcelas))), parcela);
        }

        private BigDecimal dividir(BigDecimal totalPedido, int parcelas) {
            return totalPedido.divide(BigDecimal.valueOf(parcelas), 2, RoundingMode.HALF_EVEN);
        }

        /** parcela = total x taxa / (1 - (1 + taxa)^-parcelas), na forma sem expoente negativo. */
        private BigDecimal price(BigDecimal totalPedido, int parcelas) {
            BigDecimal fator = BigDecimal.ONE.add(TAXA_AO_MES).pow(parcelas);
            return totalPedido.multiply(TAXA_AO_MES).multiply(fator)
                    .divide(fator.subtract(BigDecimal.ONE), 12, RoundingMode.HALF_EVEN);
        }
    };

    public abstract boolean permiteParcelas(int parcelas);

    public abstract ResultadoPagamento calcular(BigDecimal totalPedido, int parcelas);

    public boolean atende(BigDecimal totalPedido) {
        return true;
    }

    protected static ResultadoPagamento aVista(BigDecimal totalFinal) {
        return new ResultadoPagamento(totalFinal, totalFinal);
    }
}

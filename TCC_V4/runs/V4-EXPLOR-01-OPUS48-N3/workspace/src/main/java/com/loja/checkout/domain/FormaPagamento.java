package com.loja.checkout.domain;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Optional;

/**
 * Forma de pagamento. Cada forma tem suas parcelas permitidas, sua
 * disponibilidade e seu ajuste sobre o total do pedido; cada uma mora no seu
 * próprio membro. O resultado traz valor final e valor da parcela, porque no
 * cartão com juros o valor final é parcela × nº de parcelas (≠ total), enquanto
 * nos demais casos o valor final é o próprio total ajustado.
 */
public enum FormaPagamento {

    PIX {
        @Override
        public boolean parcelasValidas(int parcelas) {
            return parcelas == 1;
        }

        @Override
        public Resultado calcular(BigDecimal totalPedido, int parcelas) {
            BigDecimal desconto = Dinheiro.centavos(totalPedido.multiply(new BigDecimal("0.05")));
            BigDecimal valorFinal = Dinheiro.centavos(totalPedido.subtract(desconto));
            return new Resultado(valorFinal, valorFinal);
        }
    },
    BOLETO {
        @Override
        public boolean parcelasValidas(int parcelas) {
            return parcelas == 1;
        }

        @Override
        public boolean disponivel(BigDecimal totalPedido) {
            return totalPedido.compareTo(new BigDecimal("1000")) <= 0;
        }

        @Override
        public Resultado calcular(BigDecimal totalPedido, int parcelas) {
            BigDecimal valorFinal = Dinheiro.centavos(totalPedido.add(new BigDecimal("3.49")));
            return new Resultado(valorFinal, valorFinal);
        }
    },
    CARTAO {
        private static final int SEM_JUROS_ATE = 3;
        private static final BigDecimal TAXA_MENSAL = new BigDecimal("0.0199");
        private static final int ESCALA_CALCULO = 20;

        @Override
        public boolean parcelasValidas(int parcelas) {
            return parcelas >= 1 && parcelas <= 12;
        }

        @Override
        public Resultado calcular(BigDecimal totalPedido, int parcelas) {
            if (parcelas <= SEM_JUROS_ATE) {
                BigDecimal parcela = Dinheiro.centavos(
                        totalPedido.divide(BigDecimal.valueOf(parcelas), ESCALA_CALCULO, RoundingMode.HALF_EVEN));
                return new Resultado(Dinheiro.centavos(totalPedido), parcela);
            }
            // Tabela Price toda em BigDecimal, como as demais etapas:
            // parcela = total × taxa ÷ (1 − (1 + taxa)^−n).
            BigDecimal umMaisTaxa = BigDecimal.ONE.add(TAXA_MENSAL);
            BigDecimal fator = BigDecimal.ONE.subtract(
                    BigDecimal.ONE.divide(umMaisTaxa.pow(parcelas), ESCALA_CALCULO, RoundingMode.HALF_EVEN));
            BigDecimal parcelaBruta = totalPedido.multiply(TAXA_MENSAL)
                    .divide(fator, ESCALA_CALCULO, RoundingMode.HALF_EVEN);
            BigDecimal parcela = Dinheiro.centavos(parcelaBruta);
            BigDecimal valorFinal = Dinheiro.centavos(parcela.multiply(BigDecimal.valueOf(parcelas)));
            return new Resultado(valorFinal, parcela);
        }
    };

    public abstract boolean parcelasValidas(int parcelas);

    public boolean disponivel(BigDecimal totalPedido) {
        return true;
    }

    public abstract Resultado calcular(BigDecimal totalPedido, int parcelas);

    public static Optional<FormaPagamento> resolver(String codigo) {
        return Catalogo.achar(FormaPagamento.class, codigo);
    }

    public record Resultado(BigDecimal valorFinal, BigDecimal valorParcela) {
    }
}

package loja.checkout.pagamento;

import java.math.BigDecimal;
import java.math.MathContext;
import loja.checkout.dominio.Dinheiro;
import org.springframework.stereotype.Component;

public final class Formas {
    private Formas() {
    }

    @Component
    static class Pix implements FormaPagamento {
        public String codigo() {
            return "PIX";
        }

        public boolean parcelasPermitidas(int parcelas) {
            return parcelas == 1;
        }

        public Cobranca cobrar(BigDecimal total, int parcelas) {
            BigDecimal desconto = Dinheiro.arredondar(total.multiply(new BigDecimal("0.05")));
            BigDecimal valor = total.subtract(desconto);
            return new Cobranca(valor, valor);
        }
    }

    @Component
    static class Boleto implements FormaPagamento {
        private static final BigDecimal LIMITE = new BigDecimal("1000.00");
        private static final BigDecimal TARIFA = new BigDecimal("3.49");

        public String codigo() {
            return "BOLETO";
        }

        public boolean parcelasPermitidas(int parcelas) {
            return parcelas == 1;
        }

        public boolean disponivel(BigDecimal total) {
            return total.compareTo(LIMITE) <= 0;
        }

        public Cobranca cobrar(BigDecimal total, int parcelas) {
            BigDecimal valor = total.add(TARIFA);
            return new Cobranca(valor, valor);
        }
    }

    @Component
    static class Cartao implements FormaPagamento {
        private static final int MAX_SEM_JUROS = 3;
        private static final int MAX_PARCELAS = 12;
        private static final BigDecimal TAXA = new BigDecimal("0.0199");

        public String codigo() {
            return "CARTAO";
        }

        public boolean parcelasPermitidas(int parcelas) {
            return parcelas >= 1 && parcelas <= MAX_PARCELAS;
        }

        public Cobranca cobrar(BigDecimal total, int parcelas) {
            BigDecimal n = BigDecimal.valueOf(parcelas);
            if (parcelas <= MAX_SEM_JUROS) {
                return new Cobranca(total, Dinheiro.arredondar(total.divide(n, MathContext.DECIMAL128)));
            }
            BigDecimal fator = BigDecimal.ONE.add(TAXA).pow(parcelas, MathContext.DECIMAL128);
            BigDecimal denominador = BigDecimal.ONE.subtract(BigDecimal.ONE.divide(fator, MathContext.DECIMAL128));
            BigDecimal parcela = Dinheiro.arredondar(
                    total.multiply(TAXA).divide(denominador, MathContext.DECIMAL128));
            return new Cobranca(parcela.multiply(n), parcela);
        }
    }
}

package br.loja.checkout.pagamento;

import br.loja.checkout.Dinheiro;
import java.math.BigDecimal;
import java.math.MathContext;
import org.springframework.stereotype.Component;

final class Pagamentos {
    private Pagamentos() {
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
            BigDecimal valor = total.subtract(Dinheiro.percentual(total, new BigDecimal("0.05")));
            return new Cobranca(valor, valor);
        }
    }

    @Component
    static class Boleto implements FormaPagamento {
        private static final BigDecimal LIMITE = new BigDecimal("1000.00");
        private static final BigDecimal TARIFA = Dinheiro.of("3.49");

        public String codigo() {
            return "BOLETO";
        }

        public boolean parcelasPermitidas(int parcelas) {
            return parcelas == 1;
        }

        public boolean atende(BigDecimal totalPedido) {
            return totalPedido.compareTo(LIMITE) <= 0;
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
            BigDecimal parcela;
            if (parcelas <= MAX_SEM_JUROS) {
                parcela = Dinheiro.arredondar(total.divide(BigDecimal.valueOf(parcelas), MathContext.DECIMAL128));
                return new Cobranca(total, parcela);
            }
            // Tabela Price: total * i / (1 - (1 + i)^-n)
            BigDecimal fator = BigDecimal.ONE.add(TAXA).pow(parcelas);
            BigDecimal divisor = BigDecimal.ONE.subtract(BigDecimal.ONE.divide(fator, MathContext.DECIMAL128));
            parcela = Dinheiro.arredondar(total.multiply(TAXA).divide(divisor, MathContext.DECIMAL128));
            return new Cobranca(Dinheiro.arredondar(parcela.multiply(BigDecimal.valueOf(parcelas))), parcela);
        }
    }
}

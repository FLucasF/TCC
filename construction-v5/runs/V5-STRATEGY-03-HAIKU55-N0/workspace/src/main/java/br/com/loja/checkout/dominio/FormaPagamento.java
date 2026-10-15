package br.com.loja.checkout.dominio;

import java.math.BigDecimal;
import java.math.MathContext;
import java.math.RoundingMode;

public enum FormaPagamento {

    PIX {
        @Override
        public boolean parcelamentoPermitido(int parcelas) {
            return parcelas == 1;
        }

        @Override
        public Pagamento calcular(BigDecimal total, int parcelas) {
            BigDecimal valor = total.subtract(Dinheiro.arredonda(total.multiply(new BigDecimal("0.05"))));
            return new Pagamento(valor, valor);
        }
    },

    CARTAO {
        @Override
        public boolean parcelamentoPermitido(int parcelas) {
            return parcelas >= 1 && parcelas <= 12;
        }

        @Override
        public Pagamento calcular(BigDecimal total, int parcelas) {
            if (parcelas <= 3) {
                return new Pagamento(total, total.divide(BigDecimal.valueOf(parcelas), 2, RoundingMode.HALF_EVEN));
            }
            BigDecimal parcela = parcelaComJuros(total, parcelas);
            return new Pagamento(parcela.multiply(BigDecimal.valueOf(parcelas)), parcela);
        }
    },

    BOLETO {
        @Override
        public boolean parcelamentoPermitido(int parcelas) {
            return parcelas == 1;
        }

        @Override
        public boolean disponivelPara(BigDecimal total) {
            return total.compareTo(new BigDecimal("1000.00")) <= 0;
        }

        @Override
        public Pagamento calcular(BigDecimal total, int parcelas) {
            BigDecimal valor = total.add(new BigDecimal("3.49"));
            return new Pagamento(valor, valor);
        }
    };

    private static final BigDecimal TAXA_JUROS_CARTAO = new BigDecimal("0.0199");
    private static final MathContext PRECISAO = new MathContext(40, RoundingMode.HALF_EVEN);

    public abstract boolean parcelamentoPermitido(int parcelas);

    public boolean disponivelPara(BigDecimal total) {
        return true;
    }

    public abstract Pagamento calcular(BigDecimal total, int parcelas);

    private static BigDecimal parcelaComJuros(BigDecimal total, int parcelas) {
        BigDecimal fatorDesconto = BigDecimal.ONE.divide(
                BigDecimal.ONE.add(TAXA_JUROS_CARTAO).pow(parcelas), PRECISAO);
        BigDecimal parcela = total.multiply(TAXA_JUROS_CARTAO)
                .divide(BigDecimal.ONE.subtract(fatorDesconto), PRECISAO);
        return Dinheiro.arredonda(parcela);
    }
}

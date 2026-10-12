package com.loja.checkout.dominio;

import java.math.BigDecimal;
import java.math.MathContext;

public enum FormaPagamento {

    PIX(1) {
        public Cobranca cobrar(BigDecimal total, int parcelas) {
            BigDecimal totalFinal = total.subtract(Dinheiro.porcentagem(total, "0.05"));
            return new Cobranca(totalFinal, totalFinal, parcelas);
        }
    },
    CARTAO(12) {
        public Cobranca cobrar(BigDecimal total, int parcelas) {
            if (parcelas <= 3) {
                BigDecimal parcela = Dinheiro.arredondar(total.divide(BigDecimal.valueOf(parcelas), MathContext.DECIMAL128));
                return new Cobranca(total, parcela, parcelas);
            }
            BigDecimal taxa = new BigDecimal("0.0199");
            BigDecimal fatorInverso = BigDecimal.ONE.divide(BigDecimal.ONE.add(taxa).pow(parcelas), MathContext.DECIMAL128);
            BigDecimal parcela = Dinheiro.arredondar(
                    total.multiply(taxa).divide(BigDecimal.ONE.subtract(fatorInverso), MathContext.DECIMAL128));
            return new Cobranca(parcela.multiply(BigDecimal.valueOf(parcelas)), parcela, parcelas);
        }
    },
    BOLETO(1) {
        public void verificarDisponibilidade(BigDecimal total) {
            if (total.compareTo(new BigDecimal("1000.00")) > 0) {
                throw new ErroPedido(CodigoErro.FORMA_PAGAMENTO_INDISPONIVEL);
            }
        }

        public Cobranca cobrar(BigDecimal total, int parcelas) {
            BigDecimal totalFinal = total.add(new BigDecimal("3.49"));
            return new Cobranca(totalFinal, totalFinal, parcelas);
        }
    };

    private final int maxParcelas;

    FormaPagamento(int maxParcelas) {
        this.maxParcelas = maxParcelas;
    }

    public int maxParcelas() {
        return maxParcelas;
    }

    public void verificarDisponibilidade(BigDecimal total) {
    }

    public abstract Cobranca cobrar(BigDecimal total, int parcelas);
}

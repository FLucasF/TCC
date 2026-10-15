package com.loja.checkout.dominio;

import java.math.BigDecimal;
import java.math.MathContext;

public enum FormaPagamento {

    PIX {
        @Override
        public Pagamento aplicar(BigDecimal total, int parcelas) {
            return Pagamento.avista(total.subtract(Dinheiro.percentual(total, DESCONTO_PIX)));
        }
    },

    BOLETO {
        @Override
        public void validarDisponibilidade(BigDecimal total) {
            if (total.compareTo(LIMITE_BOLETO) > 0) {
                throw new RecusaPedido(Erro.FORMA_PAGAMENTO_INDISPONIVEL);
            }
        }

        @Override
        public Pagamento aplicar(BigDecimal total, int parcelas) {
            return Pagamento.avista(total.add(TARIFA_BOLETO));
        }
    },

    CARTAO {
        @Override
        public void validarParcelas(int parcelas) {
            if (parcelas < 1 || parcelas > MAX_PARCELAS_CARTAO) {
                throw new RecusaPedido(Erro.PARCELAMENTO_INVALIDO);
            }
        }

        @Override
        public Pagamento aplicar(BigDecimal total, int parcelas) {
            if (parcelas <= PARCELAS_SEM_JUROS) {
                BigDecimal parcela = Dinheiro.dividir(total, parcelas);
                return new Pagamento(total, parcela, parcelas);
            }
            BigDecimal fator = BigDecimal.ONE.add(TAXA_JUROS_CARTAO).pow(parcelas);
            BigDecimal parcela = Dinheiro.centavos(total.multiply(TAXA_JUROS_CARTAO).multiply(fator)
                    .divide(fator.subtract(BigDecimal.ONE), MathContext.DECIMAL128));
            return new Pagamento(parcela.multiply(BigDecimal.valueOf(parcelas)), parcela, parcelas);
        }
    };

    private static final BigDecimal DESCONTO_PIX = new BigDecimal("0.05");
    private static final BigDecimal TARIFA_BOLETO = new BigDecimal("3.49");
    private static final BigDecimal LIMITE_BOLETO = new BigDecimal("1000");
    private static final BigDecimal TAXA_JUROS_CARTAO = new BigDecimal("0.0199");
    private static final int PARCELAS_SEM_JUROS = 3;
    private static final int MAX_PARCELAS_CARTAO = 12;

    public void validarParcelas(int parcelas) {
        if (parcelas != 1) {
            throw new RecusaPedido(Erro.PARCELAMENTO_INVALIDO);
        }
    }

    public void validarDisponibilidade(BigDecimal total) {
    }

    public abstract Pagamento aplicar(BigDecimal total, int parcelas);
}

package com.loja.dominio;

import java.math.BigDecimal;
import java.math.MathContext;
import java.math.RoundingMode;

import org.springframework.stereotype.Component;

public final class FormasPagamento {

    private FormasPagamento() {
    }

    @Component
    public static class Pix implements FormaPagamento {
        @Override
        public String codigo() {
            return "PIX";
        }

        @Override
        public boolean parcelasPermitidas(int parcelas) {
            return parcelas == 1;
        }

        @Override
        public Resultado calcular(BigDecimal total, int parcelas, boolean usouCupom) {
            BigDecimal desconto = usouCupom ? Dinheiro.ZERO : Dinheiro.porcentagem(total, "0.05");
            BigDecimal finalPix = Dinheiro.arredondar(total.subtract(desconto));
            return new Resultado(finalPix, finalPix);
        }
    }

    @Component
    public static class Boleto implements FormaPagamento {

        private static final BigDecimal LIMITE = Dinheiro.de("1000.00");

        @Override
        public String codigo() {
            return "BOLETO";
        }

        @Override
        public boolean parcelasPermitidas(int parcelas) {
            return parcelas == 1;
        }

        @Override
        public boolean disponivel(BigDecimal totalPedido) {
            return totalPedido.compareTo(LIMITE) <= 0;
        }

        @Override
        public Resultado calcular(BigDecimal total, int parcelas, boolean usouCupom) {
            BigDecimal finalBoleto = Dinheiro.arredondar(total.add(Dinheiro.de("3.49")));
            return new Resultado(finalBoleto, finalBoleto);
        }
    }

    @Component
    public static class Cartao implements FormaPagamento {

        private static final BigDecimal TAXA_MENSAL = new BigDecimal("0.0199");
        private static final MathContext PRECISAO = new MathContext(40);

        @Override
        public String codigo() {
            return "CARTAO";
        }

        @Override
        public boolean parcelasPermitidas(int parcelas) {
            return parcelas >= 1 && parcelas <= 12;
        }

        @Override
        public Resultado calcular(BigDecimal total, int parcelas, boolean usouCupom) {
            BigDecimal n = BigDecimal.valueOf(parcelas);
            if (parcelas <= 3) {
                return new Resultado(total, total.divide(n, 2, RoundingMode.HALF_EVEN));
            }
            // Tabela Price: total * i / (1 - (1 + i)^-n)
            BigDecimal potencia = BigDecimal.ONE.add(TAXA_MENSAL).pow(parcelas, PRECISAO);
            BigDecimal fator = BigDecimal.ONE.subtract(BigDecimal.ONE.divide(potencia, PRECISAO));
            BigDecimal parcela = Dinheiro.arredondar(total.multiply(TAXA_MENSAL).divide(fator, PRECISAO));
            return new Resultado(Dinheiro.arredondar(parcela.multiply(n)), parcela);
        }
    }
}

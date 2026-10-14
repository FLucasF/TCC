package com.loja.dominio;

import java.math.BigDecimal;
import java.util.Optional;

import org.springframework.stereotype.Component;

public final class Cupons {

    private Cupons() {
    }

    @Component
    public static class BemVindo10 implements Cupom {
        @Override
        public String codigo() {
            return "BEMVINDO10";
        }

        @Override
        public Optional<BigDecimal> desconto(Contexto c) {
            if (!c.primeiraCompra().getAsBoolean()) {
                return Optional.empty();
            }
            return Optional.of(Dinheiro.porcentagem(c.subtotalProdutos(), "0.10"));
        }
    }

    @Component
    public static class Menos50 implements Cupom {
        @Override
        public String codigo() {
            return "MENOS50";
        }

        @Override
        public Optional<BigDecimal> desconto(Contexto c) {
            if (c.subtotalProdutos().compareTo(Dinheiro.de("300.00")) < 0) {
                return Optional.empty();
            }
            return Optional.of(Dinheiro.de("50.00"));
        }
    }

    @Component
    public static class FreteGratis implements Cupom {
        @Override
        public String codigo() {
            return "FRETEGRATIS";
        }

        @Override
        public Optional<BigDecimal> desconto(Contexto c) {
            if ("RETIRADA_LOJA".equals(c.modalidade())) {
                return Optional.empty();
            }
            return Optional.of(c.frete());
        }
    }

    @Component
    public static class Leve3Pague2 implements Cupom {
        @Override
        public String codigo() {
            return "LEVE3PAGUE2";
        }

        @Override
        public Optional<BigDecimal> desconto(Contexto c) {
            BigDecimal total = Dinheiro.ZERO;
            for (Linha l : c.linhas()) {
                total = total.add(l.preco().multiply(BigDecimal.valueOf(l.quantidade() / 3)));
            }
            if (total.signum() == 0) {
                return Optional.empty();
            }
            return Optional.of(Dinheiro.arredondar(total));
        }
    }
}

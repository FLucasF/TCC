package com.loja.dominio;

import java.math.BigDecimal;

import org.springframework.stereotype.Component;

public final class ModalidadesEntrega {

    private ModalidadesEntrega() {
    }

    /** Opções cotadas por uma transportadora externa. */
    public abstract static class PorTransportadora implements ModalidadeEntrega {

        private final Transportadora transportadora;
        private final String codigo;

        protected PorTransportadora(Transportadora transportadora, String codigo) {
            this.transportadora = transportadora;
            this.codigo = codigo;
        }

        @Override
        public String codigo() {
            return codigo;
        }

        @Override
        public Cotacao cotar(Contexto contexto) {
            Transportadora.Cotacao cotacao = transportadora.cotar(contexto.cep(), contexto.pesoKg(), codigo)
                    .orElseThrow(() -> ErroNegocio.invalido("MODALIDADE_INDISPONIVEL"));
            return new Cotacao(Dinheiro.arredondar(cotacao.valor()),
                    cotacao.prazoDias() + contexto.regiao().diasExtraDespacho());
        }
    }

    @Component
    public static class Economica extends PorTransportadora {
        public Economica(Transportadora transportadora) {
            super(transportadora, "ECONOMICA");
        }
    }

    @Component
    public static class Expressa extends PorTransportadora {
        public Expressa(Transportadora transportadora) {
            super(transportadora, "EXPRESSA");
        }
    }

    @Component
    public static class RetiradaLoja implements ModalidadeEntrega {
        @Override
        public String codigo() {
            return "RETIRADA_LOJA";
        }

        @Override
        public Cotacao cotar(Contexto contexto) {
            return new Cotacao(Dinheiro.ZERO, 1);
        }
    }

    @Component
    public static class Motoboy implements ModalidadeEntrega {

        private static final BigDecimal PESO_MAXIMO_KG = new BigDecimal("5");

        @Override
        public String codigo() {
            return "MOTOBOY";
        }

        @Override
        public Cotacao cotar(Contexto contexto) {
            if (contexto.pesoKg().compareTo(PESO_MAXIMO_KG) > 0) {
                throw ErroNegocio.invalido("MODALIDADE_INDISPONIVEL");
            }
            return new Cotacao(Dinheiro.de("18.00"), 0);
        }

        /** Motoboy é terceirizado: o clube não isenta o frete. */
        @Override
        public boolean aceitaFreteGratisDoClube() {
            return false;
        }
    }
}

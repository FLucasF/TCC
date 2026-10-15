package com.loja.dominio;

import java.math.BigDecimal;

import org.springframework.stereotype.Component;

public final class NiveisClube {

    private NiveisClube() {
    }

    @Component
    public static class Bronze implements NivelClube {
        @Override
        public String codigo() {
            return "BRONZE";
        }

        @Override
        public String taxaCredito() {
            return "0";
        }
    }

    @Component
    public static class Prata implements NivelClube {
        @Override
        public String codigo() {
            return "PRATA";
        }

        @Override
        public String taxaCredito() {
            return "0.02";
        }
    }

    @Component
    public static class Ouro implements NivelClube {
        @Override
        public String codigo() {
            return "OURO";
        }

        @Override
        public String taxaCredito() {
            return "0.05";
        }

        @Override
        public boolean freteGratis() {
            return true;
        }

        @Override
        public boolean brinde(BigDecimal subtotalProdutos) {
            return subtotalProdutos.compareTo(Dinheiro.de("500.00")) > 0;
        }
    }
}

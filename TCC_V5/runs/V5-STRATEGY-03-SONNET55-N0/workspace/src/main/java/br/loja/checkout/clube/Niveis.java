package br.loja.checkout.clube;

import java.math.BigDecimal;
import org.springframework.stereotype.Component;

final class Niveis {
    private Niveis() {
    }

    @Component
    static class Bronze implements NivelClube {
        public String codigo() {
            return "BRONZE";
        }
    }

    @Component
    static class Prata implements NivelClube {
        public String codigo() {
            return "PRATA";
        }

        public BigDecimal percentualCredito() {
            return new BigDecimal("0.02");
        }
    }

    @Component
    static class Ouro implements NivelClube {
        private static final BigDecimal LIMITE_BRINDE = new BigDecimal("500.00");

        public String codigo() {
            return "OURO";
        }

        public BigDecimal percentualCredito() {
            return new BigDecimal("0.05");
        }

        public boolean freteGratis() {
            return true;
        }

        public boolean brinde(BigDecimal subtotalProdutos) {
            return subtotalProdutos.compareTo(LIMITE_BRINDE) > 0;
        }
    }
}

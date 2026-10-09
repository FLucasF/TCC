package loja.checkout.clube;

import java.math.BigDecimal;
import loja.checkout.dominio.Dinheiro;
import loja.checkout.dominio.Pedido;
import org.springframework.stereotype.Component;

public final class Niveis {
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

        public BigDecimal credito(Pedido pedido) {
            return Dinheiro.arredondar(pedido.subtotal().multiply(new BigDecimal("0.02")));
        }
    }

    @Component
    static class Ouro implements NivelClube {
        private static final BigDecimal LIMITE_BRINDE = new BigDecimal("500.00");

        public String codigo() {
            return "OURO";
        }

        public BigDecimal credito(Pedido pedido) {
            return Dinheiro.arredondar(pedido.subtotal().multiply(new BigDecimal("0.05")));
        }

        public boolean freteGratis() {
            return true;
        }

        public boolean brinde(Pedido pedido) {
            return pedido.subtotal().compareTo(LIMITE_BRINDE) > 0;
        }
    }
}

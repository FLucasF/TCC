package loja.checkout.entrega;

import java.math.BigDecimal;
import loja.checkout.dominio.Dinheiro;
import loja.checkout.dominio.Pedido;
import org.springframework.stereotype.Component;

public final class Modalidades {
    private Modalidades() {
    }

    abstract static class PorPeso implements ModalidadeEntrega {
        private final String codigo;
        private final int prazo;
        private final BigDecimal base;
        private final BigDecimal porKg;

        PorPeso(String codigo, int prazo, String base, String porKg) {
            this.codigo = codigo;
            this.prazo = prazo;
            this.base = new BigDecimal(base);
            this.porKg = new BigDecimal(porKg);
        }

        public String codigo() {
            return codigo;
        }

        public int prazoDias() {
            return prazo;
        }

        public BigDecimal frete(Pedido pedido) {
            return Dinheiro.arredondar(base.add(porKg.multiply(pedido.pesoKg())));
        }
    }

    @Component
    static class Economica extends PorPeso {
        Economica() {
            super("ECONOMICA", 7, "12.00", "2.00");
        }
    }

    @Component
    static class Expressa extends PorPeso {
        Expressa() {
            super("EXPRESSA", 2, "25.00", "4.50");
        }
    }

    @Component
    static class RetiradaLoja implements ModalidadeEntrega {
        public String codigo() {
            return "RETIRADA_LOJA";
        }

        public int prazoDias() {
            return 1;
        }

        public BigDecimal frete(Pedido pedido) {
            return Dinheiro.ZERO;
        }
    }

    @Component
    static class Motoboy implements ModalidadeEntrega {
        private static final BigDecimal PESO_MAXIMO = new BigDecimal("5");

        public String codigo() {
            return "MOTOBOY";
        }

        public int prazoDias() {
            return 0;
        }

        public boolean atende(Pedido pedido) {
            return pedido.pesoKg().compareTo(PESO_MAXIMO) <= 0;
        }

        public BigDecimal frete(Pedido pedido) {
            return Dinheiro.arredondar(new BigDecimal("18.00"));
        }
    }
}

package loja;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

class PlanosTest {

    private final Planos planos = new Planos();

    @Test
    void basicoCobraOPrecoCheio() {
        assertEquals(100.0, planos.preco("BASICO", 100.0), 0.001);
    }

    @Test
    void plusTemDezPorCentoDeDesconto() {
        assertEquals(90.0, planos.preco("PLUS", 100.0), 0.001);
    }

    @Test
    void basicoPermiteUmUsuario() {
        assertEquals(1, planos.limite("BASICO"));
    }

    @Test
    void plusPermiteTresUsuarios() {
        assertEquals(3, planos.limite("PLUS"));
    }

    @Test
    void planoDesconhecidoEhRecusado() {
        assertThrows(IllegalArgumentException.class, () -> planos.preco("OURO", 100.0));
    }
}

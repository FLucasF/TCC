package loja.checkout;

import static org.junit.jupiter.api.Assertions.assertEquals;

import loja.checkout.web.ResumoRequest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
class CheckoutIntegracaoTest {

    @Autowired
    ResumoService service;

    @Test
    void contextoMontaTodasAsOpcoes() {
        var r = service.resumir(new ResumoRequest(
                java.util.List.of(new ResumoRequest.ItemRequest("A", new java.math.BigDecimal("10"), 1,
                        new java.math.BigDecimal("1"))),
                "MOTOBOY", "BEMVINDO10", "PIX", null, "PRATA", "SUL"));
        assertEquals(0, r.prazoEntregaDias());
    }
}

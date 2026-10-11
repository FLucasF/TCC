package com.loja.checkout;

import com.loja.checkout.domain.CheckoutService;
import com.loja.checkout.web.ItemRequest;
import com.loja.checkout.web.ResumoRequest;
import com.loja.checkout.web.ResumoResponse;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/** Vantagens do clube e cupom de frete gratis. */
@SpringBootTest
class VantagensTest {

    @Autowired
    CheckoutService service;

    @Test
    void ouroAcimaDe500GanhaBrinde() {
        ResumoResponse r = service.resumir(new ResumoRequest(
                List.of(new ItemRequest("Tenis", new BigDecimal("249.90"), 3, new BigDecimal("1.20"))),
                "RETIRADA_LOJA", null, "PIX", null, "OURO", "SUDESTE"));

        assertThat(r.subtotalProdutos()).isEqualByComparingTo("749.70");
        assertThat(r.brinde()).isTrue();
        // 5% de 749,70 = 37,485; meio-para-o-par arredonda para o par -> 37,48.
        assertThat(r.creditoProximaCompra()).isEqualByComparingTo("37.48");
    }

    @Test
    void freteGratisDescontaExatamenteOFrete() {
        ResumoResponse r = service.resumir(new ResumoRequest(
                List.of(new ItemRequest("Camiseta", new BigDecimal("79.90"), 2, new BigDecimal("0.30")),
                        new ItemRequest("Tenis", new BigDecimal("249.90"), 1, new BigDecimal("1.20"))),
                "EXPRESSA", "FRETEGRATIS", "PIX", null, "BRONZE", "SUDESTE"));

        assertThat(r.frete()).isEqualByComparingTo("33.10");
        assertThat(r.descontoCupom()).isEqualByComparingTo("33.10");
    }
}

package com.loja.checkout;

import static org.assertj.core.api.Assertions.assertThat;

import com.loja.checkout.api.CheckoutService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
class CheckoutApplicationTest {

    @Autowired
    private CheckoutService servico;

    @Test
    void aplicacaoSobeComOServicoDeResumo() {
        assertThat(servico).isNotNull();
    }
}

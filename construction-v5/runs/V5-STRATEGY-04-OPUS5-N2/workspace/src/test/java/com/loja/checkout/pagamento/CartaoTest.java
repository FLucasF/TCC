package com.loja.checkout.pagamento;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;

class CartaoTest {

    private final Cartao cartao = new Cartao();

    @Test
    @DisplayName("Ate 3x o valor final e o proprio total do pedido")
    void semJuros() {
        Cobranca cobranca = cartao.cobrar(new BigDecimal("262.29"), 3);

        assertThat(cobranca).isEqualTo(
                new Cobranca(new BigDecimal("262.29"), 3, new BigDecimal("87.43")));
    }

    @Test
    @DisplayName("De 4x a 12x cobra juros de 1,99% ao mes pela tabela Price")
    void comJuros() {
        Cobranca cobranca = cartao.cobrar(new BigDecimal("431.45"), 6);

        assertThat(cobranca).isEqualTo(
                new Cobranca(new BigDecimal("462.00"), 6, new BigDecimal("77.00")));
    }

    @Test
    @DisplayName("Parcelamento permitido de 1x a 12x")
    void parcelamentoPermitido() {
        assertThat(cartao.permiteParcelas(0)).isFalse();
        assertThat(cartao.permiteParcelas(1)).isTrue();
        assertThat(cartao.permiteParcelas(12)).isTrue();
        assertThat(cartao.permiteParcelas(13)).isFalse();
    }
}

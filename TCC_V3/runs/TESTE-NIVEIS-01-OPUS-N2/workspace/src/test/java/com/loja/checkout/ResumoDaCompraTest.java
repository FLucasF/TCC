package com.loja.checkout;

import static org.assertj.core.api.Assertions.assertThat;

import com.loja.checkout.api.ItemRequest;
import com.loja.checkout.api.ResumoRequest;
import com.loja.checkout.api.ResumoResponse;
import com.loja.checkout.aplicacao.CalculadoraDoResumo;
import java.math.BigDecimal;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

/** Os exemplos conferidos pelo financeiro. */
@SpringBootTest
class ResumoDaCompraTest {

    private static final ItemRequest CAMISETA = item("Camiseta", "79.90", 2, "0.30");
    private static final ItemRequest TENIS = item("Tenis", "249.90", 1, "1.20");

    @Autowired
    private CalculadoraDoResumo calculadora;

    @Test
    void exemplo1_expressa_bemvindo10_pix_bronze_norte() {
        ResumoResponse resumo = calculadora.calcular(new ResumoRequest(
                List.of(CAMISETA, TENIS), "EXPRESSA", "BEMVINDO10", "PIX", 1, "BRONZE", "NORTE"));

        assertThat(resumo).isEqualTo(new ResumoResponse(
                valor("409.70"), valor("40.97"), valor("33.10"), 2, valor("10.24"),
                valor("-20.60"), valor("391.47"), 1, valor("391.47"), valor("0.00"), false));
    }

    @Test
    void exemplo2_economica_sem_cupom_cartao6x_prata_centro_oeste() {
        ResumoResponse resumo = calculadora.calcular(new ResumoRequest(
                List.of(CAMISETA, TENIS), "ECONOMICA", null, "CARTAO", 6, "PRATA", "CENTRO_OESTE"));

        assertThat(resumo).isEqualTo(new ResumoResponse(
                valor("409.70"), valor("0.00"), valor("15.60"), 7, valor("6.15"),
                valor("30.55"), valor("462.00"), 6, valor("77.00"), valor("8.19"), false));
    }

    @Test
    void exemplo3_motoboy_menos50_boleto_bronze_nordeste() {
        ResumoResponse resumo = calculadora.calcular(new ResumoRequest(
                List.of(item("Fone", "199.90", 2, "0.25")), "MOTOBOY", "MENOS50", "BOLETO", 1,
                "BRONZE", "NORDESTE"));

        assertThat(resumo).isEqualTo(new ResumoResponse(
                valor("399.80"), valor("50.00"), valor("18.00"), 0, valor("8.00"),
                valor("3.49"), valor("379.29"), 1, valor("379.29"), valor("0.00"), false));
    }

    @Test
    void exemplo4_retirada_leve3pague2_cartao3x_prata_sul() {
        ResumoResponse resumo = calculadora.calcular(new ResumoRequest(
                List.of(item("Meia", "19.90", 7, "0.10"), CAMISETA), "RETIRADA_LOJA", "LEVE3PAGUE2",
                "CARTAO", 3, "PRATA", "SUL"));

        assertThat(resumo).isEqualTo(new ResumoResponse(
                valor("299.10"), valor("39.80"), valor("0.00"), 1, valor("2.99"),
                valor("0.00"), valor("262.29"), 3, valor("87.43"), valor("5.98"), false));
    }

    @Test
    void exemplo5_expressa_sem_cupom_pix_ouro_sudeste() {
        ResumoResponse resumo = calculadora.calcular(new ResumoRequest(
                List.of(CAMISETA, TENIS), "EXPRESSA", null, "PIX", 1, "OURO", "SUDESTE"));

        assertThat(resumo).isEqualTo(new ResumoResponse(
                valor("409.70"), valor("0.00"), valor("0.00"), 2, valor("4.10"),
                valor("-20.69"), valor("393.11"), 1, valor("393.11"), valor("20.48"), false));
    }

    @Test
    void exemplo_do_anexo_expressa_bemvindo10_pix_ouro_sudeste() {
        ResumoResponse resumo = calculadora.calcular(new ResumoRequest(
                List.of(CAMISETA, TENIS), "EXPRESSA", "BEMVINDO10", "PIX", null, "OURO", "SUDESTE"));

        assertThat(resumo).isEqualTo(new ResumoResponse(
                valor("409.70"), valor("40.97"), valor("0.00"), 2, valor("4.10"),
                valor("-18.64"), valor("354.19"), 1, valor("354.19"), valor("20.48"), false));
    }

    static ItemRequest item(String nome, String preco, int quantidade, String peso) {
        return new ItemRequest(nome, new BigDecimal(preco), quantidade, new BigDecimal(peso));
    }

    static BigDecimal valor(String texto) {
        return new BigDecimal(texto);
    }
}

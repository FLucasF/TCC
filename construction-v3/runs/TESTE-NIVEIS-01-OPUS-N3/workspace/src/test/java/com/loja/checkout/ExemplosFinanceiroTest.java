package com.loja.checkout;

import com.loja.checkout.api.ResumoRequest;
import com.loja.checkout.api.ResumoRequest.ItemRequest;
import com.loja.checkout.api.ResumoResponse;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/** Os cinco exemplos conferidos pelo financeiro. */
@ExtendWith(SpringExtension.class)
@SpringBootTest
class ExemplosFinanceiroTest {

    private static final ItemRequest CAMISETA = item("Camiseta", "79.90", 2, "0.30");
    private static final ItemRequest TENIS = item("Tenis", "249.90", 1, "1.20");

    @Autowired
    private CalculadoraResumo calculadora;

    private static ItemRequest item(String nome, String preco, int quantidade, String peso) {
        return new ItemRequest(nome, new BigDecimal(preco), quantidade, new BigDecimal(peso));
    }

    @Test
    void exemplo1_expressa_bemvindo10_pix_bronze_norte() {
        ResumoResponse r = calculadora.calcular(new ResumoRequest(
                List.of(CAMISETA, TENIS), "EXPRESSA", "BEMVINDO10", "PIX", 1, "BRONZE", "NORTE"));

        assertThat(r).isEqualTo(resumo("409.70", "40.97", "33.10", 2, "10.24",
                "-20.60", "391.47", 1, "391.47", "0.00", false));
    }

    @Test
    void exemplo2_economica_sem_cupom_cartao6x_prata_centro_oeste() {
        ResumoResponse r = calculadora.calcular(new ResumoRequest(
                List.of(CAMISETA, TENIS), "ECONOMICA", null, "CARTAO", 6, "PRATA", "CENTRO_OESTE"));

        assertThat(r).isEqualTo(resumo("409.70", "0.00", "15.60", 7, "6.15",
                "30.55", "462.00", 6, "77.00", "8.19", false));
    }

    @Test
    void exemplo3_motoboy_menos50_boleto_bronze_nordeste() {
        ResumoResponse r = calculadora.calcular(new ResumoRequest(
                List.of(item("Fone", "199.90", 2, "0.25")), "MOTOBOY", "MENOS50", "BOLETO", null,
                "BRONZE", "NORDESTE"));

        assertThat(r).isEqualTo(resumo("399.80", "50.00", "18.00", 0, "8.00",
                "3.49", "379.29", 1, "379.29", "0.00", false));
    }

    @Test
    void exemplo4_retirada_leve3pague2_cartao3x_prata_sul() {
        ResumoResponse r = calculadora.calcular(new ResumoRequest(
                List.of(item("Meia", "19.90", 7, "0.10"), CAMISETA), "RETIRADA_LOJA", "LEVE3PAGUE2",
                "CARTAO", 3, "PRATA", "SUL"));

        assertThat(r).isEqualTo(resumo("299.10", "39.80", "0.00", 1, "2.99",
                "0.00", "262.29", 3, "87.43", "5.98", false));
    }

    @Test
    void exemplo5_expressa_sem_cupom_pix_ouro_sudeste() {
        ResumoResponse r = calculadora.calcular(new ResumoRequest(
                List.of(CAMISETA, TENIS), "EXPRESSA", null, "PIX", 1, "OURO", "SUDESTE"));

        assertThat(r).isEqualTo(resumo("409.70", "0.00", "0.00", 2, "4.10",
                "-20.69", "393.11", 1, "393.11", "20.48", false));
    }

    @Test
    void exemploDoAnexo_ouro_com_bemvindo10() {
        ResumoResponse r = calculadora.calcular(new ResumoRequest(
                List.of(CAMISETA, TENIS), "EXPRESSA", "BEMVINDO10", "PIX", 1, "OURO", "SUDESTE"));

        assertThat(r).isEqualTo(resumo("409.70", "40.97", "0.00", 2, "4.10",
                "-18.64", "354.19", 1, "354.19", "20.48", false));
    }

    private static ResumoResponse resumo(String subtotal, String cupom, String frete, int prazo,
                                         String seguro, String ajuste, String total, int parcelas,
                                         String parcela, String credito, boolean brinde) {
        return new ResumoResponse(new BigDecimal(subtotal), new BigDecimal(cupom), new BigDecimal(frete),
                prazo, new BigDecimal(seguro), new BigDecimal(ajuste), new BigDecimal(total), parcelas,
                new BigDecimal(parcela), new BigDecimal(credito), brinde);
    }
}

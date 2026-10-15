package com.loja.checkout;

import com.loja.checkout.dominio.CalculadoraResumo;
import com.loja.checkout.dominio.ItemInformado;
import com.loja.checkout.dominio.PedidoInformado;
import com.loja.checkout.dominio.ResumoCompra;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
class ExemplosDoFinanceiroTest {

    private static final ItemInformado CAMISETA =
            new ItemInformado("Camiseta", new BigDecimal("79.90"), 2, new BigDecimal("0.30"));
    private static final ItemInformado TENIS =
            new ItemInformado("Tenis", new BigDecimal("249.90"), 1, new BigDecimal("1.20"));

    @Autowired
    private CalculadoraResumo calculadora;

    @Test
    void exemplo1_expressa_bemvindo10_pix_bronze_norte() {
        ResumoCompra resumo = calculadora.calcular(new PedidoInformado(
                List.of(CAMISETA, TENIS), "EXPRESSA", "BEMVINDO10", "PIX", 1, "BRONZE", "NORTE"));

        confere(resumo, "409.70", "40.97", "33.10", 2, "10.24", "-20.60", "391.47", 1, "391.47", "0.00", false);
    }

    @Test
    void exemplo2_economica_sem_cupom_cartao_6x_prata_centro_oeste() {
        ResumoCompra resumo = calculadora.calcular(new PedidoInformado(
                List.of(CAMISETA, TENIS), "ECONOMICA", null, "CARTAO", 6, "PRATA", "CENTRO_OESTE"));

        confere(resumo, "409.70", "0.00", "15.60", 7, "6.15", "30.55", "462.00", 6, "77.00", "8.19", false);
    }

    @Test
    void exemplo3_motoboy_menos50_boleto_bronze_nordeste() {
        ResumoCompra resumo = calculadora.calcular(new PedidoInformado(
                List.of(new ItemInformado("Fone", new BigDecimal("199.90"), 2, new BigDecimal("0.25"))),
                "MOTOBOY", "MENOS50", "BOLETO", 1, "BRONZE", "NORDESTE"));

        confere(resumo, "399.80", "50.00", "18.00", 0, "8.00", "3.49", "379.29", 1, "379.29", "0.00", false);
    }

    @Test
    void exemplo4_retirada_leve3pague2_cartao_3x_prata_sul() {
        ResumoCompra resumo = calculadora.calcular(new PedidoInformado(
                List.of(new ItemInformado("Meia", new BigDecimal("19.90"), 7, new BigDecimal("0.10")), CAMISETA),
                "RETIRADA_LOJA", "LEVE3PAGUE2", "CARTAO", 3, "PRATA", "SUL"));

        confere(resumo, "299.10", "39.80", "0.00", 1, "2.99", "0.00", "262.29", 3, "87.43", "5.98", false);
    }

    @Test
    void exemplo5_expressa_sem_cupom_pix_ouro_sudeste() {
        ResumoCompra resumo = calculadora.calcular(new PedidoInformado(
                List.of(CAMISETA, TENIS), "EXPRESSA", null, "PIX", 1, "OURO", "SUDESTE"));

        confere(resumo, "409.70", "0.00", "0.00", 2, "4.10", "-20.69", "393.11", 1, "393.11", "20.48", false);
    }

    @Test
    void exemploDoAnexo_expressa_bemvindo10_pix_ouro_sudeste() {
        ResumoCompra resumo = calculadora.calcular(new PedidoInformado(
                List.of(CAMISETA, TENIS), "EXPRESSA", "BEMVINDO10", "PIX", 1, "OURO", "SUDESTE"));

        confere(resumo, "409.70", "40.97", "0.00", 2, "4.10", "-18.64", "354.19", 1, "354.19", "20.48", false);
    }

    private void confere(ResumoCompra resumo, String subtotal, String cupom, String frete, int prazo,
                         String seguro, String ajuste, String totalFinal, int parcelas, String valorParcela,
                         String credito, boolean brinde) {
        assertThat(resumo.subtotalProdutos()).isEqualByComparingTo(subtotal);
        assertThat(resumo.descontoCupom()).isEqualByComparingTo(cupom);
        assertThat(resumo.frete()).isEqualByComparingTo(frete);
        assertThat(resumo.prazoEntregaDias()).isEqualTo(prazo);
        assertThat(resumo.seguro()).isEqualByComparingTo(seguro);
        assertThat(resumo.ajustePagamento()).isEqualByComparingTo(ajuste);
        assertThat(resumo.totalFinal()).isEqualByComparingTo(totalFinal);
        assertThat(resumo.parcelas()).isEqualTo(parcelas);
        assertThat(resumo.valorParcela()).isEqualByComparingTo(valorParcela);
        assertThat(resumo.creditoProximaCompra()).isEqualByComparingTo(credito);
        assertThat(resumo.brinde()).isEqualTo(brinde);
        assertThat(resumo.subtotalProdutos().scale()).isEqualTo(2);
        assertThat(resumo.totalFinal().scale()).isEqualTo(2);
    }
}

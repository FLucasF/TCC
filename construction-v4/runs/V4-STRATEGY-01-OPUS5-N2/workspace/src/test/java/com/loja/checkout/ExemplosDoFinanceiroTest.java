package com.loja.checkout;

import static com.loja.checkout.Pedidos.CAMISETA;
import static com.loja.checkout.Pedidos.TENIS;
import static com.loja.checkout.Pedidos.item;
import static com.loja.checkout.Pedidos.pedido;
import static org.assertj.core.api.Assertions.assertThat;

import com.loja.checkout.contrato.PedidoRequest;
import com.loja.checkout.contrato.ResumoResponse;
import com.loja.checkout.dominio.CalculadoraResumo;
import java.math.BigDecimal;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
class ExemplosDoFinanceiroTest {

    @Autowired
    CalculadoraResumo calculadora;

    @Test
    void exemplo1_expressa_bemvindo10_pix_bronze_norte() {
        ResumoResponse resumo = calculadora.calcular(pedido(List.of(CAMISETA, TENIS),
                "EXPRESSA", "BEMVINDO10", "PIX", 1, "BRONZE", "NORTE"));

        confere(resumo, "409.70", "40.97", "33.10", 2, "10.24", "-20.60", "391.47", 1, "391.47", "0.00", false);
    }

    @Test
    void exemplo2_economica_sem_cupom_cartao6x_prata_centro_oeste() {
        ResumoResponse resumo = calculadora.calcular(pedido(List.of(CAMISETA, TENIS),
                "ECONOMICA", null, "CARTAO", 6, "PRATA", "CENTRO_OESTE"));

        confere(resumo, "409.70", "0.00", "15.60", 7, "6.15", "30.55", "462.00", 6, "77.00", "8.19", false);
    }

    @Test
    void exemplo3_motoboy_menos50_boleto_bronze_nordeste() {
        ResumoResponse resumo = calculadora.calcular(pedido(List.of(item("Fone", "199.90", 2, "0.25")),
                "MOTOBOY", "MENOS50", "BOLETO", 1, "BRONZE", "NORDESTE"));

        confere(resumo, "399.80", "50.00", "18.00", 0, "8.00", "3.49", "379.29", 1, "379.29", "0.00", false);
    }

    @Test
    void exemplo4_retirada_leve3pague2_cartao3x_prata_sul() {
        ResumoResponse resumo = calculadora.calcular(pedido(
                List.of(item("Meia", "19.90", 7, "0.10"), CAMISETA),
                "RETIRADA_LOJA", "LEVE3PAGUE2", "CARTAO", 3, "PRATA", "SUL"));

        confere(resumo, "299.10", "39.80", "0.00", 1, "2.99", "0.00", "262.29", 3, "87.43", "5.98", false);
    }

    @Test
    void exemplo5_expressa_sem_cupom_pix_ouro_sudeste() {
        ResumoResponse resumo = calculadora.calcular(pedido(List.of(CAMISETA, TENIS),
                "EXPRESSA", null, "PIX", 1, "OURO", "SUDESTE"));

        confere(resumo, "409.70", "0.00", "0.00", 2, "4.10", "-20.69", "393.11", 1, "393.11", "20.48", false);
    }

    @Test
    void exemplo_do_anexo_expressa_bemvindo10_pix_ouro_sudeste() {
        PedidoRequest requisicao = pedido(List.of(CAMISETA, TENIS),
                "EXPRESSA", "BEMVINDO10", "PIX", 1, "OURO", "SUDESTE");

        ResumoResponse resumo = calculadora.calcular(requisicao);

        confere(resumo, "409.70", "40.97", "0.00", 2, "4.10", "-18.64", "354.19", 1, "354.19", "20.48", false);
    }

    private void confere(ResumoResponse resumo, String subtotal, String cupom, String frete, int prazo,
                         String seguro, String ajuste, String total, int parcelas, String parcela,
                         String credito, boolean brinde) {
        assertThat(resumo.subtotalProdutos()).isEqualByComparingTo(subtotal);
        assertThat(resumo.descontoCupom()).isEqualByComparingTo(cupom);
        assertThat(resumo.frete()).isEqualByComparingTo(frete);
        assertThat(resumo.prazoEntregaDias()).isEqualTo(prazo);
        assertThat(resumo.seguro()).isEqualByComparingTo(seguro);
        assertThat(resumo.ajustePagamento()).isEqualByComparingTo(ajuste);
        assertThat(resumo.totalFinal()).isEqualByComparingTo(total);
        assertThat(resumo.parcelas()).isEqualTo(parcelas);
        assertThat(resumo.valorParcela()).isEqualByComparingTo(parcela);
        assertThat(resumo.creditoProximaCompra()).isEqualByComparingTo(credito);
        assertThat(resumo.brinde()).isEqualTo(brinde);
        assertThat(List.of(resumo.subtotalProdutos(), resumo.descontoCupom(), resumo.frete(),
                        resumo.seguro(), resumo.ajustePagamento(), resumo.totalFinal(),
                        resumo.valorParcela(), resumo.creditoProximaCompra()))
                .allSatisfy(valor -> assertThat(valor.scale()).isEqualTo(2));
    }

    @Test
    void arredonda_meio_para_o_par_em_cada_etapa() {
        ResumoResponse acima = calculadora.calcular(pedido(List.of(item("Brinco", "2.995", 1, "0.01")),
                "RETIRADA_LOJA", null, "CARTAO", 1, "BRONZE", "SUDESTE"));
        ResumoResponse abaixo = calculadora.calcular(pedido(List.of(item("Brinco", "2.985", 1, "0.01")),
                "RETIRADA_LOJA", null, "CARTAO", 1, "BRONZE", "SUDESTE"));

        assertThat(acima.subtotalProdutos()).isEqualByComparingTo(new BigDecimal("3.00"));
        assertThat(abaixo.subtotalProdutos()).isEqualByComparingTo(new BigDecimal("2.98"));
    }
}

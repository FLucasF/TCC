package com.loja.checkout;

import static com.loja.checkout.Pedidos.CAMISETA;
import static com.loja.checkout.Pedidos.TENIS;
import static com.loja.checkout.Pedidos.item;
import static com.loja.checkout.Pedidos.pedido;
import static org.assertj.core.api.Assertions.assertThat;

import com.loja.checkout.contrato.ResumoResponse;
import com.loja.checkout.dominio.CalculadoraResumo;
import java.math.BigDecimal;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
class RegrasDoClubeECuponsTest {

    @Autowired
    CalculadoraResumo calculadora;

    @Test
    void fretegratis_mostra_o_frete_e_desconta_o_mesmo_valor() {
        ResumoResponse resumo = calculadora.calcular(pedido(List.of(CAMISETA, TENIS),
                "ECONOMICA", "FRETEGRATIS", "PIX", 1, "BRONZE", "SUDESTE"));

        assertThat(resumo.frete()).isEqualByComparingTo(new BigDecimal("15.60"));
        assertThat(resumo.descontoCupom()).isEqualByComparingTo(new BigDecimal("15.60"));
        assertThat(resumo.totalFinal()).isEqualByComparingTo(new BigDecimal("393.11"));
    }

    @Test
    void fretegratis_no_ouro_nao_desconta_nada_porque_o_frete_ja_e_zero() {
        ResumoResponse resumo = calculadora.calcular(pedido(List.of(CAMISETA, TENIS),
                "ECONOMICA", "FRETEGRATIS", "PIX", 1, "OURO", "SUDESTE"));

        assertThat(resumo.frete()).isEqualByComparingTo(new BigDecimal("0.00"));
        assertThat(resumo.descontoCupom()).isEqualByComparingTo(new BigDecimal("0.00"));
    }

    @Test
    void ouro_leva_brinde_acima_de_quinhentos_reais_em_produtos() {
        ResumoResponse resumo = calculadora.calcular(pedido(List.of(item("Tenis", "249.90", 3, "1.20")),
                "EXPRESSA", null, "PIX", 1, "OURO", "SUDESTE"));

        assertThat(resumo.subtotalProdutos()).isEqualByComparingTo(new BigDecimal("749.70"));
        assertThat(resumo.brinde()).isTrue();
        assertThat(resumo.creditoProximaCompra()).isEqualByComparingTo(new BigDecimal("37.48"));
        assertThat(resumo.frete()).isEqualByComparingTo(new BigDecimal("0.00"));
    }

    @Test
    void ouro_em_quinhentos_reais_exatos_nao_leva_brinde() {
        ResumoResponse resumo = calculadora.calcular(pedido(List.of(item("Jaqueta", "250.00", 2, "0.80")),
                "RETIRADA_LOJA", null, "PIX", 1, "OURO", "SUDESTE"));

        assertThat(resumo.subtotalProdutos()).isEqualByComparingTo(new BigDecimal("500.00"));
        assertThat(resumo.brinde()).isFalse();
    }

    @Test
    void bronze_nao_ganha_credito_nem_isencao_de_frete() {
        ResumoResponse resumo = calculadora.calcular(pedido(List.of(CAMISETA),
                "ECONOMICA", null, "PIX", 1, "BRONZE", "SUDESTE"));

        assertThat(resumo.creditoProximaCompra()).isEqualByComparingTo(new BigDecimal("0.00"));
        assertThat(resumo.frete()).isEqualByComparingTo(new BigDecimal("13.20"));
    }

    @Test
    void seguro_e_a_taxa_da_regiao_sobre_os_produtos_sem_desconto_e_sem_frete() {
        List<String> regioes = List.of("SUDESTE", "SUL", "CENTRO_OESTE", "NORTE", "NORDESTE");
        List<String> esperados = List.of("4.10", "4.10", "6.15", "10.24", "8.19");

        for (int i = 0; i < regioes.size(); i++) {
            ResumoResponse resumo = calculadora.calcular(pedido(List.of(CAMISETA, TENIS),
                    "EXPRESSA", "BEMVINDO10", "PIX", 1, "BRONZE", regioes.get(i)));

            assertThat(resumo.seguro()).isEqualByComparingTo(new BigDecimal(esperados.get(i)));
        }
    }

    @Test
    void cartao_sem_juros_nao_cobra_ajuste_em_uma_duas_ou_tres_vezes() {
        for (int parcelas = 1; parcelas <= 3; parcelas++) {
            ResumoResponse resumo = calculadora.calcular(pedido(List.of(CAMISETA, TENIS),
                    "RETIRADA_LOJA", null, "CARTAO", parcelas, "BRONZE", "SUDESTE"));

            assertThat(resumo.ajustePagamento()).isEqualByComparingTo(new BigDecimal("0.00"));
            assertThat(resumo.totalFinal()).isEqualByComparingTo(new BigDecimal("413.80"));
            assertThat(resumo.parcelas()).isEqualTo(parcelas);
        }
    }

    @Test
    void cartao_em_doze_vezes_cobra_juros_pela_tabela_price() {
        ResumoResponse resumo = calculadora.calcular(pedido(List.of(CAMISETA, TENIS),
                "RETIRADA_LOJA", null, "CARTAO", 12, "BRONZE", "SUDESTE"));

        assertThat(resumo.valorParcela()).isEqualByComparingTo(new BigDecimal("39.10"));
        assertThat(resumo.totalFinal()).isEqualByComparingTo(new BigDecimal("469.20"));
        assertThat(resumo.ajustePagamento()).isEqualByComparingTo(new BigDecimal("55.40"));
    }

    @Test
    void parcelas_ausentes_valem_uma_vez() {
        ResumoResponse resumo = calculadora.calcular(pedido(List.of(CAMISETA),
                "RETIRADA_LOJA", null, "CARTAO", null, "BRONZE", "SUDESTE"));

        assertThat(resumo.parcelas()).isEqualTo(1);
        assertThat(resumo.valorParcela()).isEqualByComparingTo(resumo.totalFinal());
    }
}

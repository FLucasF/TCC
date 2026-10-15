package com.loja.checkout;

import static com.loja.checkout.ResumoDaCompraTest.item;
import static org.assertj.core.api.Assertions.assertThat;

import com.loja.checkout.api.ItemRequest;
import com.loja.checkout.api.ResumoRequest;
import com.loja.checkout.api.ResumoResponse;
import com.loja.checkout.aplicacao.CalculadoraDoResumo;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

/** Cupons, vantagens do clube e ajustes de pagamento fora dos exemplos. */
@SpringBootTest
class VantagensETaxasTest {

    private static final ItemRequest CAMISETA = item("Camiseta", "79.90", 2, "0.30");
    private static final ItemRequest TENIS = item("Tenis", "249.90", 1, "1.20");
    private static final ItemRequest VESTIDO = item("Vestido", "299.00", 2, "0.40");

    @Autowired
    private CalculadoraDoResumo calculadora;

    @Test
    void fretegratis_mostra_o_frete_e_desconta_o_mesmo_valor() {
        ResumoResponse resumo = calcular("EXPRESSA", "FRETEGRATIS", "PIX", 1, "BRONZE", "SUDESTE",
                CAMISETA, TENIS);

        assertThat(resumo.frete()).isEqualByComparingTo("33.10");
        assertThat(resumo.descontoCupom()).isEqualByComparingTo("33.10");
        // 409,70 - 33,10 + 33,10 + 4,10 = 413,80, menos 5% do Pix
        assertThat(resumo.totalFinal()).isEqualByComparingTo("393.11");
    }

    @Test
    void ouro_nao_paga_frete_entao_fretegratis_nao_desconta_nada() {
        ResumoResponse resumo = calcular("EXPRESSA", "FRETEGRATIS", "PIX", 1, "OURO", "SUDESTE",
                CAMISETA, TENIS);

        assertThat(resumo.frete()).isEqualByComparingTo("0.00");
        assertThat(resumo.descontoCupom()).isEqualByComparingTo("0.00");
    }

    @Test
    void ouro_acima_de_quinhentos_reais_em_produtos_ganha_brinde() {
        ResumoResponse resumo = calcular("RETIRADA_LOJA", null, "PIX", 1, "OURO", "SUL", VESTIDO);

        assertThat(resumo.subtotalProdutos()).isEqualByComparingTo("598.00");
        assertThat(resumo.brinde()).isTrue();
        assertThat(resumo.creditoProximaCompra()).isEqualByComparingTo("29.90");
    }

    @Test
    void prata_nao_ganha_brinde_nem_frete_gratis() {
        ResumoResponse resumo = calcular("EXPRESSA", null, "PIX", 1, "PRATA", "SUL", VESTIDO);

        assertThat(resumo.brinde()).isFalse();
        assertThat(resumo.frete()).isEqualByComparingTo("28.60");
        assertThat(resumo.creditoProximaCompra()).isEqualByComparingTo("11.96");
    }

    @Test
    void bronze_nao_ganha_credito() {
        ResumoResponse resumo = calcular("RETIRADA_LOJA", null, "PIX", 1, "BRONZE", "SUL", VESTIDO);

        assertThat(resumo.creditoProximaCompra()).isEqualByComparingTo("0.00");
        assertThat(resumo.brinde()).isFalse();
    }

    @Test
    void leve3pague2_da_uma_unidade_a_cada_tres_do_mesmo_item() {
        ResumoResponse resumo = calcular("RETIRADA_LOJA", "LEVE3PAGUE2", "PIX", 1, "BRONZE", "SUL",
                item("Meia", "19.90", 6, "0.10"), item("Camiseta", "79.90", 3, "0.30"));

        // 2 meias e 1 camiseta de graca
        assertThat(resumo.descontoCupom()).isEqualByComparingTo("119.70");
    }

    @Test
    void cartao_em_uma_vez_nao_tem_juros() {
        ResumoResponse resumo = calcular("RETIRADA_LOJA", null, "CARTAO", 1, "BRONZE", "SUDESTE",
                CAMISETA);

        assertThat(resumo.ajustePagamento()).isEqualByComparingTo("0.00");
        assertThat(resumo.totalFinal()).isEqualByComparingTo("161.40");
        assertThat(resumo.valorParcela()).isEqualByComparingTo("161.40");
    }

    @Test
    void cartao_em_doze_vezes_cobra_juros_de_tabela_price() {
        ResumoResponse resumo = calcular("RETIRADA_LOJA", null, "CARTAO", 12, "BRONZE", "SUDESTE",
                item("Sofa", "1000.00", 1, "20.00"));

        // total do pedido: 1000,00 + 10,00 de seguro
        assertThat(resumo.valorParcela()).isEqualByComparingTo("95.45");
        assertThat(resumo.totalFinal()).isEqualByComparingTo("1145.40");
        assertThat(resumo.ajustePagamento()).isEqualByComparingTo("135.40");
    }

    @Test
    void retirada_na_loja_nao_cobra_frete_mesmo_com_pedido_pesado() {
        ResumoResponse resumo = calcular("RETIRADA_LOJA", null, "PIX", 1, "BRONZE", "NORTE",
                item("Sofa", "1000.00", 1, "40.00"));

        assertThat(resumo.frete()).isEqualByComparingTo("0.00");
        assertThat(resumo.prazoEntregaDias()).isEqualTo(1);
    }

    @Test
    void sem_parcelas_informadas_o_pedido_e_a_vista() {
        ResumoResponse resumo = calcular("RETIRADA_LOJA", null, "CARTAO", null, "BRONZE", "SUDESTE",
                CAMISETA);

        assertThat(resumo.parcelas()).isEqualTo(1);
        assertThat(resumo.valorParcela()).isEqualByComparingTo(resumo.totalFinal());
    }

    private ResumoResponse calcular(String entrega, String cupom, String pagamento, Integer parcelas,
            String nivel, String regiao, ItemRequest... itens) {
        return calculadora.calcular(new ResumoRequest(
                List.of(itens), entrega, cupom, pagamento, parcelas, nivel, regiao));
    }
}

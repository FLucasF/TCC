package com.loja.checkout;

import com.loja.checkout.api.ResumoRequest;
import com.loja.checkout.api.ResumoRequest.ItemRequest;
import com.loja.checkout.api.ResumoResponse;
import com.loja.checkout.dominio.Dinheiro;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/** Regras que os exemplos do financeiro não cobrem. */
@ExtendWith(SpringExtension.class)
@SpringBootTest
class RegrasTest {

    @Autowired
    private CalculadoraResumo calculadora;

    private static ItemRequest item(String nome, String preco, int quantidade, String peso) {
        return new ItemRequest(nome, new BigDecimal(preco), quantidade, new BigDecimal(peso));
    }

    private ResumoResponse calcular(List<ItemRequest> itens, String entrega, String cupom,
                                    String pagamento, Integer parcelas, String clube, String regiao) {
        return calculadora.calcular(new ResumoRequest(itens, entrega, cupom, pagamento, parcelas,
                clube, regiao));
    }

    @Test
    void arredondamentoMeioParaOPar() {
        assertThat(Dinheiro.centavos(new BigDecimal("2.995"))).isEqualByComparingTo("3.00");
        assertThat(Dinheiro.centavos(new BigDecimal("2.985"))).isEqualByComparingTo("2.98");
    }

    @Test
    void freteGratisDescontaExatamenteOFrete() {
        // Expressa com 1,8 kg: 25 + 4,50 × 1,8 = 33,10
        ResumoResponse r = calcular(List.of(item("Camiseta", "79.90", 2, "0.30"),
                        item("Tenis", "249.90", 1, "1.20")),
                "EXPRESSA", "FRETEGRATIS", "PIX", 1, "BRONZE", "SUDESTE");

        assertThat(r.frete()).isEqualByComparingTo("33.10");
        assertThat(r.descontoCupom()).isEqualByComparingTo("33.10");
        // 409,70 − 33,10 + 33,10 + 4,10 = 413,80, menos 5% de Pix
        assertThat(r.totalFinal()).isEqualByComparingTo("393.11");
    }

    @Test
    void ouroNaoPagaFreteEntaoFreteGratisNaoDescontaNada() {
        ResumoResponse r = calcular(List.of(item("Camiseta", "79.90", 2, "0.30")),
                "EXPRESSA", "FRETEGRATIS", "PIX", 1, "OURO", "SUDESTE");

        assertThat(r.frete()).isEqualByComparingTo("0.00");
        assertThat(r.descontoCupom()).isEqualByComparingTo("0.00");
    }

    @Test
    void ouroGanhaBrindeAcimaDeQuinhentosEmProdutos() {
        ResumoResponse acima = calcular(List.of(item("Tenis", "250.10", 2, "1.20")),
                "RETIRADA_LOJA", null, "PIX", 1, "OURO", "SUDESTE");
        ResumoResponse exatos = calcular(List.of(item("Tenis", "250.00", 2, "1.20")),
                "RETIRADA_LOJA", null, "PIX", 1, "OURO", "SUDESTE");

        assertThat(acima.brinde()).isTrue();
        assertThat(exatos.brinde()).isFalse();
        assertThat(acima.creditoProximaCompra()).isEqualByComparingTo("25.01");
    }

    @Test
    void prataGanhaDoisPorCentoEBronzeNaoGanhaNada() {
        assertThat(calcular(List.of(item("Camiseta", "79.90", 2, "0.30")), "RETIRADA_LOJA", null,
                "PIX", 1, "PRATA", "SUL").creditoProximaCompra()).isEqualByComparingTo("3.20");
        assertThat(calcular(List.of(item("Camiseta", "79.90", 2, "0.30")), "RETIRADA_LOJA", null,
                "PIX", 1, "BRONZE", "SUL").creditoProximaCompra()).isEqualByComparingTo("0.00");
    }

    @Test
    void seguroPorRegiaoSobreOsProdutosSemDescontoESemFrete() {
        List<ItemRequest> itens = List.of(item("Tenis", "200.00", 1, "1.00"));

        assertThat(calcular(itens, "EXPRESSA", "BEMVINDO10", "PIX", 1, "BRONZE", "SUDESTE").seguro())
                .isEqualByComparingTo("2.00");
        assertThat(calcular(itens, "EXPRESSA", null, "PIX", 1, "BRONZE", "SUL").seguro())
                .isEqualByComparingTo("2.00");
        assertThat(calcular(itens, "EXPRESSA", null, "PIX", 1, "BRONZE", "CENTRO_OESTE").seguro())
                .isEqualByComparingTo("3.00");
        assertThat(calcular(itens, "EXPRESSA", null, "PIX", 1, "BRONZE", "NORTE").seguro())
                .isEqualByComparingTo("5.00");
        assertThat(calcular(itens, "EXPRESSA", null, "PIX", 1, "BRONZE", "NORDESTE").seguro())
                .isEqualByComparingTo("4.00");
    }

    @Test
    void cartaoSemJurosAteTresVezes() {
        ResumoResponse r = calcular(List.of(item("Tenis", "100.00", 1, "1.00")),
                "RETIRADA_LOJA", null, "CARTAO", 3, "BRONZE", "SUDESTE");

        // 100,00 + seguro 1,00 = 101,00
        assertThat(r.ajustePagamento()).isEqualByComparingTo("0.00");
        assertThat(r.totalFinal()).isEqualByComparingTo("101.00");
        assertThat(r.valorParcela()).isEqualByComparingTo("33.67");
    }

    @Test
    void cartaoComJurosDaQuartaParcelaEmDiante() {
        ResumoResponse r = calcular(List.of(item("Tenis", "100.00", 1, "1.00")),
                "RETIRADA_LOJA", null, "CARTAO", 4, "BRONZE", "SUDESTE");

        // Price sobre 101,00 em 4x a 1,99%: parcela 26,52
        assertThat(r.valorParcela()).isEqualByComparingTo("26.52");
        assertThat(r.totalFinal()).isEqualByComparingTo("106.08");
        assertThat(r.ajustePagamento()).isEqualByComparingTo("5.08");
        assertThat(r.parcelas()).isEqualTo(4);
    }

    @Test
    void cartaoEmDozeVezes() {
        ResumoResponse r = calcular(List.of(item("Tenis", "100.00", 1, "1.00")),
                "RETIRADA_LOJA", null, "CARTAO", 12, "BRONZE", "SUDESTE");

        // Price sobre 101,00 em 12x a 1,99%: parcela 9,54
        assertThat(r.valorParcela()).isEqualByComparingTo("9.54");
        assertThat(r.totalFinal()).isEqualByComparingTo("114.48");
        assertThat(r.ajustePagamento()).isEqualByComparingTo("13.48");
    }

    @Test
    void parcelasAusentesValeUma() {
        ResumoResponse r = calcular(List.of(item("Tenis", "100.00", 1, "1.00")),
                "RETIRADA_LOJA", null, "CARTAO", null, "BRONZE", "SUDESTE");

        assertThat(r.parcelas()).isEqualTo(1);
        assertThat(r.valorParcela()).isEqualByComparingTo("101.00");
    }

    @Test
    void leve3pague2ContaPorItemDoCarrinho() {
        ResumoResponse r = calcular(List.of(item("Meia", "10.00", 6, "0.10"),
                        item("Boné", "30.00", 2, "0.20")),
                "RETIRADA_LOJA", "LEVE3PAGUE2", "PIX", 1, "BRONZE", "SUDESTE");

        // 6 meias dão 2 de graça (20,00); 2 bonés não dão nada
        assertThat(r.descontoCupom()).isEqualByComparingTo("20.00");
    }

    @Test
    void leve3pague2ContaCadaLinhaDoCarrinhoSeparado() {
        ResumoResponse r = calcular(List.of(item("Meia", "10.00", 2, "0.10"),
                        item("Meia", "10.00", 2, "0.10")),
                "RETIRADA_LOJA", "LEVE3PAGUE2", "PIX", 1, "BRONZE", "SUDESTE");

        // duas linhas de 2 unidades: nenhuma linha chega a 3, então não há unidade grátis
        assertThat(r.descontoCupom()).isEqualByComparingTo("0.00");
    }

    @Test
    void prazoVemDaModalidadeEscolhida() {
        List<ItemRequest> itens = List.of(item("Camiseta", "79.90", 1, "0.30"));

        assertThat(calcular(itens, "ECONOMICA", null, "PIX", 1, "BRONZE", "SUDESTE").prazoEntregaDias())
                .isEqualTo(7);
        assertThat(calcular(itens, "EXPRESSA", null, "PIX", 1, "BRONZE", "SUDESTE").prazoEntregaDias())
                .isEqualTo(2);
        assertThat(calcular(itens, "RETIRADA_LOJA", null, "PIX", 1, "BRONZE", "SUDESTE").prazoEntregaDias())
                .isEqualTo(1);
        assertThat(calcular(itens, "MOTOBOY", null, "PIX", 1, "BRONZE", "SUDESTE").prazoEntregaDias())
                .isEqualTo(0);
    }

    @Test
    void ouroMantemOPrazoMesmoSemPagarFrete() {
        ResumoResponse r = calcular(List.of(item("Camiseta", "79.90", 1, "0.30")),
                "ECONOMICA", null, "PIX", 1, "OURO", "SUDESTE");

        assertThat(r.frete()).isEqualByComparingTo("0.00");
        assertThat(r.prazoEntregaDias()).isEqualTo(7);
    }
}

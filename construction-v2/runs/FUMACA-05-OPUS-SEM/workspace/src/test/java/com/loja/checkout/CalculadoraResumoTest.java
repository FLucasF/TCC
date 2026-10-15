package com.loja.checkout;

import com.loja.checkout.api.CalculadoraResumo;
import com.loja.checkout.api.ItemRequisicao;
import com.loja.checkout.api.RequisicaoResumo;
import com.loja.checkout.api.RespostaResumo;
import com.loja.checkout.cupom.Bemvindo10;
import com.loja.checkout.cupom.CatalogoCupons;
import com.loja.checkout.cupom.FreteGratis;
import com.loja.checkout.cupom.Leve3Pague2;
import com.loja.checkout.cupom.Menos50;
import com.loja.checkout.dominio.CheckoutException;
import com.loja.checkout.dominio.ErroCheckout;
import com.loja.checkout.entrega.CatalogoEntregas;
import com.loja.checkout.entrega.EntregaEconomica;
import com.loja.checkout.entrega.EntregaExpressa;
import com.loja.checkout.entrega.Motoboy;
import com.loja.checkout.entrega.RetiradaLoja;
import com.loja.checkout.pagamento.CatalogoPagamentos;
import com.loja.checkout.pagamento.PagamentoBoleto;
import com.loja.checkout.pagamento.PagamentoCartao;
import com.loja.checkout.pagamento.PagamentoPix;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class CalculadoraResumoTest {

    private final CalculadoraResumo calculadora = new CalculadoraResumo(
            new CatalogoEntregas(List.of(
                    new EntregaEconomica(), new EntregaExpressa(), new RetiradaLoja(), new Motoboy())),
            new CatalogoCupons(List.of(
                    new Bemvindo10(), new Menos50(), new FreteGratis(), new Leve3Pague2())),
            new CatalogoPagamentos(List.of(
                    new PagamentoPix(), new PagamentoCartao(), new PagamentoBoleto())));

    private static ItemRequisicao item(String nome, String preco, int quantidade, String peso) {
        return new ItemRequisicao(nome, new BigDecimal(preco), quantidade, new BigDecimal(peso));
    }

    private static ItemRequisicao camiseta() {
        return item("Camiseta", "79.90", 2, "0.30");
    }

    private static ItemRequisicao tenis() {
        return item("Tenis", "249.90", 1, "1.20");
    }

    private RespostaResumo calcular(List<ItemRequisicao> itens, String entrega, String cupom,
                                    String pagamento, Integer parcelas) {
        return calculadora.calcular(
                new RequisicaoResumo(itens, entrega, cupom, pagamento, parcelas));
    }

    private void esperaErro(ErroCheckout erro, List<ItemRequisicao> itens, String entrega,
                            String cupom, String pagamento, Integer parcelas) {
        assertThatThrownBy(() -> calcular(itens, entrega, cupom, pagamento, parcelas))
                .isInstanceOf(CheckoutException.class)
                .extracting(excecao -> ((CheckoutException) excecao).getErro())
                .isEqualTo(erro);
    }

    private static void confere(RespostaResumo resumo, String subtotal, String cupom, String frete,
                                int prazo, String ajuste, String total, int parcelas, String parcela) {
        assertThat(resumo.subtotalProdutos()).isEqualByComparingTo(subtotal);
        assertThat(resumo.descontoCupom()).isEqualByComparingTo(cupom);
        assertThat(resumo.frete()).isEqualByComparingTo(frete);
        assertThat(resumo.prazoEntregaDias()).isEqualTo(prazo);
        assertThat(resumo.ajustePagamento()).isEqualByComparingTo(ajuste);
        assertThat(resumo.totalFinal()).isEqualByComparingTo(total);
        assertThat(resumo.parcelas()).isEqualTo(parcelas);
        assertThat(resumo.valorParcela()).isEqualByComparingTo(parcela);
    }

    @Test
    @DisplayName("Exemplo 1 do financeiro: expressa + BEMVINDO10 + Pix")
    void exemplo1() {
        RespostaResumo resumo = calcular(
                List.of(camiseta(), tenis()), "EXPRESSA", "BEMVINDO10", "PIX", 1);
        confere(resumo, "409.70", "40.97", "33.10", 2, "-20.09", "381.74", 1, "381.74");
    }

    @Test
    @DisplayName("Exemplo 2 do financeiro: economica, sem cupom, cartao em 6x com juros")
    void exemplo2() {
        RespostaResumo resumo = calcular(
                List.of(camiseta(), tenis()), "ECONOMICA", null, "CARTAO", 6);
        confere(resumo, "409.70", "0.00", "15.60", 7, "30.10", "455.40", 6, "75.90");
    }

    @Test
    @DisplayName("Exemplo 3 do financeiro: motoboy + MENOS50 + boleto")
    void exemplo3() {
        RespostaResumo resumo = calcular(
                List.of(item("Fone", "199.90", 2, "0.25")), "MOTOBOY", "MENOS50", "BOLETO", 1);
        confere(resumo, "399.80", "50.00", "18.00", 0, "3.49", "371.29", 1, "371.29");
    }

    @Test
    @DisplayName("Exemplo 4 do financeiro: retirada + LEVE3PAGUE2 + cartao em 3x sem juros")
    void exemplo4() {
        RespostaResumo resumo = calcular(
                List.of(item("Meia", "19.90", 7, "0.10"), camiseta()),
                "RETIRADA_LOJA", "LEVE3PAGUE2", "CARTAO", 3);
        confere(resumo, "299.10", "39.80", "0.00", 1, "0.00", "259.30", 3, "86.43");
    }

    @Test
    @DisplayName("FRETEGRATIS zera o frete mantendo o valor no resumo")
    void freteGratis() {
        RespostaResumo resumo = calcular(
                List.of(camiseta(), tenis()), "EXPRESSA", "FRETEGRATIS", "CARTAO", 1);
        confere(resumo, "409.70", "33.10", "33.10", 2, "0.00", "409.70", 1, "409.70");
    }

    @Test
    @DisplayName("Sem cupom e sem parcelas informadas o pedido sai a vista")
    void semCupomSemParcelas() {
        RespostaResumo resumo = calculadora.calcular(new RequisicaoResumo(
                List.of(camiseta()), "RETIRADA_LOJA", null, "CARTAO", null));
        confere(resumo, "159.80", "0.00", "0.00", 1, "0.00", "159.80", 1, "159.80");
    }

    @Test
    @DisplayName("Arredondamento meio para o par nos centavos")
    void arredondamentoMeioParaOPar() {
        // 29,95 x 1 com 10% = 2,995 -> 3,00 (arredonda para o par de cima)
        assertThat(calcular(List.of(item("Bone", "29.95", 1, "0.10")),
                "RETIRADA_LOJA", "BEMVINDO10", "CARTAO", 1).descontoCupom())
                .isEqualByComparingTo("3.00");
        // 29,85 x 1 com 10% = 2,985 -> 2,98 (arredonda para o par de baixo)
        assertThat(calcular(List.of(item("Bone", "29.85", 1, "0.10")),
                "RETIRADA_LOJA", "BEMVINDO10", "CARTAO", 1).descontoCupom())
                .isEqualByComparingTo("2.98");
    }

    @Test
    @DisplayName("Peso do pedido soma peso x quantidade sem arredondar")
    void pesoDoPedido() {
        // 3 x 0,333 kg = 0,999 kg -> 12,00 + 2,00 x 0,999 = 13,998 -> 14,00
        assertThat(calcular(List.of(item("Lenco", "10.00", 3, "0.333")),
                "ECONOMICA", null, "PIX", 1).frete()).isEqualByComparingTo("14.00");
    }

    @Test
    @DisplayName("LEVE3PAGUE2 conta as unidades gratis item a item")
    void leve3Pague2PorItem() {
        // 7 meias -> 2 gratis; 2 camisetas -> nenhuma gratis
        assertThat(calcular(List.of(item("Meia", "19.90", 7, "0.10"), camiseta()),
                "RETIRADA_LOJA", "LEVE3PAGUE2", "PIX", 1).descontoCupom())
                .isEqualByComparingTo("39.80");
    }

    @Test
    @DisplayName("Carrinho vazio, ausente ou com item invalido e PEDIDO_INVALIDO")
    void pedidoInvalido() {
        esperaErro(ErroCheckout.PEDIDO_INVALIDO, List.of(), "EXPRESSA", null, "PIX", 1);
        esperaErro(ErroCheckout.PEDIDO_INVALIDO, null, "EXPRESSA", null, "PIX", 1);
        esperaErro(ErroCheckout.PEDIDO_INVALIDO,
                List.of(item("Camiseta", "0.00", 2, "0.30")), "EXPRESSA", null, "PIX", 1);
        esperaErro(ErroCheckout.PEDIDO_INVALIDO,
                List.of(item("Camiseta", "79.90", 0, "0.30")), "EXPRESSA", null, "PIX", 1);
        esperaErro(ErroCheckout.PEDIDO_INVALIDO,
                List.of(item("Camiseta", "79.90", 2, "-0.30")), "EXPRESSA", null, "PIX", 1);
        esperaErro(ErroCheckout.PEDIDO_INVALIDO,
                Arrays.asList(new ItemRequisicao("Camiseta", null, 2, new BigDecimal("0.30"))),
                "EXPRESSA", null, "PIX", 1);
        esperaErro(ErroCheckout.PEDIDO_INVALIDO,
                Arrays.asList(new ItemRequisicao("Camiseta", new BigDecimal("79.90"), null,
                        new BigDecimal("0.30"))), "EXPRESSA", null, "PIX", 1);
        esperaErro(ErroCheckout.PEDIDO_INVALIDO,
                Arrays.asList(new ItemRequisicao("Camiseta", new BigDecimal("79.90"), 2, null)),
                "EXPRESSA", null, "PIX", 1);
    }

    @Test
    @DisplayName("Entrega inexistente ou ausente e MODALIDADE_INVALIDA")
    void modalidadeInvalida() {
        esperaErro(ErroCheckout.MODALIDADE_INVALIDA, List.of(camiseta()), "DRONE", null, "PIX", 1);
        esperaErro(ErroCheckout.MODALIDADE_INVALIDA, List.of(camiseta()), null, null, "PIX", 1);
        esperaErro(ErroCheckout.MODALIDADE_INVALIDA, List.of(camiseta()), "expressa", null, "PIX", 1);
    }

    @Test
    @DisplayName("Motoboy acima de 5 kg e MODALIDADE_INDISPONIVEL")
    void modalidadeIndisponivel() {
        esperaErro(ErroCheckout.MODALIDADE_INDISPONIVEL,
                List.of(item("Mochila", "50.00", 3, "2.00")), "MOTOBOY", null, "PIX", 1);
        // exatamente 5 kg ainda vale
        assertThat(calcular(List.of(item("Mochila", "50.00", 5, "1.00")),
                "MOTOBOY", null, "PIX", 1).frete()).isEqualByComparingTo("18.00");
    }

    @Test
    @DisplayName("Cupom inexistente e CUPOM_INVALIDO")
    void cupomInvalido() {
        esperaErro(ErroCheckout.CUPOM_INVALIDO, List.of(camiseta()), "EXPRESSA", "NATAL2020", "PIX", 1);
        esperaErro(ErroCheckout.CUPOM_INVALIDO, List.of(camiseta()), "EXPRESSA", "bemvindo10", "PIX", 1);
    }

    @Test
    @DisplayName("MENOS50 abaixo de R$ 300,00 em produtos e CUPOM_NAO_APLICAVEL")
    void cupomNaoAplicavel() {
        esperaErro(ErroCheckout.CUPOM_NAO_APLICAVEL,
                List.of(item("Meia", "19.90", 5, "0.10")), "EXPRESSA", "MENOS50", "PIX", 1);
        // exatamente R$ 300,00 ja vale
        assertThat(calcular(List.of(item("Camisa", "100.00", 3, "0.30")),
                "RETIRADA_LOJA", "MENOS50", "PIX", 1).descontoCupom()).isEqualByComparingTo("50.00");
    }

    @Test
    @DisplayName("Forma de pagamento inexistente ou ausente e FORMA_PAGAMENTO_INVALIDA")
    void formaPagamentoInvalida() {
        esperaErro(ErroCheckout.FORMA_PAGAMENTO_INVALIDA,
                List.of(camiseta()), "EXPRESSA", null, "DINHEIRO", 1);
        esperaErro(ErroCheckout.FORMA_PAGAMENTO_INVALIDA,
                List.of(camiseta()), "EXPRESSA", null, null, 1);
    }

    @Test
    @DisplayName("Parcelamento fora do permitido e PARCELAMENTO_INVALIDO")
    void parcelamentoInvalido() {
        esperaErro(ErroCheckout.PARCELAMENTO_INVALIDO, List.of(camiseta()), "EXPRESSA", null, "PIX", 2);
        esperaErro(ErroCheckout.PARCELAMENTO_INVALIDO, List.of(camiseta()), "EXPRESSA", null, "BOLETO", 3);
        esperaErro(ErroCheckout.PARCELAMENTO_INVALIDO, List.of(camiseta()), "EXPRESSA", null, "CARTAO", 13);
        esperaErro(ErroCheckout.PARCELAMENTO_INVALIDO, List.of(camiseta()), "EXPRESSA", null, "CARTAO", 0);
    }

    @Test
    @DisplayName("Boleto acima de R$ 1.000,00 e FORMA_PAGAMENTO_INDISPONIVEL")
    void formaPagamentoIndisponivel() {
        esperaErro(ErroCheckout.FORMA_PAGAMENTO_INDISPONIVEL,
                List.of(item("Casaco", "600.00", 2, "0.50")), "RETIRADA_LOJA", null, "BOLETO", 1);
        // exatamente R$ 1.000,00 ainda passa
        assertThat(calcular(List.of(item("Casaco", "500.00", 2, "0.50")),
                "RETIRADA_LOJA", null, "BOLETO", 1).totalFinal()).isEqualByComparingTo("1003.49");
    }

    @Test
    @DisplayName("Os erros saem na ordem combinada com o desenvolvedor")
    void ordemDosErros() {
        // carrinho invalido ganha da modalidade invalida
        esperaErro(ErroCheckout.PEDIDO_INVALIDO, List.of(), "DRONE", "NATAL2020", "DINHEIRO", 9);
        // modalidade invalida ganha do cupom invalido
        esperaErro(ErroCheckout.MODALIDADE_INVALIDA,
                List.of(camiseta()), "DRONE", "NATAL2020", "DINHEIRO", 9);
        // modalidade indisponivel ganha do cupom invalido
        esperaErro(ErroCheckout.MODALIDADE_INDISPONIVEL,
                List.of(item("Mochila", "50.00", 3, "2.00")), "MOTOBOY", "NATAL2020", "DINHEIRO", 9);
        // cupom invalido ganha do cupom nao aplicavel e da forma de pagamento
        esperaErro(ErroCheckout.CUPOM_INVALIDO,
                List.of(camiseta()), "EXPRESSA", "NATAL2020", "DINHEIRO", 9);
        // cupom nao aplicavel ganha da forma de pagamento invalida
        esperaErro(ErroCheckout.CUPOM_NAO_APLICAVEL,
                List.of(camiseta()), "EXPRESSA", "MENOS50", "DINHEIRO", 9);
        // forma de pagamento invalida ganha do parcelamento
        esperaErro(ErroCheckout.FORMA_PAGAMENTO_INVALIDA,
                List.of(camiseta()), "EXPRESSA", null, "DINHEIRO", 9);
        // parcelamento invalido ganha da indisponibilidade do boleto
        esperaErro(ErroCheckout.PARCELAMENTO_INVALIDO,
                List.of(item("Casaco", "600.00", 2, "0.50")), "RETIRADA_LOJA", null, "BOLETO", 2);
    }

    @Test
    @DisplayName("Cartao de 4x a 12x usa a tabela Price")
    void cartaoComJuros() {
        // total 100,00 em 12x: parcela 9,45 -> total final 113,40
        RespostaResumo resumo = calcular(List.of(item("Bone", "100.00", 1, "0.10")),
                "RETIRADA_LOJA", null, "CARTAO", 12);
        confere(resumo, "100.00", "0.00", "0.00", 1, "13.40", "113.40", 12, "9.45");
    }
}

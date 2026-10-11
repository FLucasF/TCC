package com.loja.resumocompra;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;
import java.util.List;

import org.junit.jupiter.api.Test;

class ResumoCompraServiceTest {

    private final ResumoCompraService service = new ResumoCompraService();

    @Test
    void exemplo1PixComCupomBemVindoNoNorte() {
        var resumo = service.calcular(pedido(
                List.of(camiseta(2), tenis(1)), "EXPRESSA", "BEMVINDO10", "PIX", null, "BRONZE", "NORTE"));

        assertThat(resumo).isEqualTo(new ResumoResponse(
                d("409.70"), d("40.97"), d("33.10"), 2, d("10.24"), d("-20.60"), d("391.47"), 1, d("391.47"), d("0.00"), false));
    }

    @Test
    void exemplo2CartaoSeisVezesComJurosNoCentroOeste() {
        var resumo = service.calcular(pedido(
                List.of(camiseta(2), tenis(1)), "ECONOMICA", null, "CARTAO", 6, "PRATA", "CENTRO_OESTE"));

        assertThat(resumo).isEqualTo(new ResumoResponse(
                d("409.70"), d("0.00"), d("15.60"), 7, d("6.15"), d("30.55"), d("462.00"), 6, d("77.00"), d("8.19"), false));
    }

    @Test
    void exemplo3BoletoComMenos50NoNordeste() {
        var resumo = service.calcular(pedido(
                List.of(fone(2)), "MOTOBOY", "MENOS50", "BOLETO", null, "BRONZE", "NORDESTE"));

        assertThat(resumo).isEqualTo(new ResumoResponse(
                d("399.80"), d("50.00"), d("18.00"), 0, d("8.00"), d("3.49"), d("379.29"), 1, d("379.29"), d("0.00"), false));
    }

    @Test
    void exemplo4CartaoTresVezesSemJurosComLeve3Pague2NoSul() {
        var resumo = service.calcular(pedido(
                List.of(meia(7), camiseta(2)), "RETIRADA_LOJA", "LEVE3PAGUE2", "CARTAO", 3, "PRATA", "SUL"));

        assertThat(resumo).isEqualTo(new ResumoResponse(
                d("299.10"), d("39.80"), d("0.00"), 1, d("2.99"), d("0.00"), d("262.29"), 3, d("87.43"), d("5.98"), false));
    }

    @Test
    void exemplo5PixOuroSemFreteNoSudeste() {
        var resumo = service.calcular(pedido(
                List.of(camiseta(2), tenis(1)), "EXPRESSA", null, "PIX", null, "OURO", "SUDESTE"));

        assertThat(resumo).isEqualTo(new ResumoResponse(
                d("409.70"), d("0.00"), d("0.00"), 2, d("4.10"), d("-20.69"), d("393.11"), 1, d("393.11"), d("20.48"), false));
    }

    @Test
    void ouroComProdutosAcimaDeQuinhentosGanhaBrinde() {
        var resumo = service.calcular(pedido(
                List.of(new ItemPedido("Relogio", d("600.00"), 1, d("0.10"))), "RETIRADA_LOJA", null, "PIX", null, "OURO", "SUDESTE"));

        assertThat(resumo.brinde()).isTrue();
        assertThat(resumo.creditoProximaCompra()).isEqualTo(d("30.00"));
    }

    @Test
    void freteGratisDoCupomIgualaAoFrete() {
        var resumo = service.calcular(pedido(
                List.of(camiseta(1)), "ECONOMICA", "FRETEGRATIS", "PIX", null, "BRONZE", "SUDESTE"));

        assertThat(resumo.descontoCupom()).isEqualTo(resumo.frete());
        assertThat(resumo.frete()).isEqualTo(d("12.60"));
    }

    @Test
    void motoboyAcimaDeCincoKgEhIndisponivel() {
        assertErro(pedido(List.of(new ItemPedido("Jaqueta", d("100.00"), 6, d("1.00"))),
                "MOTOBOY", null, "PIX", null, "BRONZE", "SUDESTE"), CodigoErro.MODALIDADE_INDISPONIVEL);
    }

    @Test
    void menos50AbaixoDeTrezentosNaoEhAplicavel() {
        assertErro(pedido(List.of(camiseta(1)), "EXPRESSA", "MENOS50", "PIX", null, "BRONZE", "SUDESTE"),
                CodigoErro.CUPOM_NAO_APLICAVEL);
    }

    @Test
    void leve3Pague2SemTresUnidadesNaoEhAplicavel() {
        assertErro(pedido(List.of(camiseta(2)), "EXPRESSA", "LEVE3PAGUE2", "PIX", null, "BRONZE", "SUDESTE"),
                CodigoErro.CUPOM_NAO_APLICAVEL);
    }

    @Test
    void boletoAcimaDeMilNaoEhAceito() {
        assertErro(pedido(List.of(new ItemPedido("Casaco", d("1200.00"), 1, d("0.50"))),
                "RETIRADA_LOJA", null, "BOLETO", null, "BRONZE", "SUDESTE"), CodigoErro.FORMA_PAGAMENTO_INDISPONIVEL);
    }

    @Test
    void pixNaoParcela() {
        assertErro(pedido(List.of(camiseta(1)), "EXPRESSA", null, "PIX", 2, "BRONZE", "SUDESTE"),
                CodigoErro.PARCELAMENTO_INVALIDO);
    }

    @Test
    void cartaoAceitaNoMaximoDozeParcelas() {
        assertErro(pedido(List.of(camiseta(1)), "EXPRESSA", null, "CARTAO", 13, "BRONZE", "SUDESTE"),
                CodigoErro.PARCELAMENTO_INVALIDO);
    }

    @Test
    void carrinhoVazioEhPedidoInvalido() {
        assertErro(pedido(List.of(), "EXPRESSA", null, "PIX", null, "BRONZE", "SUDESTE"), CodigoErro.PEDIDO_INVALIDO);
    }

    @Test
    void itemComPesoAusenteEhPedidoInvalido() {
        assertErro(pedido(List.of(new ItemPedido("Meia", d("19.90"), 1, null)),
                "EXPRESSA", null, "PIX", null, "BRONZE", "SUDESTE"), CodigoErro.PEDIDO_INVALIDO);
    }

    @Test
    void nivelInvalidoTemPrecedenciaSobreRegiaoEModalidade() {
        assertErro(pedido(List.of(camiseta(1)), "NAO_EXISTE", null, "PIX", null, "PLATINA", "MARTE"),
                CodigoErro.NIVEL_CLUBE_INVALIDO);
    }

    @Test
    void modalidadeIndisponivelTemPrecedenciaSobreCupomInvalido() {
        assertErro(pedido(List.of(new ItemPedido("Jaqueta", d("100.00"), 6, d("1.00"))),
                "MOTOBOY", "CUPOM_FALSO", "PIX", null, "BRONZE", "SUDESTE"), CodigoErro.MODALIDADE_INDISPONIVEL);
    }

    @Test
    void cupomNaoAplicavelTemPrecedenciaSobreFormaInvalida() {
        assertErro(pedido(List.of(camiseta(1)), "EXPRESSA", "MENOS50", "BITCOIN", null, "BRONZE", "SUDESTE"),
                CodigoErro.CUPOM_NAO_APLICAVEL);
    }

    @Test
    void cupomNaoMaiusculoEhInvalido() {
        assertErro(pedido(List.of(camiseta(1)), "EXPRESSA", "bemvindo10", "PIX", null, "BRONZE", "SUDESTE"),
                CodigoErro.CUPOM_INVALIDO);
    }

    private void assertErro(PedidoRequest pedido, CodigoErro codigo) {
        assertThatThrownBy(() -> service.calcular(pedido))
                .isInstanceOfSatisfying(ErroPedido.class, erro -> assertThat(erro.getCodigo()).isEqualTo(codigo));
    }

    private static PedidoRequest pedido(List<ItemPedido> itens, String modalidade, String cupom, String forma,
                                        Integer parcelas, String nivel, String regiao) {
        return new PedidoRequest(itens, modalidade, cupom, forma, parcelas, nivel, regiao);
    }

    private static ItemPedido camiseta(int quantidade) {
        return new ItemPedido("Camiseta", d("79.90"), quantidade, d("0.30"));
    }

    private static ItemPedido tenis(int quantidade) {
        return new ItemPedido("Tenis", d("249.90"), quantidade, d("1.20"));
    }

    private static ItemPedido fone(int quantidade) {
        return new ItemPedido("Fone", d("199.90"), quantidade, d("0.25"));
    }

    private static ItemPedido meia(int quantidade) {
        return new ItemPedido("Meia", d("19.90"), quantidade, d("0.10"));
    }

    private static BigDecimal d(String valor) {
        return new BigDecimal(valor);
    }
}

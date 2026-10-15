package com.loja.checkout;

import static com.loja.checkout.Pedidos.CAMISETA;
import static com.loja.checkout.Pedidos.MEIA;
import static com.loja.checkout.Pedidos.TENIS;
import static com.loja.checkout.Pedidos.item;
import static com.loja.checkout.Pedidos.pedido;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.loja.checkout.aplicacao.CodigoErro;
import com.loja.checkout.aplicacao.ErroDeNegocio;
import com.loja.checkout.api.ResumoRequest;
import com.loja.checkout.api.ResumoResponse;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit.jupiter.SpringExtension;

@SpringBootTest
@ExtendWith(SpringExtension.class)
class RegrasDeNegocioTest {

    @Autowired
    private com.loja.checkout.aplicacao.ResumoCheckoutService service;

    private void esperaErro(ResumoRequest request, CodigoErro esperado) {
        assertThatThrownBy(() -> service.calcular(request))
                .isInstanceOf(ErroDeNegocio.class)
                .extracting(erro -> ((ErroDeNegocio) erro).codigo())
                .isEqualTo(esperado);
    }

    // --- Entrega ---

    @Test
    void freteEconomicoCobraPorKgDoPedido() {
        ResumoResponse resumo = service.calcular(pedido(CAMISETA, TENIS).entrega("ECONOMICA").montar());

        assertThat(resumo.frete()).isEqualByComparingTo("15.60");
        assertThat(resumo.prazoEntregaDias()).isEqualTo(7);
    }

    @Test
    void motoboyNaoAtendeAcimaDeCincoQuilos() {
        esperaErro(pedido(item("Halteres", "100.00", 1, "5.01")).entrega("MOTOBOY").montar(),
                CodigoErro.MODALIDADE_INDISPONIVEL);
    }

    @Test
    void motoboyAtendeExatamenteCincoQuilos() {
        ResumoResponse resumo = service.calcular(
                pedido(item("Halteres", "100.00", 1, "5.00")).entrega("MOTOBOY").montar());

        assertThat(resumo.frete()).isEqualByComparingTo("18.00");
        assertThat(resumo.prazoEntregaDias()).isZero();
    }

    // --- Cupons ---

    @Test
    void menos50ExigeTrezentosReaisEmProdutos() {
        esperaErro(pedido(item("Camisa", "299.99", 1, "0.20")).cupom("MENOS50").montar(),
                CodigoErro.CUPOM_NAO_APLICAVEL);

        ResumoResponse resumo = service.calcular(
                pedido(item("Camisa", "300.00", 1, "0.20")).cupom("MENOS50").montar());
        assertThat(resumo.descontoCupom()).isEqualByComparingTo("50.00");
    }

    @Test
    void freteGratisDescontaExatamenteOValorDoFrete() {
        ResumoResponse resumo = service.calcular(
                pedido(CAMISETA, TENIS).entrega("EXPRESSA").cupom("FRETEGRATIS").montar());

        assertThat(resumo.frete()).isEqualByComparingTo("33.10");
        assertThat(resumo.descontoCupom()).isEqualByComparingTo("33.10");
    }

    @Test
    void leve3Pague2DaUmaUnidadeGratisACadaTresDoMesmoItem() {
        ResumoResponse resumo = service.calcular(pedido(MEIA, CAMISETA).cupom("LEVE3PAGUE2").montar());

        assertThat(resumo.descontoCupom()).isEqualByComparingTo("39.80");
    }

    @Test
    void cupomEmLetraMinusculaNaoExiste() {
        esperaErro(pedido(CAMISETA).cupom("bemvindo10").montar(), CodigoErro.CUPOM_INVALIDO);
    }

    @Test
    void pedidoSemCupomNaoTemDesconto() {
        assertThat(service.calcular(pedido(CAMISETA).montar()).descontoCupom())
                .isEqualByComparingTo("0.00");
    }

    // --- Clube ---

    @Test
    void prataGanhaDoisPorCentoDeCredito() {
        ResumoResponse resumo = service.calcular(pedido(CAMISETA, TENIS).clube("PRATA").montar());

        assertThat(resumo.creditoProximaCompra()).isEqualByComparingTo("8.19");
        assertThat(resumo.brinde()).isFalse();
    }

    @Test
    void bronzeNaoGanhaNada() {
        ResumoResponse resumo = service.calcular(
                pedido(CAMISETA, TENIS).entrega("EXPRESSA").clube("BRONZE").montar());

        assertThat(resumo.creditoProximaCompra()).isEqualByComparingTo("0.00");
        assertThat(resumo.frete()).isEqualByComparingTo("33.10");
        assertThat(resumo.brinde()).isFalse();
    }

    @Test
    void ouroNaoPagaFreteEGanhaBrindeAcimaDeQuinhentosReais() {
        ResumoResponse resumo = service.calcular(
                pedido(item("Jaqueta", "500.10", 1, "2.00")).entrega("EXPRESSA").clube("OURO").montar());

        assertThat(resumo.frete()).isEqualByComparingTo("0.00");
        assertThat(resumo.creditoProximaCompra()).isEqualByComparingTo("25.00"); // 25,005 arredonda para o par
        assertThat(resumo.brinde()).isTrue();
    }

    // --- Imposto ---

    @ParameterizedTest
    @CsvSource({"SUDESTE,49.16", "SUL,45.07", "CENTRO_OESTE,36.87", "NORTE,28.68", "NORDESTE,28.68"})
    void impostoSegueAAliquotaDaRegiao(String regiao, String imposto) {
        ResumoResponse resumo = service.calcular(pedido(CAMISETA, TENIS).regiao(regiao).montar());

        assertThat(resumo.imposto()).isEqualByComparingTo(imposto);
    }

    @Test
    void impostoIncideSobreOsProdutosJaComODescontoDoCupom() {
        ResumoResponse resumo = service.calcular(
                pedido(CAMISETA, TENIS).cupom("BEMVINDO10").regiao("SUL").montar());

        assertThat(resumo.imposto()).isEqualByComparingTo("40.56");
    }

    // --- Pagamento ---

    @Test
    void boletoNaoAtendeAcimaDeMilReais() {
        esperaErro(pedido(item("Bicicleta", "1000.01", 1, "10.00")).pagamento("BOLETO").montar(),
                CodigoErro.FORMA_PAGAMENTO_INDISPONIVEL);
    }

    @Test
    void boletoAtendeAteMilReais() {
        ResumoResponse resumo = service.calcular(
                pedido(item("Bicicleta", "1000.00", 1, "10.00")).pagamento("BOLETO").montar());

        assertThat(resumo.ajustePagamento()).isEqualByComparingTo("3.49");
    }

    @Test
    void parcelasAusentesValemUma() {
        ResumoResponse resumo = service.calcular(
                pedido(CAMISETA).pagamento("CARTAO").parcelas(null).montar());

        assertThat(resumo.parcelas()).isEqualTo(1);
        assertThat(resumo.ajustePagamento()).isEqualByComparingTo("0.00");
        assertThat(resumo.valorParcela()).isEqualByComparingTo(resumo.totalFinal());
    }

    @ParameterizedTest
    @CsvSource({"PIX,2", "BOLETO,3", "CARTAO,13", "CARTAO,0", "CARTAO,-1"})
    void parcelamentoForaDoPermitido(String forma, int parcelas) {
        esperaErro(pedido(CAMISETA).pagamento(forma).parcelas(parcelas).montar(),
                CodigoErro.PARCELAMENTO_INVALIDO);
    }

    // --- Erros de entrada e ordem de verificacao ---

    @Test
    void carrinhoVazioOuNuloEPedidoInvalido() {
        esperaErro(new ResumoRequest(List.of(), "EXPRESSA", null, "PIX", 1, "BRONZE", "SUDESTE"),
                CodigoErro.PEDIDO_INVALIDO);
        esperaErro(new ResumoRequest(null, "EXPRESSA", null, "PIX", 1, "BRONZE", "SUDESTE"),
                CodigoErro.PEDIDO_INVALIDO);
    }

    @ParameterizedTest
    @CsvSource(nullValues = "nulo", value = {
            "0.00,2,0.30", "-1.00,2,0.30", "nulo,2,0.30",
            "79.90,0,0.30", "79.90,-1,0.30", "79.90,nulo,0.30",
            "79.90,2,0.00", "79.90,2,-0.10", "79.90,2,nulo"})
    void itemComPrecoQuantidadeOuPesoInvalido(String preco, Integer quantidade, String peso) {
        esperaErro(pedido(item("Camiseta", preco, quantidade, peso)).montar(),
                CodigoErro.PEDIDO_INVALIDO);
    }

    @ParameterizedTest
    @CsvSource(nullValues = "nulo", value = {"DIAMANTE", "bronze", "nulo"})
    void nivelDeClubeInexistenteOuAusente(String nivel) {
        esperaErro(pedido(CAMISETA).clube(nivel).montar(), CodigoErro.NIVEL_CLUBE_INVALIDO);
    }

    @ParameterizedTest
    @CsvSource(nullValues = "nulo", value = {"EUROPA", "sudeste", "nulo"})
    void regiaoInexistenteOuAusente(String regiao) {
        esperaErro(pedido(CAMISETA).regiao(regiao).montar(), CodigoErro.REGIAO_INVALIDA);
    }

    @ParameterizedTest
    @CsvSource(nullValues = "nulo", value = {"DRONE", "expressa", "nulo"})
    void modalidadeInexistenteOuAusente(String modalidade) {
        esperaErro(pedido(CAMISETA).entrega(modalidade).montar(), CodigoErro.MODALIDADE_INVALIDA);
    }

    @ParameterizedTest
    @CsvSource(nullValues = "nulo", value = {"CHEQUE", "pix", "nulo"})
    void formaDePagamentoInexistenteOuAusente(String forma) {
        esperaErro(pedido(CAMISETA).pagamento(forma).montar(), CodigoErro.FORMA_PAGAMENTO_INVALIDA);
    }

    @Test
    void erroDoPedidoVemAntesDeTodosOsOutros() {
        esperaErro(new ResumoRequest(List.of(item("X", "-1.00", 0, "0.00")), "DRONE", "XPTO",
                "CHEQUE", 99, "DIAMANTE", "EUROPA"), CodigoErro.PEDIDO_INVALIDO);
    }

    @Test
    void nivelDoClubeVemAntesDaRegiao() {
        esperaErro(pedido(CAMISETA).clube("DIAMANTE").regiao("EUROPA").montar(),
                CodigoErro.NIVEL_CLUBE_INVALIDO);
    }

    @Test
    void regiaoVemAntesDaModalidade() {
        esperaErro(pedido(CAMISETA).regiao("EUROPA").entrega("DRONE").montar(),
                CodigoErro.REGIAO_INVALIDA);
    }

    @Test
    void modalidadeInexistenteVemAntesDoCupom() {
        esperaErro(pedido(CAMISETA).entrega("DRONE").cupom("XPTO").montar(),
                CodigoErro.MODALIDADE_INVALIDA);
    }

    @Test
    void modalidadeIndisponivelVemAntesDoCupom() {
        esperaErro(pedido(item("Halteres", "100.00", 1, "9.00")).entrega("MOTOBOY").cupom("XPTO").montar(),
                CodigoErro.MODALIDADE_INDISPONIVEL);
    }

    @Test
    void cupomInexistenteVemAntesDaFormaDePagamento() {
        esperaErro(pedido(CAMISETA).cupom("XPTO").pagamento("CHEQUE").montar(),
                CodigoErro.CUPOM_INVALIDO);
    }

    @Test
    void cupomNaoAplicavelVemAntesDaFormaDePagamento() {
        esperaErro(pedido(CAMISETA).cupom("MENOS50").pagamento("CHEQUE").montar(),
                CodigoErro.CUPOM_NAO_APLICAVEL);
    }

    @Test
    void formaDePagamentoInexistenteVemAntesDoParcelamento() {
        esperaErro(pedido(CAMISETA).pagamento("CHEQUE").parcelas(99).montar(),
                CodigoErro.FORMA_PAGAMENTO_INVALIDA);
    }

    @Test
    void parcelamentoInvalidoVemAntesDaIndisponibilidade() {
        esperaErro(pedido(item("Bicicleta", "2000.00", 1, "10.00")).pagamento("BOLETO").parcelas(4).montar(),
                CodigoErro.PARCELAMENTO_INVALIDO);
    }
}

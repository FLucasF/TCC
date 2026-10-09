package com.loja.checkout.calculo;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.loja.checkout.web.ItemRequest;
import com.loja.checkout.web.ResumoRequest;
import com.loja.checkout.web.ResumoResponse;
import java.math.BigDecimal;
import java.util.List;
import org.junit.jupiter.api.Test;

class CalculadoraResumoTest {

    private final CalculadoraResumo calculadora = new CalculadoraResumo();

    private static final List<ItemRequest> CAMISETA_E_TENIS = List.of(
            item("Camiseta", "79.90", 2, "0.30"),
            item("Tênis", "249.90", 1, "1.20"));

    @Test
    void exemplo1PixComCupomBemvindo() {
        ResumoResponse r = calcular(CAMISETA_E_TENIS, "EXPRESSA", "BEMVINDO10", "PIX", null, "BRONZE", "NORTE");

        assertThat(r).isEqualTo(new ResumoResponse(
                bd("409.70"), bd("40.97"), bd("33.10"), 2, bd("10.24"),
                bd("-20.60"), bd("391.47"), 1, bd("391.47"), bd("0.00"), false));
    }

    @Test
    void exemplo2CartaoSeisVezesComClubePrata() {
        ResumoResponse r = calcular(CAMISETA_E_TENIS, "ECONOMICA", null, "CARTAO", 6, "PRATA", "CENTRO_OESTE");

        assertThat(r).isEqualTo(new ResumoResponse(
                bd("409.70"), bd("0.00"), bd("15.60"), 7, bd("6.15"),
                bd("30.55"), bd("462.00"), 6, bd("77.00"), bd("8.19"), false));
    }

    @Test
    void exemplo3MotoboyComCupomMenos50EBoleto() {
        List<ItemRequest> itens = List.of(item("Fone", "199.90", 2, "0.25"));
        ResumoResponse r = calcular(itens, "MOTOBOY", "MENOS50", "BOLETO", null, "BRONZE", "NORDESTE");

        assertThat(r).isEqualTo(new ResumoResponse(
                bd("399.80"), bd("50.00"), bd("18.00"), 0, bd("8.00"),
                bd("3.49"), bd("379.29"), 1, bd("379.29"), bd("0.00"), false));
    }

    @Test
    void exemplo4LeveTresPagueDoisCartaoTresVezesComClubePrata() {
        List<ItemRequest> itens = List.of(
                item("Meia", "19.90", 7, "0.10"),
                item("Camiseta", "79.90", 2, "0.30"));
        ResumoResponse r = calcular(itens, "RETIRADA_LOJA", "LEVE3PAGUE2", "CARTAO", 3, "PRATA", "SUL");

        assertThat(r).isEqualTo(new ResumoResponse(
                bd("299.10"), bd("39.80"), bd("0.00"), 1, bd("2.99"),
                bd("0.00"), bd("262.29"), 3, bd("87.43"), bd("5.98"), false));
    }

    @Test
    void exemplo5PixComClubeOuroNaoPagaFrete() {
        ResumoResponse r = calcular(CAMISETA_E_TENIS, "EXPRESSA", null, "PIX", null, "OURO", "SUDESTE");

        assertThat(r).isEqualTo(new ResumoResponse(
                bd("409.70"), bd("0.00"), bd("0.00"), 2, bd("4.10"),
                bd("-20.69"), bd("393.11"), 1, bd("393.11"), bd("20.48"), false));
    }

    @Test
    void ouroComCompraAcimaDe500GanhaBrinde() {
        List<ItemRequest> itens = List.of(item("Jaqueta", "299.90", 2, "0.80"));
        ResumoResponse r = calcular(itens, "RETIRADA_LOJA", null, "PIX", null, "OURO", "SUDESTE");

        assertThat(r.brinde()).isTrue();
    }

    @Test
    void fretegratisDescontaOValorDoFrete() {
        ResumoResponse r = calcular(CAMISETA_E_TENIS, "EXPRESSA", "FRETEGRATIS", "PIX", null, "BRONZE", "SUDESTE");

        assertThat(r.descontoCupom()).isEqualByComparingTo("33.10");
        assertThat(r.frete()).isEqualByComparingTo("33.10");
    }

    @Test
    void motoboyAcimaDe5KgNaoEstaDisponivel() {
        List<ItemRequest> itens = List.of(item("Mala", "300.00", 1, "5.01"));

        assertErro(itens, "MOTOBOY", null, "PIX", null, "BRONZE", "SUDESTE", CodigoErro.MODALIDADE_INDISPONIVEL);
    }

    @Test
    void menos50AbaixoDe300NaoEAplicavel() {
        List<ItemRequest> itens = List.of(item("Meia", "19.90", 2, "0.10"));

        assertErro(itens, "EXPRESSA", "MENOS50", "PIX", null, "BRONZE", "SUDESTE", CodigoErro.CUPOM_NAO_APLICAVEL);
    }

    @Test
    void boletoAcimaDe1000NaoEstaDisponivel() {
        List<ItemRequest> itens = List.of(item("Casaco", "1200.00", 1, "1.00"));

        assertErro(itens, "EXPRESSA", null, "BOLETO", null, "BRONZE", "SUDESTE",
                CodigoErro.FORMA_PAGAMENTO_INDISPONIVEL);
    }

    @Test
    void pixNaoParcela() {
        assertErro(CAMISETA_E_TENIS, "EXPRESSA", null, "PIX", 2, "BRONZE", "SUDESTE",
                CodigoErro.PARCELAMENTO_INVALIDO);
    }

    @Test
    void cupomInexistenteEhRecusado() {
        assertErro(CAMISETA_E_TENIS, "EXPRESSA", "NAOEXISTE", "PIX", null, "BRONZE", "SUDESTE",
                CodigoErro.CUPOM_INVALIDO);
    }

    @Test
    void carrinhoComPesoZeroEhRecusado() {
        List<ItemRequest> itens = List.of(item("Meia", "19.90", 1, "0"));

        assertErro(itens, "EXPRESSA", null, "PIX", null, "BRONZE", "SUDESTE", CodigoErro.PEDIDO_INVALIDO);
    }

    @Test
    void regiaoDesconhecidaEhRecusada() {
        assertErro(CAMISETA_E_TENIS, "EXPRESSA", null, "PIX", null, "BRONZE", "CENTRO", CodigoErro.REGIAO_INVALIDA);
    }

    private static ResumoResponse calcular(
            List<ItemRequest> itens, String entrega, String cupom, String pagamento,
            Integer parcelas, String nivel, String regiao) {
        return new CalculadoraResumo().calcular(
                new ResumoRequest(itens, entrega, cupom, pagamento, parcelas, nivel, regiao));
    }

    private void assertErro(
            List<ItemRequest> itens, String entrega, String cupom, String pagamento,
            Integer parcelas, String nivel, String regiao, CodigoErro esperado) {
        assertThatThrownBy(() -> calculadora.calcular(
                new ResumoRequest(itens, entrega, cupom, pagamento, parcelas, nivel, regiao)))
                .isInstanceOfSatisfying(ErroPedido.class, e -> assertThat(e.codigo()).isEqualTo(esperado));
    }

    private static ItemRequest item(String nome, String preco, int quantidade, String peso) {
        return new ItemRequest(nome, bd(preco), quantidade, bd(peso));
    }

    private static BigDecimal bd(String valor) {
        return new BigDecimal(valor);
    }
}

package com.loja.checkout.calculo;

import com.loja.checkout.api.ItemRequest;
import com.loja.checkout.api.ResumoRequest;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowableOfType;

class CalculadoraResumoTest {

    private final CalculadoraResumo calculadora = new CalculadoraResumo();

    private static final List<ItemRequest> CAMISETA_E_TENIS = List.of(
            new ItemRequest("Camiseta", bd("79.90"), 2, bd("0.30")),
            new ItemRequest("Tenis", bd("249.90"), 1, bd("1.20")));

    @Test
    void exemplo1PixComCupomBemVindo() {
        ResumoRequest pedido = new ResumoRequest(CAMISETA_E_TENIS, "EXPRESSA", "BEMVINDO10", "PIX", null, "BRONZE", "NORTE");

        assertThat(calculadora.calcular(pedido)).isEqualTo(new ResumoCompra(
                bd("409.70"), bd("40.97"), bd("33.10"), 2, bd("10.24"), bd("-20.60"),
                bd("391.47"), 1, bd("391.47"), bd("0.00"), false));
    }

    @Test
    void exemplo2CartaoSeisVezesComJuros() {
        ResumoRequest pedido = new ResumoRequest(CAMISETA_E_TENIS, "ECONOMICA", null, "CARTAO", 6, "PRATA", "CENTRO_OESTE");

        assertThat(calculadora.calcular(pedido)).isEqualTo(new ResumoCompra(
                bd("409.70"), bd("0.00"), bd("15.60"), 7, bd("6.15"), bd("30.55"),
                bd("462.00"), 6, bd("77.00"), bd("8.19"), false));
    }

    @Test
    void exemplo3BoletoComMotoboyECupomMenos50() {
        List<ItemRequest> itens = List.of(new ItemRequest("Fone", bd("199.90"), 2, bd("0.25")));
        ResumoRequest pedido = new ResumoRequest(itens, "MOTOBOY", "MENOS50", "BOLETO", null, "BRONZE", "NORDESTE");

        assertThat(calculadora.calcular(pedido)).isEqualTo(new ResumoCompra(
                bd("399.80"), bd("50.00"), bd("18.00"), 0, bd("8.00"), bd("3.49"),
                bd("379.29"), 1, bd("379.29"), bd("0.00"), false));
    }

    @Test
    void exemplo4CartaoTresVezesComLeve3Pague2() {
        List<ItemRequest> itens = List.of(
                new ItemRequest("Meia", bd("19.90"), 7, bd("0.10")),
                new ItemRequest("Camiseta", bd("79.90"), 2, bd("0.30")));
        ResumoRequest pedido = new ResumoRequest(itens, "RETIRADA_LOJA", "LEVE3PAGUE2", "CARTAO", 3, "PRATA", "SUL");

        assertThat(calculadora.calcular(pedido)).isEqualTo(new ResumoCompra(
                bd("299.10"), bd("39.80"), bd("0.00"), 1, bd("2.99"), bd("0.00"),
                bd("262.29"), 3, bd("87.43"), bd("5.98"), false));
    }

    @Test
    void exemplo5OuroSemCupomComBrindeAusente() {
        ResumoRequest pedido = new ResumoRequest(CAMISETA_E_TENIS, "EXPRESSA", null, "PIX", null, "OURO", "SUDESTE");

        assertThat(calculadora.calcular(pedido)).isEqualTo(new ResumoCompra(
                bd("409.70"), bd("0.00"), bd("0.00"), 2, bd("4.10"), bd("-20.69"),
                bd("393.11"), 1, bd("393.11"), bd("20.48"), false));
    }

    @Test
    void ouroGanhaBrindeAcimaDeQuinhentosReais() {
        List<ItemRequest> itens = List.of(new ItemRequest("Bolsa", bd("500.01"), 1, bd("0.50")));
        ResumoRequest pedido = new ResumoRequest(itens, "RETIRADA_LOJA", null, "PIX", null, "OURO", "SUDESTE");

        assertThat(calculadora.calcular(pedido).brinde()).isTrue();
    }

    @Test
    void freteGratisDescontaOValorDoFrete() {
        ResumoRequest pedido = new ResumoRequest(CAMISETA_E_TENIS, "EXPRESSA", "FRETEGRATIS", "PIX", null, "BRONZE", "SUDESTE");

        ResumoCompra resumo = calculadora.calcular(pedido);

        assertThat(resumo.frete()).isEqualTo(bd("33.10"));
        assertThat(resumo.descontoCupom()).isEqualTo(bd("33.10"));
    }

    @Test
    void carrinhoVazioEhPedidoInvalido() {
        assertThat(erroDe(pedido(List.of(), "EXPRESSA", null, "PIX", null, "BRONZE", "SUL")))
                .isEqualTo(CodigoErro.PEDIDO_INVALIDO);
    }

    @Test
    void itemComQuantidadeZeroEhPedidoInvalido() {
        List<ItemRequest> itens = List.of(new ItemRequest("Meia", bd("19.90"), 0, bd("0.10")));

        assertThat(erroDe(pedido(itens, "EXPRESSA", null, "PIX", null, "BRONZE", "SUL")))
                .isEqualTo(CodigoErro.PEDIDO_INVALIDO);
    }

    @Test
    void itemSemPesoEhPedidoInvalido() {
        List<ItemRequest> itens = List.of(new ItemRequest("Meia", bd("19.90"), 1, null));

        assertThat(erroDe(pedido(itens, "EXPRESSA", null, "PIX", null, "BRONZE", "SUL")))
                .isEqualTo(CodigoErro.PEDIDO_INVALIDO);
    }

    @Test
    void pedidoInvalidoTemPrioridadeSobreOsOutrosErros() {
        ResumoRequest pedido = pedido(List.of(), "NAO_EXISTE", "NAO_EXISTE", "NAO_EXISTE", null, "NAO_EXISTE", "NAO_EXISTE");

        assertThat(erroDe(pedido)).isEqualTo(CodigoErro.PEDIDO_INVALIDO);
    }

    @Test
    void nivelClubeInvalidoTemPrioridadeSobreRegiaoEEntrega() {
        ResumoRequest pedido = pedido(CAMISETA_E_TENIS, "NAO_EXISTE", null, "PIX", null, "DIAMANTE", "NAO_EXISTE");

        assertThat(erroDe(pedido)).isEqualTo(CodigoErro.NIVEL_CLUBE_INVALIDO);
    }

    @Test
    void regiaoInvalidaEhRecusada() {
        assertThat(erroDe(pedido(CAMISETA_E_TENIS, "EXPRESSA", null, "PIX", null, "BRONZE", null)))
                .isEqualTo(CodigoErro.REGIAO_INVALIDA);
    }

    @Test
    void modalidadeInvalidaEhRecusada() {
        assertThat(erroDe(pedido(CAMISETA_E_TENIS, null, null, "PIX", null, "BRONZE", "SUL")))
                .isEqualTo(CodigoErro.MODALIDADE_INVALIDA);
    }

    @Test
    void motoboyAteCincoQuilosEhAceito() {
        List<ItemRequest> itens = List.of(new ItemRequest("Mala", bd("100.00"), 1, bd("5.00")));

        assertThat(calculadora.calcular(pedido(itens, "MOTOBOY", null, "PIX", null, "BRONZE", "SUL")).frete())
                .isEqualTo(bd("18.00"));
    }

    @Test
    void motoboyAcimaDeCincoQuilosEhIndisponivel() {
        List<ItemRequest> itens = List.of(new ItemRequest("Mala", bd("100.00"), 1, bd("5.01")));

        assertThat(erroDe(pedido(itens, "MOTOBOY", null, "PIX", null, "BRONZE", "SUL")))
                .isEqualTo(CodigoErro.MODALIDADE_INDISPONIVEL);
    }

    @Test
    void cupomInexistenteOuEmMinusculoEhInvalido() {
        assertThat(erroDe(pedido(CAMISETA_E_TENIS, "EXPRESSA", "bemvindo10", "PIX", null, "BRONZE", "SUL")))
                .isEqualTo(CodigoErro.CUPOM_INVALIDO);
    }

    @Test
    void menos50AbaixoDeTrezentosEhNaoAplicavel() {
        List<ItemRequest> itens = List.of(new ItemRequest("Cinto", bd("299.99"), 1, bd("0.20")));

        assertThat(erroDe(pedido(itens, "EXPRESSA", "MENOS50", "PIX", null, "BRONZE", "SUL")))
                .isEqualTo(CodigoErro.CUPOM_NAO_APLICAVEL);
    }

    @Test
    void leve3Pague2SemItemComTresUnidadesEhNaoAplicavel() {
        assertThat(erroDe(pedido(CAMISETA_E_TENIS, "EXPRESSA", "LEVE3PAGUE2", "PIX", null, "BRONZE", "SUL")))
                .isEqualTo(CodigoErro.CUPOM_NAO_APLICAVEL);
    }

    @Test
    void formaPagamentoInvalidaEhRecusada() {
        assertThat(erroDe(pedido(CAMISETA_E_TENIS, "EXPRESSA", null, null, null, "BRONZE", "SUL")))
                .isEqualTo(CodigoErro.FORMA_PAGAMENTO_INVALIDA);
    }

    @Test
    void pixEBoletoNaoParcelam() {
        assertThat(erroDe(pedido(CAMISETA_E_TENIS, "EXPRESSA", null, "PIX", 2, "BRONZE", "SUL")))
                .isEqualTo(CodigoErro.PARCELAMENTO_INVALIDO);
        assertThat(erroDe(pedido(CAMISETA_E_TENIS, "EXPRESSA", null, "BOLETO", 2, "BRONZE", "SUL")))
                .isEqualTo(CodigoErro.PARCELAMENTO_INVALIDO);
    }

    @Test
    void cartaoAceitaDeUmaADozeParcelas() {
        assertThat(erroDe(pedido(CAMISETA_E_TENIS, "EXPRESSA", null, "CARTAO", 13, "BRONZE", "SUL")))
                .isEqualTo(CodigoErro.PARCELAMENTO_INVALIDO);
        assertThat(erroDe(pedido(CAMISETA_E_TENIS, "EXPRESSA", null, "CARTAO", 0, "BRONZE", "SUL")))
                .isEqualTo(CodigoErro.PARCELAMENTO_INVALIDO);
    }

    @Test
    void boletoAcimaDeMilReaisEhIndisponivel() {
        List<ItemRequest> itens = List.of(new ItemRequest("Casaco", bd("1000.01"), 1, bd("1.00")));

        assertThat(erroDe(pedido(itens, "RETIRADA_LOJA", null, "BOLETO", null, "BRONZE", "SUDESTE")))
                .isEqualTo(CodigoErro.FORMA_PAGAMENTO_INDISPONIVEL);
    }

    private static ResumoRequest pedido(List<ItemRequest> itens, String entrega, String cupom, String pagamento,
                                        Integer parcelas, String nivel, String regiao) {
        return new ResumoRequest(itens, entrega, cupom, pagamento, parcelas, nivel, regiao);
    }

    private CodigoErro erroDe(ResumoRequest pedido) {
        return catchThrowableOfType(() -> calculadora.calcular(pedido), PedidoRecusadoException.class).getCodigo();
    }

    private static BigDecimal bd(String valor) {
        return new BigDecimal(valor);
    }
}

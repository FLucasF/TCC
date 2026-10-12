package com.loja.checkout.servico;

import com.loja.checkout.api.ItemRequest;
import com.loja.checkout.api.ResumoRequest;
import com.loja.checkout.dominio.CodigoErro;
import com.loja.checkout.dominio.ErroPedido;
import com.loja.checkout.dominio.Resumo;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class CalculadoraResumoTest {

    private final CalculadoraResumo calculadora = new CalculadoraResumo();

    private static final List<ItemRequest> CAMISETA_E_TENIS = List.of(
            new ItemRequest("Camiseta", new BigDecimal("79.90"), 2, new BigDecimal("0.30")),
            new ItemRequest("Tênis", new BigDecimal("249.90"), 1, new BigDecimal("1.20")));

    @Test
    void exemplo1() {
        Resumo r = calcular(CAMISETA_E_TENIS, "EXPRESSA", "BEMVINDO10", "PIX", null, "BRONZE", "NORTE");

        assertThat(r.subtotalProdutos()).isEqualTo("409.70");
        assertThat(r.descontoCupom()).isEqualTo("40.97");
        assertThat(r.frete()).isEqualTo("33.10");
        assertThat(r.prazoEntregaDias()).isEqualTo(2);
        assertThat(r.seguro()).isEqualTo("10.24");
        assertThat(r.ajustePagamento()).isEqualTo("-20.60");
        assertThat(r.totalFinal()).isEqualTo("391.47");
        assertThat(r.parcelas()).isEqualTo(1);
        assertThat(r.valorParcela()).isEqualTo("391.47");
        assertThat(r.creditoProximaCompra()).isEqualTo("0.00");
        assertThat(r.brinde()).isFalse();
    }

    @Test
    void exemplo2() {
        Resumo r = calcular(CAMISETA_E_TENIS, "ECONOMICA", null, "CARTAO", 6, "PRATA", "CENTRO_OESTE");

        assertThat(r.subtotalProdutos()).isEqualTo("409.70");
        assertThat(r.descontoCupom()).isEqualTo("0.00");
        assertThat(r.frete()).isEqualTo("15.60");
        assertThat(r.prazoEntregaDias()).isEqualTo(7);
        assertThat(r.seguro()).isEqualTo("6.15");
        assertThat(r.ajustePagamento()).isEqualTo("30.55");
        assertThat(r.totalFinal()).isEqualTo("462.00");
        assertThat(r.parcelas()).isEqualTo(6);
        assertThat(r.valorParcela()).isEqualTo("77.00");
        assertThat(r.creditoProximaCompra()).isEqualTo("8.19");
        assertThat(r.brinde()).isFalse();
    }

    @Test
    void exemplo3() {
        List<ItemRequest> itens = List.of(
                new ItemRequest("Fone", new BigDecimal("199.90"), 2, new BigDecimal("0.25")));
        Resumo r = calcular(itens, "MOTOBOY", "MENOS50", "BOLETO", null, "BRONZE", "NORDESTE");

        assertThat(r.subtotalProdutos()).isEqualTo("399.80");
        assertThat(r.descontoCupom()).isEqualTo("50.00");
        assertThat(r.frete()).isEqualTo("18.00");
        assertThat(r.prazoEntregaDias()).isZero();
        assertThat(r.seguro()).isEqualTo("8.00");
        assertThat(r.ajustePagamento()).isEqualTo("3.49");
        assertThat(r.totalFinal()).isEqualTo("379.29");
        assertThat(r.parcelas()).isEqualTo(1);
        assertThat(r.valorParcela()).isEqualTo("379.29");
        assertThat(r.creditoProximaCompra()).isEqualTo("0.00");
        assertThat(r.brinde()).isFalse();
    }

    @Test
    void exemplo4() {
        List<ItemRequest> itens = List.of(
                new ItemRequest("Meia", new BigDecimal("19.90"), 7, new BigDecimal("0.10")),
                new ItemRequest("Camiseta", new BigDecimal("79.90"), 2, new BigDecimal("0.30")));
        Resumo r = calcular(itens, "RETIRADA_LOJA", "LEVE3PAGUE2", "CARTAO", 3, "PRATA", "SUL");

        assertThat(r.subtotalProdutos()).isEqualTo("299.10");
        assertThat(r.descontoCupom()).isEqualTo("39.80");
        assertThat(r.frete()).isEqualTo("0.00");
        assertThat(r.prazoEntregaDias()).isEqualTo(1);
        assertThat(r.seguro()).isEqualTo("2.99");
        assertThat(r.ajustePagamento()).isEqualTo("0.00");
        assertThat(r.totalFinal()).isEqualTo("262.29");
        assertThat(r.parcelas()).isEqualTo(3);
        assertThat(r.valorParcela()).isEqualTo("87.43");
        assertThat(r.creditoProximaCompra()).isEqualTo("5.98");
        assertThat(r.brinde()).isFalse();
    }

    @Test
    void exemplo5() {
        Resumo r = calcular(CAMISETA_E_TENIS, "EXPRESSA", null, "PIX", null, "OURO", "SUDESTE");

        assertThat(r.subtotalProdutos()).isEqualTo("409.70");
        assertThat(r.descontoCupom()).isEqualTo("0.00");
        assertThat(r.frete()).isEqualTo("0.00");
        assertThat(r.prazoEntregaDias()).isEqualTo(2);
        assertThat(r.seguro()).isEqualTo("4.10");
        assertThat(r.ajustePagamento()).isEqualTo("-20.69");
        assertThat(r.totalFinal()).isEqualTo("393.11");
        assertThat(r.parcelas()).isEqualTo(1);
        assertThat(r.valorParcela()).isEqualTo("393.11");
        assertThat(r.creditoProximaCompra()).isEqualTo("20.48");
        assertThat(r.brinde()).isFalse();
    }

    @Test
    void ourooComSubtotalAcimaDe500GanhaBrinde() {
        List<ItemRequest> itens = List.of(
                new ItemRequest("Bolsa", new BigDecimal("300.00"), 2, new BigDecimal("0.50")));
        Resumo r = calcular(itens, "RETIRADA_LOJA", null, "PIX", null, "OURO", "SUL");

        assertThat(r.brinde()).isTrue();
    }

    @Test
    void subtotalIgualA500NaoGanhaBrinde() {
        List<ItemRequest> itens = List.of(
                new ItemRequest("Bolsa", new BigDecimal("250.00"), 2, new BigDecimal("0.50")));
        Resumo r = calcular(itens, "RETIRADA_LOJA", null, "PIX", null, "OURO", "SUL");

        assertThat(r.brinde()).isFalse();
    }

    @Test
    void freteGratisDevolveODescontoIgualAoFrete() {
        Resumo r = calcular(CAMISETA_E_TENIS, "EXPRESSA", "FRETEGRATIS", "PIX", null, "BRONZE", "SUDESTE");

        assertThat(r.frete()).isEqualTo("33.10");
        assertThat(r.descontoCupom()).isEqualTo("33.10");
    }

    @Test
    void pedidoSemItens() {
        assertErro(List.of(), "EXPRESSA", null, "PIX", null, "BRONZE", "SUL", CodigoErro.PEDIDO_INVALIDO);
    }

    @Test
    void itemComPrecoZero() {
        List<ItemRequest> itens = List.of(
                new ItemRequest("Meia", BigDecimal.ZERO, 1, new BigDecimal("0.10")));
        assertErro(itens, "EXPRESSA", null, "PIX", null, "BRONZE", "SUL", CodigoErro.PEDIDO_INVALIDO);
    }

    @Test
    void itemSemQuantidade() {
        List<ItemRequest> itens = List.of(
                new ItemRequest("Meia", new BigDecimal("10.00"), null, new BigDecimal("0.10")));
        assertErro(itens, "EXPRESSA", null, "PIX", null, "BRONZE", "SUL", CodigoErro.PEDIDO_INVALIDO);
    }

    @Test
    void nivelInexistente() {
        assertErro(CAMISETA_E_TENIS, "EXPRESSA", null, "PIX", null, "DIAMANTE", "SUL", CodigoErro.NIVEL_CLUBE_INVALIDO);
    }

    @Test
    void regiaoNaoInformada() {
        assertErro(CAMISETA_E_TENIS, "EXPRESSA", null, "PIX", null, "BRONZE", null, CodigoErro.REGIAO_INVALIDA);
    }

    @Test
    void modalidadeInexistente() {
        assertErro(CAMISETA_E_TENIS, "DRONE", null, "PIX", null, "BRONZE", "SUL", CodigoErro.MODALIDADE_INVALIDA);
    }

    @Test
    void motoboyAcimaDe5kg() {
        List<ItemRequest> itens = List.of(
                new ItemRequest("Casaco", new BigDecimal("100.00"), 2, new BigDecimal("3.00")));
        assertErro(itens, "MOTOBOY", null, "PIX", null, "BRONZE", "SUL", CodigoErro.MODALIDADE_INDISPONIVEL);
    }

    @Test
    void cupomInexistenteOuEmMinusculas() {
        assertErro(CAMISETA_E_TENIS, "EXPRESSA", "bemvindo10", "PIX", null, "BRONZE", "SUL", CodigoErro.CUPOM_INVALIDO);
    }

    @Test
    void menos50AbaixoDe300() {
        List<ItemRequest> itens = List.of(
                new ItemRequest("Cinto", new BigDecimal("299.90"), 1, new BigDecimal("0.20")));
        assertErro(itens, "EXPRESSA", "MENOS50", "PIX", null, "BRONZE", "SUL", CodigoErro.CUPOM_NAO_APLICAVEL);
    }

    @Test
    void formaPagamentoInexistente() {
        assertErro(CAMISETA_E_TENIS, "EXPRESSA", null, "CHEQUE", null, "BRONZE", "SUL", CodigoErro.FORMA_PAGAMENTO_INVALIDA);
    }

    @Test
    void pixEmDuasParcelas() {
        assertErro(CAMISETA_E_TENIS, "EXPRESSA", null, "PIX", 2, "BRONZE", "SUL", CodigoErro.PARCELAMENTO_INVALIDO);
    }

    @Test
    void cartaoEmTreze() {
        assertErro(CAMISETA_E_TENIS, "EXPRESSA", null, "CARTAO", 13, "BRONZE", "SUL", CodigoErro.PARCELAMENTO_INVALIDO);
    }

    @Test
    void boletoAcimaDe1000() {
        List<ItemRequest> itens = List.of(
                new ItemRequest("Jaqueta", new BigDecimal("1000.01"), 1, new BigDecimal("1.00")));
        assertErro(itens, "RETIRADA_LOJA", null, "BOLETO", null, "BRONZE", "SUL", CodigoErro.FORMA_PAGAMENTO_INDISPONIVEL);
    }

    private Resumo calcular(List<ItemRequest> itens, String modalidade, String cupom, String pagamento,
                            Integer parcelas, String nivel, String regiao) {
        return calculadora.calcular(new ResumoRequest(itens, modalidade, cupom, pagamento, parcelas, nivel, regiao));
    }

    private void assertErro(List<ItemRequest> itens, String modalidade, String cupom, String pagamento,
                            Integer parcelas, String nivel, String regiao, CodigoErro codigo) {
        assertThatThrownBy(() -> calcular(itens, modalidade, cupom, pagamento, parcelas, nivel, regiao))
                .isInstanceOfSatisfying(ErroPedido.class, e -> assertThat(e.getCodigo()).isEqualTo(codigo));
    }
}

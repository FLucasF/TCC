package com.loja.checkout;

import com.loja.checkout.clube.Bronze;
import com.loja.checkout.clube.NivelClube;
import com.loja.checkout.clube.Ouro;
import com.loja.checkout.clube.Prata;
import com.loja.checkout.cupom.Bemvindo10;
import com.loja.checkout.cupom.Cupom;
import com.loja.checkout.cupom.FreteGratis;
import com.loja.checkout.cupom.Leve3Pague2;
import com.loja.checkout.cupom.Menos50;
import com.loja.checkout.dto.ItemRequest;
import com.loja.checkout.dto.ResumoRequest;
import com.loja.checkout.dto.ResumoResponse;
import com.loja.checkout.entrega.Economica;
import com.loja.checkout.entrega.Expressa;
import com.loja.checkout.entrega.ModalidadeEntrega;
import com.loja.checkout.entrega.Motoboy;
import com.loja.checkout.entrega.RetiradaLoja;
import com.loja.checkout.erro.CodigoErro;
import com.loja.checkout.erro.PedidoInvalidoException;
import com.loja.checkout.pagamento.Boleto;
import com.loja.checkout.pagamento.Cartao;
import com.loja.checkout.pagamento.FormaPagamento;
import com.loja.checkout.pagamento.Pix;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ResumoServiceTest {

    private ResumoService service;

    @BeforeEach
    void montarService() {
        List<ModalidadeEntrega> entregas = List.of(
                new Economica(), new Expressa(), new RetiradaLoja(), new Motoboy());
        List<Cupom> cupons = List.of(
                new Bemvindo10(), new Menos50(), new FreteGratis(), new Leve3Pague2());
        List<NivelClube> niveis = List.of(new Bronze(), new Prata(), new Ouro());
        List<FormaPagamento> pagamentos = List.of(new Pix(), new Cartao(), new Boleto());
        service = new ResumoService(entregas, cupons, niveis, pagamentos);
    }

    private static ItemRequest item(String nome, String preco, int quantidade, String peso) {
        return new ItemRequest(nome, new BigDecimal(preco), quantidade, new BigDecimal(peso));
    }

    @Test
    void exemplo1() {
        ResumoRequest pedido = new ResumoRequest(
                List.of(item("Camiseta", "79.90", 2, "0.30"), item("Tênis", "249.90", 1, "1.20")),
                "EXPRESSA", "BEMVINDO10", "PIX", null, "BRONZE", "NORTE");

        ResumoResponse resumo = service.calcular(pedido);

        assertThat(resumo.subtotalProdutos()).isEqualTo(new BigDecimal("409.70"));
        assertThat(resumo.descontoCupom()).isEqualTo(new BigDecimal("40.97"));
        assertThat(resumo.frete()).isEqualTo(new BigDecimal("33.10"));
        assertThat(resumo.prazoEntregaDias()).isEqualTo(2);
        assertThat(resumo.seguro()).isEqualTo(new BigDecimal("10.24"));
        assertThat(resumo.ajustePagamento()).isEqualTo(new BigDecimal("-20.60"));
        assertThat(resumo.totalFinal()).isEqualTo(new BigDecimal("391.47"));
        assertThat(resumo.parcelas()).isEqualTo(1);
        assertThat(resumo.valorParcela()).isEqualTo(new BigDecimal("391.47"));
        assertThat(resumo.creditoProximaCompra()).isEqualTo(new BigDecimal("0.00"));
        assertThat(resumo.brinde()).isFalse();
    }

    @Test
    void exemplo2() {
        ResumoRequest pedido = new ResumoRequest(
                List.of(item("Camiseta", "79.90", 2, "0.30"), item("Tênis", "249.90", 1, "1.20")),
                "ECONOMICA", null, "CARTAO", 6, "PRATA", "CENTRO_OESTE");

        ResumoResponse resumo = service.calcular(pedido);

        assertThat(resumo.subtotalProdutos()).isEqualTo(new BigDecimal("409.70"));
        assertThat(resumo.descontoCupom()).isEqualTo(new BigDecimal("0.00"));
        assertThat(resumo.frete()).isEqualTo(new BigDecimal("15.60"));
        assertThat(resumo.prazoEntregaDias()).isEqualTo(7);
        assertThat(resumo.seguro()).isEqualTo(new BigDecimal("6.15"));
        assertThat(resumo.ajustePagamento()).isEqualTo(new BigDecimal("30.55"));
        assertThat(resumo.totalFinal()).isEqualTo(new BigDecimal("462.00"));
        assertThat(resumo.parcelas()).isEqualTo(6);
        assertThat(resumo.valorParcela()).isEqualTo(new BigDecimal("77.00"));
        assertThat(resumo.creditoProximaCompra()).isEqualTo(new BigDecimal("8.19"));
        assertThat(resumo.brinde()).isFalse();
    }

    @Test
    void exemplo3() {
        ResumoRequest pedido = new ResumoRequest(
                List.of(item("Fone", "199.90", 2, "0.25")),
                "MOTOBOY", "MENOS50", "BOLETO", null, "BRONZE", "NORDESTE");

        ResumoResponse resumo = service.calcular(pedido);

        assertThat(resumo.subtotalProdutos()).isEqualTo(new BigDecimal("399.80"));
        assertThat(resumo.descontoCupom()).isEqualTo(new BigDecimal("50.00"));
        assertThat(resumo.frete()).isEqualTo(new BigDecimal("18.00"));
        assertThat(resumo.prazoEntregaDias()).isEqualTo(0);
        assertThat(resumo.seguro()).isEqualTo(new BigDecimal("8.00"));
        assertThat(resumo.ajustePagamento()).isEqualTo(new BigDecimal("3.49"));
        assertThat(resumo.totalFinal()).isEqualTo(new BigDecimal("379.29"));
        assertThat(resumo.parcelas()).isEqualTo(1);
        assertThat(resumo.valorParcela()).isEqualTo(new BigDecimal("379.29"));
        assertThat(resumo.creditoProximaCompra()).isEqualTo(new BigDecimal("0.00"));
        assertThat(resumo.brinde()).isFalse();
    }

    @Test
    void exemplo4() {
        ResumoRequest pedido = new ResumoRequest(
                List.of(item("Meia", "19.90", 7, "0.10"), item("Camiseta", "79.90", 2, "0.30")),
                "RETIRADA_LOJA", "LEVE3PAGUE2", "CARTAO", 3, "PRATA", "SUL");

        ResumoResponse resumo = service.calcular(pedido);

        assertThat(resumo.subtotalProdutos()).isEqualTo(new BigDecimal("299.10"));
        assertThat(resumo.descontoCupom()).isEqualTo(new BigDecimal("39.80"));
        assertThat(resumo.frete()).isEqualTo(new BigDecimal("0.00"));
        assertThat(resumo.prazoEntregaDias()).isEqualTo(1);
        assertThat(resumo.seguro()).isEqualTo(new BigDecimal("2.99"));
        assertThat(resumo.ajustePagamento()).isEqualTo(new BigDecimal("0.00"));
        assertThat(resumo.totalFinal()).isEqualTo(new BigDecimal("262.29"));
        assertThat(resumo.parcelas()).isEqualTo(3);
        assertThat(resumo.valorParcela()).isEqualTo(new BigDecimal("87.43"));
        assertThat(resumo.creditoProximaCompra()).isEqualTo(new BigDecimal("5.98"));
        assertThat(resumo.brinde()).isFalse();
    }

    @Test
    void exemplo5() {
        ResumoRequest pedido = new ResumoRequest(
                List.of(item("Camiseta", "79.90", 2, "0.30"), item("Tênis", "249.90", 1, "1.20")),
                "EXPRESSA", null, "PIX", null, "OURO", "SUDESTE");

        ResumoResponse resumo = service.calcular(pedido);

        assertThat(resumo.subtotalProdutos()).isEqualTo(new BigDecimal("409.70"));
        assertThat(resumo.descontoCupom()).isEqualTo(new BigDecimal("0.00"));
        assertThat(resumo.frete()).isEqualTo(new BigDecimal("0.00"));
        assertThat(resumo.prazoEntregaDias()).isEqualTo(2);
        assertThat(resumo.seguro()).isEqualTo(new BigDecimal("4.10"));
        assertThat(resumo.ajustePagamento()).isEqualTo(new BigDecimal("-20.69"));
        assertThat(resumo.totalFinal()).isEqualTo(new BigDecimal("393.11"));
        assertThat(resumo.parcelas()).isEqualTo(1);
        assertThat(resumo.valorParcela()).isEqualTo(new BigDecimal("393.11"));
        assertThat(resumo.creditoProximaCompra()).isEqualTo(new BigDecimal("20.48"));
        assertThat(resumo.brinde()).isFalse();
    }

    @Test
    void carrinhoVazioGeraPedidoInvalido() {
        ResumoRequest pedido = new ResumoRequest(
                List.of(), "ECONOMICA", null, "PIX", null, "BRONZE", "SUDESTE");

        assertThatThrownBy(() -> service.calcular(pedido))
                .isInstanceOf(PedidoInvalidoException.class)
                .extracting(ex -> ((PedidoInvalidoException) ex).codigo())
                .isEqualTo(CodigoErro.PEDIDO_INVALIDO);
    }

    @Test
    void motoboyAcimaDoLimiteDeRecusado() {
        ResumoRequest pedido = new ResumoRequest(
                List.of(item("Caixa", "10.00", 1, "6.00")),
                "MOTOBOY", null, "PIX", null, "BRONZE", "SUDESTE");

        assertThatThrownBy(() -> service.calcular(pedido))
                .isInstanceOf(PedidoInvalidoException.class)
                .extracting(ex -> ((PedidoInvalidoException) ex).codigo())
                .isEqualTo(CodigoErro.MODALIDADE_INDISPONIVEL);
    }

    @Test
    void boletoAcimaDoLimiteDeRecusado() {
        ResumoRequest pedido = new ResumoRequest(
                List.of(item("Notebook", "2000.00", 1, "2.00")),
                "RETIRADA_LOJA", null, "BOLETO", null, "BRONZE", "SUDESTE");

        assertThatThrownBy(() -> service.calcular(pedido))
                .isInstanceOf(PedidoInvalidoException.class)
                .extracting(ex -> ((PedidoInvalidoException) ex).codigo())
                .isEqualTo(CodigoErro.FORMA_PAGAMENTO_INDISPONIVEL);
    }

    @Test
    void nivelClubeInvalidoTemPrioridadeSobreRegiaoInvalida() {
        ResumoRequest pedido = new ResumoRequest(
                List.of(item("Caixa", "10.00", 1, "1.00")),
                "ECONOMICA", null, "PIX", null, "DIAMANTE", "LUA");

        assertErro(pedido, CodigoErro.NIVEL_CLUBE_INVALIDO);
    }

    @Test
    void regiaoInvalidaGeraErro() {
        ResumoRequest pedido = new ResumoRequest(
                List.of(item("Caixa", "10.00", 1, "1.00")),
                "ECONOMICA", null, "PIX", null, "BRONZE", "LUA");

        assertErro(pedido, CodigoErro.REGIAO_INVALIDA);
    }

    @Test
    void modalidadeInvalidaGeraErro() {
        ResumoRequest pedido = new ResumoRequest(
                List.of(item("Caixa", "10.00", 1, "1.00")),
                "TELEPORTE", null, "PIX", null, "BRONZE", "SUDESTE");

        assertErro(pedido, CodigoErro.MODALIDADE_INVALIDA);
    }

    @Test
    void cupomInvalidoGeraErro() {
        ResumoRequest pedido = new ResumoRequest(
                List.of(item("Caixa", "10.00", 1, "1.00")),
                "ECONOMICA", "NAOEXISTE", "PIX", null, "BRONZE", "SUDESTE");

        assertErro(pedido, CodigoErro.CUPOM_INVALIDO);
    }

    @Test
    void cupomNaoAplicavelGeraErro() {
        ResumoRequest pedido = new ResumoRequest(
                List.of(item("Caixa", "10.00", 1, "1.00")),
                "ECONOMICA", "MENOS50", "PIX", null, "BRONZE", "SUDESTE");

        assertErro(pedido, CodigoErro.CUPOM_NAO_APLICAVEL);
    }

    @Test
    void formaPagamentoInvalidaGeraErro() {
        ResumoRequest pedido = new ResumoRequest(
                List.of(item("Caixa", "10.00", 1, "1.00")),
                "ECONOMICA", null, "CHEQUE", null, "BRONZE", "SUDESTE");

        assertErro(pedido, CodigoErro.FORMA_PAGAMENTO_INVALIDA);
    }

    @Test
    void parcelamentoInvalidoParaPixGeraErro() {
        ResumoRequest pedido = new ResumoRequest(
                List.of(item("Caixa", "10.00", 1, "1.00")),
                "ECONOMICA", null, "PIX", 2, "BRONZE", "SUDESTE");

        assertErro(pedido, CodigoErro.PARCELAMENTO_INVALIDO);
    }

    @Test
    void parcelamentoInvalidoParaCartaoAcimaDeDozeGeraErro() {
        ResumoRequest pedido = new ResumoRequest(
                List.of(item("Caixa", "10.00", 1, "1.00")),
                "ECONOMICA", null, "CARTAO", 13, "BRONZE", "SUDESTE");

        assertErro(pedido, CodigoErro.PARCELAMENTO_INVALIDO);
    }

    private void assertErro(ResumoRequest pedido, CodigoErro esperado) {
        assertThatThrownBy(() -> service.calcular(pedido))
                .isInstanceOf(PedidoInvalidoException.class)
                .extracting(ex -> ((PedidoInvalidoException) ex).codigo())
                .isEqualTo(esperado);
    }
}

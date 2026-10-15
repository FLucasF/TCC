package com.loja.checkout.service;

import com.loja.checkout.dto.ItemCarrinho;
import com.loja.checkout.dto.RequisicaoResumo;
import com.loja.checkout.dto.RespostaResumo;
import com.loja.checkout.dto.RespostaErro;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
class CheckoutServiceTest {

    @Autowired
    private CheckoutService checkoutService;

    @Test
    void exemplo1() {
        List<ItemCarrinho> itens = Arrays.asList(
            new ItemCarrinho("Camiseta", 79.90, 2, 0.30),
            new ItemCarrinho("Tênis", 249.90, 1, 1.20)
        );

        RequisicaoResumo requisicao = criarRequisicao(itens, "EXPRESSA", "BEMVINDO10", "PIX", 1, "BRONZE", "NORTE");
        Object resultado = checkoutService.calcularResumo(requisicao);

        assertTrue(resultado instanceof RespostaResumo);
        RespostaResumo resposta = (RespostaResumo) resultado;

        assertEquals(new BigDecimal("409.70"), resposta.getSubtotalProdutos());
        assertEquals(new BigDecimal("40.97"), resposta.getDescontoCupom());
        assertEquals(new BigDecimal("33.10"), resposta.getFrete());
        assertEquals(2, resposta.getPrazoEntregaDias());
        assertEquals(new BigDecimal("10.24"), resposta.getSeguro());
        assertEquals(new BigDecimal("-20.60"), resposta.getAjustePagamento());
        assertEquals(new BigDecimal("391.47"), resposta.getTotalFinal());
        assertEquals(1, resposta.getParcelas());
        assertEquals(new BigDecimal("391.47"), resposta.getValorParcela());
        assertEquals(new BigDecimal("0.00"), resposta.getCreditoProximaCompra());
        assertEquals(false, resposta.getBrinde());
    }

    @Test
    void exemplo2() {
        List<ItemCarrinho> itens = Arrays.asList(
            new ItemCarrinho("Camiseta", 79.90, 2, 0.30),
            new ItemCarrinho("Tênis", 249.90, 1, 1.20)
        );

        RequisicaoResumo requisicao = criarRequisicao(itens, "ECONOMICA", null, "CARTAO", 6, "PRATA", "CENTRO_OESTE");
        Object resultado = checkoutService.calcularResumo(requisicao);

        assertTrue(resultado instanceof RespostaResumo);
        RespostaResumo resposta = (RespostaResumo) resultado;

        assertEquals(new BigDecimal("409.70"), resposta.getSubtotalProdutos());
        assertEquals(new BigDecimal("0.00"), resposta.getDescontoCupom());
        assertEquals(new BigDecimal("15.60"), resposta.getFrete());
        assertEquals(7, resposta.getPrazoEntregaDias());
        assertEquals(new BigDecimal("6.15"), resposta.getSeguro());
        assertEquals(new BigDecimal("30.55"), resposta.getAjustePagamento());
        assertEquals(new BigDecimal("462.00"), resposta.getTotalFinal());
        assertEquals(6, resposta.getParcelas());
        assertEquals(new BigDecimal("77.00"), resposta.getValorParcela());
        assertEquals(new BigDecimal("8.19"), resposta.getCreditoProximaCompra());
        assertEquals(false, resposta.getBrinde());
    }

    @Test
    void exemplo3() {
        List<ItemCarrinho> itens = Arrays.asList(
            new ItemCarrinho("Fone", 199.90, 2, 0.25)
        );

        RequisicaoResumo requisicao = criarRequisicao(itens, "MOTOBOY", "MENOS50", "BOLETO", 1, "BRONZE", "NORDESTE");
        Object resultado = checkoutService.calcularResumo(requisicao);

        assertTrue(resultado instanceof RespostaResumo);
        RespostaResumo resposta = (RespostaResumo) resultado;

        assertEquals(new BigDecimal("399.80"), resposta.getSubtotalProdutos());
        assertEquals(new BigDecimal("50.00"), resposta.getDescontoCupom());
        assertEquals(new BigDecimal("18.00"), resposta.getFrete());
        assertEquals(0, resposta.getPrazoEntregaDias());
        assertEquals(new BigDecimal("8.00"), resposta.getSeguro());
        assertEquals(new BigDecimal("3.49"), resposta.getAjustePagamento());
        assertEquals(new BigDecimal("379.29"), resposta.getTotalFinal());
        assertEquals(1, resposta.getParcelas());
        assertEquals(new BigDecimal("379.29"), resposta.getValorParcela());
        assertEquals(new BigDecimal("0.00"), resposta.getCreditoProximaCompra());
        assertEquals(false, resposta.getBrinde());
    }

    @Test
    void exemplo4() {
        List<ItemCarrinho> itens = Arrays.asList(
            new ItemCarrinho("Meia", 19.90, 7, 0.10),
            new ItemCarrinho("Camiseta", 79.90, 2, 0.30)
        );

        RequisicaoResumo requisicao = criarRequisicao(itens, "RETIRADA_LOJA", "LEVE3PAGUE2", "CARTAO", 3, "PRATA", "SUL");
        Object resultado = checkoutService.calcularResumo(requisicao);

        assertTrue(resultado instanceof RespostaResumo);
        RespostaResumo resposta = (RespostaResumo) resultado;

        assertBigDecimal(new BigDecimal("299.10"), resposta.getSubtotalProdutos());
        assertBigDecimal(new BigDecimal("39.80"), resposta.getDescontoCupom());
        assertBigDecimal(new BigDecimal("0.00"), resposta.getFrete());
        assertEquals(1, resposta.getPrazoEntregaDias());
        assertBigDecimal(new BigDecimal("2.99"), resposta.getSeguro());
        assertBigDecimal(new BigDecimal("0.00"), resposta.getAjustePagamento());
        assertEquals(new BigDecimal("262.29"), resposta.getTotalFinal());
        assertEquals(3, resposta.getParcelas());
        assertEquals(new BigDecimal("87.43"), resposta.getValorParcela());
        assertEquals(new BigDecimal("5.98"), resposta.getCreditoProximaCompra());
        assertEquals(false, resposta.getBrinde());
    }

    @Test
    void exemplo5() {
        List<ItemCarrinho> itens = Arrays.asList(
            new ItemCarrinho("Camiseta", 79.90, 2, 0.30),
            new ItemCarrinho("Tênis", 249.90, 1, 1.20)
        );

        RequisicaoResumo requisicao = criarRequisicao(itens, "EXPRESSA", null, "PIX", 1, "OURO", "SUDESTE");
        Object resultado = checkoutService.calcularResumo(requisicao);

        assertTrue(resultado instanceof RespostaResumo);
        RespostaResumo resposta = (RespostaResumo) resultado;

        assertBigDecimal(new BigDecimal("409.70"), resposta.getSubtotalProdutos());
        assertBigDecimal(new BigDecimal("0.00"), resposta.getDescontoCupom());
        assertBigDecimal(new BigDecimal("0.00"), resposta.getFrete());
        assertEquals(2, resposta.getPrazoEntregaDias());
        assertBigDecimal(new BigDecimal("4.10"), resposta.getSeguro());
        assertBigDecimal(new BigDecimal("-20.69"), resposta.getAjustePagamento());
        assertBigDecimal(new BigDecimal("393.11"), resposta.getTotalFinal());
        assertEquals(1, resposta.getParcelas());
        assertBigDecimal(new BigDecimal("393.11"), resposta.getValorParcela());
        assertBigDecimal(new BigDecimal("20.48"), resposta.getCreditoProximaCompra());
        assertEquals(false, resposta.getBrinde());
    }

    @Test
    void testCarrinhoVazio() {
        RequisicaoResumo requisicao = new RequisicaoResumo();

        Object resultado = checkoutService.calcularResumo(requisicao);

        assertTrue(resultado instanceof RespostaErro);
        RespostaErro erro = (RespostaErro) resultado;
        assertEquals("PEDIDO_INVALIDO", erro.getErro());
    }

    @Test
    void testNivelClubeInvalido() {
        List<ItemCarrinho> itens = Arrays.asList(
            new ItemCarrinho("Produto", 100.0, 1, 0.5)
        );

        RequisicaoResumo requisicao = criarRequisicao(itens, "EXPRESSA", null, "PIX", 1, "INVALIDO", "SUDESTE");
        Object resultado = checkoutService.calcularResumo(requisicao);

        assertTrue(resultado instanceof RespostaErro);
        RespostaErro erro = (RespostaErro) resultado;
        assertEquals("NIVEL_CLUBE_INVALIDO", erro.getErro());
    }

    @Test
    void testRegaoInvalida() {
        List<ItemCarrinho> itens = Arrays.asList(
            new ItemCarrinho("Produto", 100.0, 1, 0.5)
        );

        RequisicaoResumo requisicao = criarRequisicao(itens, "EXPRESSA", null, "PIX", 1, "BRONZE", "INVALIDA");
        Object resultado = checkoutService.calcularResumo(requisicao);

        assertTrue(resultado instanceof RespostaErro);
        RespostaErro erro = (RespostaErro) resultado;
        assertEquals("REGIAO_INVALIDA", erro.getErro());
    }

    @Test
    void testModalidadeInvalida() {
        List<ItemCarrinho> itens = Arrays.asList(
            new ItemCarrinho("Produto", 100.0, 1, 0.5)
        );

        RequisicaoResumo requisicao = criarRequisicao(itens, "INVALIDA", null, "PIX", 1, "BRONZE", "SUDESTE");
        Object resultado = checkoutService.calcularResumo(requisicao);

        assertTrue(resultado instanceof RespostaErro);
        RespostaErro erro = (RespostaErro) resultado;
        assertEquals("MODALIDADE_INVALIDA", erro.getErro());
    }

    @Test
    void testMotoboyAcima5Kg() {
        List<ItemCarrinho> itens = Arrays.asList(
            new ItemCarrinho("Produto", 100.0, 1, 6.0)
        );

        RequisicaoResumo requisicao = criarRequisicao(itens, "MOTOBOY", null, "PIX", 1, "BRONZE", "SUDESTE");
        Object resultado = checkoutService.calcularResumo(requisicao);

        assertTrue(resultado instanceof RespostaErro);
        RespostaErro erro = (RespostaErro) resultado;
        assertEquals("MODALIDADE_INDISPONIVEL", erro.getErro());
    }

    @Test
    void testCupomInvalido() {
        List<ItemCarrinho> itens = Arrays.asList(
            new ItemCarrinho("Produto", 100.0, 1, 0.5)
        );

        RequisicaoResumo requisicao = criarRequisicao(itens, "EXPRESSA", "CUPOMINVALIDO", "PIX", 1, "BRONZE", "SUDESTE");
        Object resultado = checkoutService.calcularResumo(requisicao);

        assertTrue(resultado instanceof RespostaErro);
        RespostaErro erro = (RespostaErro) resultado;
        assertEquals("CUPOM_INVALIDO", erro.getErro());
    }

    @Test
    void testCupomMenos50NaoAplicavel() {
        List<ItemCarrinho> itens = Arrays.asList(
            new ItemCarrinho("Produto", 100.0, 1, 0.5)
        );

        RequisicaoResumo requisicao = criarRequisicao(itens, "EXPRESSA", "MENOS50", "PIX", 1, "BRONZE", "SUDESTE");
        Object resultado = checkoutService.calcularResumo(requisicao);

        assertTrue(resultado instanceof RespostaErro);
        RespostaErro erro = (RespostaErro) resultado;
        assertEquals("CUPOM_NAO_APLICAVEL", erro.getErro());
    }

    @Test
    void testFormaPagamentoInvalida() {
        List<ItemCarrinho> itens = Arrays.asList(
            new ItemCarrinho("Produto", 100.0, 1, 0.5)
        );

        RequisicaoResumo requisicao = criarRequisicao(itens, "EXPRESSA", null, "INVALIDA", 1, "BRONZE", "SUDESTE");
        Object resultado = checkoutService.calcularResumo(requisicao);

        assertTrue(resultado instanceof RespostaErro);
        RespostaErro erro = (RespostaErro) resultado;
        assertEquals("FORMA_PAGAMENTO_INVALIDA", erro.getErro());
    }

    @Test
    void testParcelamentoInvalidoPixAcima1() {
        List<ItemCarrinho> itens = Arrays.asList(
            new ItemCarrinho("Produto", 100.0, 1, 0.5)
        );

        RequisicaoResumo requisicao = criarRequisicao(itens, "EXPRESSA", null, "PIX", 2, "BRONZE", "SUDESTE");
        Object resultado = checkoutService.calcularResumo(requisicao);

        assertTrue(resultado instanceof RespostaErro);
        RespostaErro erro = (RespostaErro) resultado;
        assertEquals("PARCELAMENTO_INVALIDO", erro.getErro());
    }

    @Test
    void testParcelamentoInvalidoCartaoAcima12() {
        List<ItemCarrinho> itens = Arrays.asList(
            new ItemCarrinho("Produto", 100.0, 1, 0.5)
        );

        RequisicaoResumo requisicao = criarRequisicao(itens, "EXPRESSA", null, "CARTAO", 13, "BRONZE", "SUDESTE");
        Object resultado = checkoutService.calcularResumo(requisicao);

        assertTrue(resultado instanceof RespostaErro);
        RespostaErro erro = (RespostaErro) resultado;
        assertEquals("PARCELAMENTO_INVALIDO", erro.getErro());
    }

    @Test
    void testBoletoAcima1000() {
        List<ItemCarrinho> itens = Arrays.asList(
            new ItemCarrinho("Produto", 1000.0, 1, 0.5)
        );

        RequisicaoResumo requisicao = criarRequisicao(itens, "EXPRESSA", null, "BOLETO", 1, "BRONZE", "SUDESTE");
        Object resultado = checkoutService.calcularResumo(requisicao);

        assertTrue(resultado instanceof RespostaErro);
        RespostaErro erro = (RespostaErro) resultado;
        assertEquals("FORMA_PAGAMENTO_INDISPONIVEL", erro.getErro());
    }

    @Test
    void testFretegratisAplicaCorreto() {
        List<ItemCarrinho> itens = Arrays.asList(
            new ItemCarrinho("Produto", 100.0, 1, 0.5)
        );

        RequisicaoResumo requisicao = criarRequisicao(itens, "EXPRESSA", "FRETEGRATIS", "PIX", 1, "BRONZE", "SUDESTE");
        Object resultado = checkoutService.calcularResumo(requisicao);

        assertTrue(resultado instanceof RespostaResumo);
        RespostaResumo resposta = (RespostaResumo) resultado;

        assertEquals(new BigDecimal("27.25"), resposta.getFrete());
        assertEquals(new BigDecimal("27.25"), resposta.getDescontoCupom());
    }

    private RequisicaoResumo criarRequisicao(List<ItemCarrinho> itens, String modalidade, String cupom,
                                            String formaPagamento, Integer parcelas, String nivelClube, String regiao) {
        RequisicaoResumo requisicao = new RequisicaoResumo();
        requisicao.setItens(itens);
        requisicao.setModalidadeEntrega(modalidade);
        requisicao.setCupom(cupom);
        requisicao.setFormaPagamento(formaPagamento);
        requisicao.setParcelas(parcelas);
        requisicao.setNivelClube(nivelClube);
        requisicao.setRegiao(regiao);
        return requisicao;
    }

    private void assertBigDecimal(BigDecimal expected, BigDecimal actual) {
        assertEquals(expected.setScale(2, java.math.RoundingMode.HALF_EVEN),
                    actual.setScale(2, java.math.RoundingMode.HALF_EVEN));
    }
}

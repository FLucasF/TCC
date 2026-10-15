package com.loja.checkout.service;

import com.loja.checkout.dto.Item;
import com.loja.checkout.dto.RequisicaoCheckout;
import com.loja.checkout.dto.ResumoCheckout;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.Collections;

import static org.junit.jupiter.api.Assertions.*;

class CheckoutServiceTest {
    private CheckoutService service;

    @BeforeEach
    void setUp() {
        service = new CheckoutService();
    }

    @Test
    void exemplo1_ProdutosComCupom10PercentoPixExpressa() throws CheckoutException {
        RequisicaoCheckout requisicao = criarRequisicao(
            Arrays.asList(
                new Item("Camiseta", 79.90, 2, 0.30),
                new Item("Tênis", 249.90, 1, 1.20)
            ),
            "EXPRESSA",
            "BEMVINDO10",
            "PIX",
            1
        );

        ResumoCheckout resumo = service.calcularResumo(requisicao);

        assertEquals(409.70, resumo.getSubtotalProdutos());
        assertEquals(40.97, resumo.getDescontoCupom());
        assertEquals(33.10, resumo.getFrete());
        assertEquals(2, resumo.getPrazoEntregaDias());
        assertEquals(-20.09, resumo.getAjustePagamento());
        assertEquals(381.74, resumo.getTotalFinal());
        assertEquals(1, resumo.getParcelas());
        assertEquals(381.74, resumo.getValorParcela());
    }

    @Test
    void exemplo2_SemCupomCartao6xEconomica() throws CheckoutException {
        RequisicaoCheckout requisicao = criarRequisicao(
            Arrays.asList(
                new Item("Camiseta", 79.90, 2, 0.30),
                new Item("Tênis", 249.90, 1, 1.20)
            ),
            "ECONOMICA",
            null,
            "CARTAO",
            6
        );

        ResumoCheckout resumo = service.calcularResumo(requisicao);

        assertEquals(409.70, resumo.getSubtotalProdutos());
        assertEquals(0.00, resumo.getDescontoCupom());
        assertEquals(15.60, resumo.getFrete());
        assertEquals(7, resumo.getPrazoEntregaDias());
        assertEquals(30.10, resumo.getAjustePagamento());
        assertEquals(455.40, resumo.getTotalFinal());
        assertEquals(6, resumo.getParcelas());
        assertEquals(75.90, resumo.getValorParcela());
    }

    @Test
    void exemplo3_FoneComCupomMenos50BoletoMotoboy() throws CheckoutException {
        RequisicaoCheckout requisicao = criarRequisicao(
            Collections.singletonList(
                new Item("Fone", 199.90, 2, 0.25)
            ),
            "MOTOBOY",
            "MENOS50",
            "BOLETO",
            1
        );

        ResumoCheckout resumo = service.calcularResumo(requisicao);

        assertEquals(399.80, resumo.getSubtotalProdutos());
        assertEquals(50.00, resumo.getDescontoCupom());
        assertEquals(18.00, resumo.getFrete());
        assertEquals(0, resumo.getPrazoEntregaDias());
        assertEquals(3.49, resumo.getAjustePagamento());
        assertEquals(371.29, resumo.getTotalFinal());
        assertEquals(1, resumo.getParcelas());
        assertEquals(371.29, resumo.getValorParcela());
    }

    @Test
    void exemplo4_MeiaECamisetaComCupomLeve3Pague2CartaoRetirada() throws CheckoutException {
        RequisicaoCheckout requisicao = criarRequisicao(
            Arrays.asList(
                new Item("Meia", 19.90, 7, 0.10),
                new Item("Camiseta", 79.90, 2, 0.30)
            ),
            "RETIRADA_LOJA",
            "LEVE3PAGUE2",
            "CARTAO",
            3
        );

        ResumoCheckout resumo = service.calcularResumo(requisicao);

        assertEquals(299.10, resumo.getSubtotalProdutos());
        assertEquals(39.80, resumo.getDescontoCupom());
        assertEquals(0.00, resumo.getFrete());
        assertEquals(1, resumo.getPrazoEntregaDias());
        assertEquals(0.00, resumo.getAjustePagamento());
        assertEquals(259.30, resumo.getTotalFinal());
        assertEquals(3, resumo.getParcelas());
        assertEquals(86.43, resumo.getValorParcela());
    }

    @Test
    void carrinhoVazio_DeveRetornarPEDIDO_INVALIDO() {
        RequisicaoCheckout requisicao = criarRequisicao(
            Collections.emptyList(),
            "RETIRADA_LOJA",
            null,
            "PIX",
            1
        );

        CheckoutException exception = assertThrows(CheckoutException.class,
            () -> service.calcularResumo(requisicao));
        assertEquals("PEDIDO_INVALIDO", exception.getCodigo());
    }

    @Test
    void precoUnitarioZero_DeveRetornarPEDIDO_INVALIDO() {
        RequisicaoCheckout requisicao = criarRequisicao(
            Collections.singletonList(
                new Item("Produto", 0.0, 1, 0.5)
            ),
            "RETIRADA_LOJA",
            null,
            "PIX",
            1
        );

        CheckoutException exception = assertThrows(CheckoutException.class,
            () -> service.calcularResumo(requisicao));
        assertEquals("PEDIDO_INVALIDO", exception.getCodigo());
    }

    @Test
    void quantidadeZero_DeveRetornarPEDIDO_INVALIDO() {
        RequisicaoCheckout requisicao = criarRequisicao(
            Collections.singletonList(
                new Item("Produto", 100.0, 0, 0.5)
            ),
            "RETIRADA_LOJA",
            null,
            "PIX",
            1
        );

        CheckoutException exception = assertThrows(CheckoutException.class,
            () -> service.calcularResumo(requisicao));
        assertEquals("PEDIDO_INVALIDO", exception.getCodigo());
    }

    @Test
    void pesoNegativo_DeveRetornarPEDIDO_INVALIDO() {
        RequisicaoCheckout requisicao = criarRequisicao(
            Collections.singletonList(
                new Item("Produto", 100.0, 1, -0.5)
            ),
            "RETIRADA_LOJA",
            null,
            "PIX",
            1
        );

        CheckoutException exception = assertThrows(CheckoutException.class,
            () -> service.calcularResumo(requisicao));
        assertEquals("PEDIDO_INVALIDO", exception.getCodigo());
    }

    @Test
    void modalidadeInvalida_DeveRetornarMODALIDADE_INVALIDA() {
        RequisicaoCheckout requisicao = criarRequisicao(
            Collections.singletonList(
                new Item("Produto", 100.0, 1, 0.5)
            ),
            "INVALIDA",
            null,
            "PIX",
            1
        );

        CheckoutException exception = assertThrows(CheckoutException.class,
            () -> service.calcularResumo(requisicao));
        assertEquals("MODALIDADE_INVALIDA", exception.getCodigo());
    }

    @Test
    void modalidadeVazia_DeveRetornarMODALIDADE_INVALIDA() {
        RequisicaoCheckout requisicao = criarRequisicao(
            Collections.singletonList(
                new Item("Produto", 100.0, 1, 0.5)
            ),
            "",
            null,
            "PIX",
            1
        );

        CheckoutException exception = assertThrows(CheckoutException.class,
            () -> service.calcularResumo(requisicao));
        assertEquals("MODALIDADE_INVALIDA", exception.getCodigo());
    }

    @Test
    void motoboyAcima5Kg_DeveRetornarMODALIDADE_INDISPONIVEL() {
        RequisicaoCheckout requisicao = criarRequisicao(
            Collections.singletonList(
                new Item("Produto", 100.0, 1, 5.1)
            ),
            "MOTOBOY",
            null,
            "PIX",
            1
        );

        CheckoutException exception = assertThrows(CheckoutException.class,
            () -> service.calcularResumo(requisicao));
        assertEquals("MODALIDADE_INDISPONIVEL", exception.getCodigo());
    }

    @Test
    void cupomInvalido_DeveRetornarCUPOM_INVALIDO() {
        RequisicaoCheckout requisicao = criarRequisicao(
            Collections.singletonList(
                new Item("Produto", 100.0, 1, 0.5)
            ),
            "RETIRADA_LOJA",
            "CUPOMINVALIDO",
            "PIX",
            1
        );

        CheckoutException exception = assertThrows(CheckoutException.class,
            () -> service.calcularResumo(requisicao));
        assertEquals("CUPOM_INVALIDO", exception.getCodigo());
    }

    @Test
    void cupomMenos50AbaixoDe300_DeveRetornarCUPOM_NAO_APLICAVEL() {
        RequisicaoCheckout requisicao = criarRequisicao(
            Collections.singletonList(
                new Item("Produto", 250.0, 1, 0.5)
            ),
            "RETIRADA_LOJA",
            "MENOS50",
            "PIX",
            1
        );

        CheckoutException exception = assertThrows(CheckoutException.class,
            () -> service.calcularResumo(requisicao));
        assertEquals("CUPOM_NAO_APLICAVEL", exception.getCodigo());
    }

    @Test
    void formaPagamentoInvalida_DeveRetornarFORMA_PAGAMENTO_INVALIDA() {
        RequisicaoCheckout requisicao = criarRequisicao(
            Collections.singletonList(
                new Item("Produto", 100.0, 1, 0.5)
            ),
            "RETIRADA_LOJA",
            null,
            "INVALIDA",
            1
        );

        CheckoutException exception = assertThrows(CheckoutException.class,
            () -> service.calcularResumo(requisicao));
        assertEquals("FORMA_PAGAMENTO_INVALIDA", exception.getCodigo());
    }

    @Test
    void formaPagamentoVazia_DeveRetornarFORMA_PAGAMENTO_INVALIDA() {
        RequisicaoCheckout requisicao = criarRequisicao(
            Collections.singletonList(
                new Item("Produto", 100.0, 1, 0.5)
            ),
            "RETIRADA_LOJA",
            null,
            "",
            1
        );

        CheckoutException exception = assertThrows(CheckoutException.class,
            () -> service.calcularResumo(requisicao));
        assertEquals("FORMA_PAGAMENTO_INVALIDA", exception.getCodigo());
    }

    @Test
    void pixCom2Parcelas_DeveRetornarPARCELAMENTO_INVALIDO() {
        RequisicaoCheckout requisicao = criarRequisicao(
            Collections.singletonList(
                new Item("Produto", 100.0, 1, 0.5)
            ),
            "RETIRADA_LOJA",
            null,
            "PIX",
            2
        );

        CheckoutException exception = assertThrows(CheckoutException.class,
            () -> service.calcularResumo(requisicao));
        assertEquals("PARCELAMENTO_INVALIDO", exception.getCodigo());
    }

    @Test
    void boletoCom2Parcelas_DeveRetornarPARCELAMENTO_INVALIDO() {
        RequisicaoCheckout requisicao = criarRequisicao(
            Collections.singletonList(
                new Item("Produto", 100.0, 1, 0.5)
            ),
            "RETIRADA_LOJA",
            null,
            "BOLETO",
            2
        );

        CheckoutException exception = assertThrows(CheckoutException.class,
            () -> service.calcularResumo(requisicao));
        assertEquals("PARCELAMENTO_INVALIDO", exception.getCodigo());
    }

    @Test
    void cartaoCom13Parcelas_DeveRetornarPARCELAMENTO_INVALIDO() {
        RequisicaoCheckout requisicao = criarRequisicao(
            Collections.singletonList(
                new Item("Produto", 100.0, 1, 0.5)
            ),
            "RETIRADA_LOJA",
            null,
            "CARTAO",
            13
        );

        CheckoutException exception = assertThrows(CheckoutException.class,
            () -> service.calcularResumo(requisicao));
        assertEquals("PARCELAMENTO_INVALIDO", exception.getCodigo());
    }

    @Test
    void boletoAcimaDe1000_DeveRetornarFORMA_PAGAMENTO_INDISPONIVEL() {
        RequisicaoCheckout requisicao = criarRequisicao(
            Collections.singletonList(
                new Item("Produto", 1001.0, 1, 0.5)
            ),
            "RETIRADA_LOJA",
            null,
            "BOLETO",
            1
        );

        CheckoutException exception = assertThrows(CheckoutException.class,
            () -> service.calcularResumo(requisicao));
        assertEquals("FORMA_PAGAMENTO_INDISPONIVEL", exception.getCodigo());
    }

    @Test
    void fretegratisRemoveValorDoFrete() throws CheckoutException {
        RequisicaoCheckout requisicao = criarRequisicao(
            Collections.singletonList(
                new Item("Produto", 100.0, 1, 1.0)
            ),
            "EXPRESSA",
            "FRETEGRATIS",
            "PIX",
            1
        );

        ResumoCheckout resumo = service.calcularResumo(requisicao);

        assertEquals(100.0, resumo.getSubtotalProdutos());
        assertEquals(29.50, resumo.getDescontoCupom());
        assertEquals(29.50, resumo.getFrete());
        assertEquals(-3.53, resumo.getAjustePagamento());
        assertEquals(66.97, resumo.getTotalFinal());
    }

    @Test
    void leve3Pague2Com6ItemsSomaDescontoCorretamente() throws CheckoutException {
        RequisicaoCheckout requisicao = criarRequisicao(
            Collections.singletonList(
                new Item("Produto", 100.0, 6, 0.5)
            ),
            "RETIRADA_LOJA",
            "LEVE3PAGUE2",
            "PIX",
            1
        );

        ResumoCheckout resumo = service.calcularResumo(requisicao);

        assertEquals(600.0, resumo.getSubtotalProdutos());
        assertEquals(200.0, resumo.getDescontoCupom());
        assertEquals(0.0, resumo.getFrete());
        assertEquals(-20.00, resumo.getAjustePagamento());
        assertEquals(380.0, resumo.getTotalFinal());
    }

    private RequisicaoCheckout criarRequisicao(java.util.List<Item> itens, String modalidade,
                                                String cupom, String formaPagamento, Integer parcelas) {
        RequisicaoCheckout requisicao = new RequisicaoCheckout();
        requisicao.itens = itens;
        requisicao.modalidadeEntrega = modalidade;
        requisicao.cupom = cupom;
        requisicao.formaPagamento = formaPagamento;
        requisicao.parcelas = parcelas;
        return requisicao;
    }
}

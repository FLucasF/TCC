package com.loja.service;

import com.loja.dto.ItemPedido;
import com.loja.dto.ResumoRequisicao;
import com.loja.dto.ResumoResposta;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class ResumoServiceTest {

    private ResumoService service;

    @BeforeEach
    void setup() {
        service = new ResumoService();
    }

    @Test
    void exemplo1_CamisetaTenisExpressaBemvindo10Pix() {
        ResumoRequisicao req = new ResumoRequisicao();
        req.setItens(Arrays.asList(
            new ItemPedido("Camiseta", 79.90, 2, 0.30),
            new ItemPedido("Tênis", 249.90, 1, 1.20)
        ));
        req.setModalidadeEntrega("EXPRESSA");
        req.setCupom("BEMVINDO10");
        req.setFormaPagamento("PIX");
        req.setParcelas(1);

        ResumoResposta resposta = service.calcularResumo(req);

        assertEquals(409.70, resposta.getSubtotalProdutos(), 0.01);
        assertEquals(40.97, resposta.getDescontoCupom(), 0.01);
        assertEquals(33.10, resposta.getFrete(), 0.01);
        assertEquals(2, resposta.getPrazoEntregaDias());
        assertEquals(-20.09, resposta.getAjustePagamento(), 0.01);
        assertEquals(381.74, resposta.getTotalFinal(), 0.01);
        assertEquals(1, resposta.getParcelas());
        assertEquals(381.74, resposta.getValorParcela(), 0.01);
    }

    @Test
    void exemplo2_CamisetaTenisEconomicaSemCupomCartao6x() {
        ResumoRequisicao req = new ResumoRequisicao();
        req.setItens(Arrays.asList(
            new ItemPedido("Camiseta", 79.90, 2, 0.30),
            new ItemPedido("Tênis", 249.90, 1, 1.20)
        ));
        req.setModalidadeEntrega("ECONOMICA");
        req.setCupom(null);
        req.setFormaPagamento("CARTAO");
        req.setParcelas(6);

        ResumoResposta resposta = service.calcularResumo(req);

        assertEquals(409.70, resposta.getSubtotalProdutos(), 0.01);
        assertEquals(0.0, resposta.getDescontoCupom(), 0.01);
        assertEquals(15.60, resposta.getFrete(), 0.01);
        assertEquals(7, resposta.getPrazoEntregaDias());
        assertEquals(30.10, resposta.getAjustePagamento(), 0.01);
        assertEquals(455.40, resposta.getTotalFinal(), 0.01);
        assertEquals(6, resposta.getParcelas());
        assertEquals(75.90, resposta.getValorParcela(), 0.01);
    }

    @Test
    void exemplo3_FoneMotoboySem50Boleto() {
        ResumoRequisicao req = new ResumoRequisicao();
        req.setItens(Arrays.asList(
            new ItemPedido("Fone", 199.90, 2, 0.25)
        ));
        req.setModalidadeEntrega("MOTOBOY");
        req.setCupom("MENOS50");
        req.setFormaPagamento("BOLETO");
        req.setParcelas(1);

        ResumoResposta resposta = service.calcularResumo(req);

        assertEquals(399.80, resposta.getSubtotalProdutos(), 0.01);
        assertEquals(50.00, resposta.getDescontoCupom(), 0.01);
        assertEquals(18.00, resposta.getFrete(), 0.01);
        assertEquals(0, resposta.getPrazoEntregaDias());
        assertEquals(3.49, resposta.getAjustePagamento(), 0.01);
        assertEquals(371.29, resposta.getTotalFinal(), 0.01);
        assertEquals(1, resposta.getParcelas());
        assertEquals(371.29, resposta.getValorParcela(), 0.01);
    }

    @Test
    void exemplo4_MeiasCamisetasRetiraLoja_Leve3Pague2Cartao3x() {
        ResumoRequisicao req = new ResumoRequisicao();
        req.setItens(Arrays.asList(
            new ItemPedido("Meia", 19.90, 7, 0.10),
            new ItemPedido("Camiseta", 79.90, 2, 0.30)
        ));
        req.setModalidadeEntrega("RETIRADA_LOJA");
        req.setCupom("LEVE3PAGUE2");
        req.setFormaPagamento("CARTAO");
        req.setParcelas(3);

        ResumoResposta resposta = service.calcularResumo(req);

        assertEquals(299.10, resposta.getSubtotalProdutos(), 0.01);
        assertEquals(39.80, resposta.getDescontoCupom(), 0.01);
        assertEquals(0.0, resposta.getFrete(), 0.01);
        assertEquals(1, resposta.getPrazoEntregaDias());
        assertEquals(0.0, resposta.getAjustePagamento(), 0.01);
        assertEquals(259.30, resposta.getTotalFinal(), 0.01);
        assertEquals(3, resposta.getParcelas());
        assertEquals(86.43, resposta.getValorParcela(), 0.01);
    }

    @Test
    void erroCarrinhoVazio() {
        ResumoRequisicao req = new ResumoRequisicao();
        req.setItens(Arrays.asList());
        req.setModalidadeEntrega("ECONOMICA");
        req.setFormaPagamento("PIX");

        ResumoService.ValidationException ex = assertThrows(ResumoService.ValidationException.class,
            () -> service.calcularResumo(req));

        assertEquals("PEDIDO_INVALIDO", ex.getCodigo());
    }

    @Test
    void erroItemComPrecoNegativo() {
        ResumoRequisicao req = new ResumoRequisicao();
        req.setItens(Arrays.asList(
            new ItemPedido("Produto", -10.0, 1, 0.5)
        ));
        req.setModalidadeEntrega("ECONOMICA");
        req.setFormaPagamento("PIX");

        ResumoService.ValidationException ex = assertThrows(ResumoService.ValidationException.class,
            () -> service.calcularResumo(req));

        assertEquals("PEDIDO_INVALIDO", ex.getCodigo());
    }

    @Test
    void erroModalidadeInvalida() {
        ResumoRequisicao req = new ResumoRequisicao();
        req.setItens(Arrays.asList(
            new ItemPedido("Produto", 10.0, 1, 0.5)
        ));
        req.setModalidadeEntrega("INVALIDA");
        req.setFormaPagamento("PIX");

        ResumoService.ValidationException ex = assertThrows(ResumoService.ValidationException.class,
            () -> service.calcularResumo(req));

        assertEquals("MODALIDADE_INVALIDA", ex.getCodigo());
    }

    @Test
    void erroMotoboySobrepassaPeso() {
        ResumoRequisicao req = new ResumoRequisicao();
        req.setItens(Arrays.asList(
            new ItemPedido("Produto", 10.0, 1, 5.1)
        ));
        req.setModalidadeEntrega("MOTOBOY");
        req.setFormaPagamento("PIX");

        ResumoService.ValidationException ex = assertThrows(ResumoService.ValidationException.class,
            () -> service.calcularResumo(req));

        assertEquals("MODALIDADE_INDISPONIVEL", ex.getCodigo());
    }

    @Test
    void erroCupomInvalido() {
        ResumoRequisicao req = new ResumoRequisicao();
        req.setItens(Arrays.asList(
            new ItemPedido("Produto", 10.0, 1, 0.5)
        ));
        req.setModalidadeEntrega("ECONOMICA");
        req.setCupom("INVALIDO");
        req.setFormaPagamento("PIX");

        ResumoService.ValidationException ex = assertThrows(ResumoService.ValidationException.class,
            () -> service.calcularResumo(req));

        assertEquals("CUPOM_INVALIDO", ex.getCodigo());
    }

    @Test
    void erroCupomMenos50AbaixoDeMinimo() {
        ResumoRequisicao req = new ResumoRequisicao();
        req.setItens(Arrays.asList(
            new ItemPedido("Produto", 100.0, 2, 0.5)
        ));
        req.setModalidadeEntrega("ECONOMICA");
        req.setCupom("MENOS50");
        req.setFormaPagamento("PIX");

        ResumoService.ValidationException ex = assertThrows(ResumoService.ValidationException.class,
            () -> service.calcularResumo(req));

        assertEquals("CUPOM_NAO_APLICAVEL", ex.getCodigo());
    }

    @Test
    void erroFormaPagamentoInvalida() {
        ResumoRequisicao req = new ResumoRequisicao();
        req.setItens(Arrays.asList(
            new ItemPedido("Produto", 10.0, 1, 0.5)
        ));
        req.setModalidadeEntrega("ECONOMICA");
        req.setFormaPagamento("INVALIDA");

        ResumoService.ValidationException ex = assertThrows(ResumoService.ValidationException.class,
            () -> service.calcularResumo(req));

        assertEquals("FORMA_PAGAMENTO_INVALIDA", ex.getCodigo());
    }

    @Test
    void erroParcelasInvalidasPix() {
        ResumoRequisicao req = new ResumoRequisicao();
        req.setItens(Arrays.asList(
            new ItemPedido("Produto", 10.0, 1, 0.5)
        ));
        req.setModalidadeEntrega("ECONOMICA");
        req.setFormaPagamento("PIX");
        req.setParcelas(2);

        ResumoService.ValidationException ex = assertThrows(ResumoService.ValidationException.class,
            () -> service.calcularResumo(req));

        assertEquals("PARCELAMENTO_INVALIDO", ex.getCodigo());
    }

    @Test
    void erroParcelasInvalidasCartao() {
        ResumoRequisicao req = new ResumoRequisicao();
        req.setItens(Arrays.asList(
            new ItemPedido("Produto", 10.0, 1, 0.5)
        ));
        req.setModalidadeEntrega("ECONOMICA");
        req.setFormaPagamento("CARTAO");
        req.setParcelas(13);

        ResumoService.ValidationException ex = assertThrows(ResumoService.ValidationException.class,
            () -> service.calcularResumo(req));

        assertEquals("PARCELAMENTO_INVALIDO", ex.getCodigo());
    }

    @Test
    void erroBoletoAcimaDe1000() {
        ResumoRequisicao req = new ResumoRequisicao();
        req.setItens(Arrays.asList(
            new ItemPedido("Produto", 1001.0, 1, 0.5)
        ));
        req.setModalidadeEntrega("RETIRADA_LOJA");
        req.setFormaPagamento("BOLETO");

        ResumoService.ValidationException ex = assertThrows(ResumoService.ValidationException.class,
            () -> service.calcularResumo(req));

        assertEquals("FORMA_PAGAMENTO_INDISPONIVEL", ex.getCodigo());
    }

    @Test
    void cupomFreteGratisComFrete() {
        ResumoRequisicao req = new ResumoRequisicao();
        req.setItens(Arrays.asList(
            new ItemPedido("Produto", 100.0, 1, 1.0)
        ));
        req.setModalidadeEntrega("EXPRESSA");
        req.setCupom("FRETEGRATIS");
        req.setFormaPagamento("PIX");

        ResumoResposta resposta = service.calcularResumo(req);

        Double freteNormal = 25.0 + (1.0 * 4.50);
        assertEquals(25.0 + 4.50, resposta.getFrete(), 0.01);
        assertEquals(freteNormal, resposta.getDescontoCupom(), 0.01);
        assertEquals(100.0, resposta.getSubtotalProdutos(), 0.01);
    }
}

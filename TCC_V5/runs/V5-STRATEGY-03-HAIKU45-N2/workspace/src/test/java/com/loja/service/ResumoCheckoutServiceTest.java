package com.loja.service;

import com.loja.model.ItemPedido;
import com.loja.model.RequisicaoResumo;
import com.loja.model.RespostaResumo;
import java.math.BigDecimal;
import java.util.Arrays;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

public class ResumoCheckoutServiceTest {

    private ResumoCheckoutService service;

    @BeforeEach
    public void setup() {
        service = new ResumoCheckoutService();
    }

    @Test
    public void exemplo1_BemvindeComExpressa() {
        RequisicaoResumo req = new RequisicaoResumo();
        req.setItens(Arrays.asList(
                new ItemPedido("Camiseta", 79.90, 2, 0.30),
                new ItemPedido("Tênis", 249.90, 1, 1.20)
        ));
        req.setModalidadeEntrega("EXPRESSA");
        req.setCupom("BEMVINDO10");
        req.setFormaPagamento("PIX");
        req.setNivelClube("BRONZE");
        req.setRegiao("NORTE");

        Object resultado = service.calcularResumo(req);
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
        assertFalse(resposta.getBrinde());
    }

    @Test
    public void exemplo2_EconomicaCartao6Vezes() {
        RequisicaoResumo req = new RequisicaoResumo();
        req.setItens(Arrays.asList(
                new ItemPedido("Camiseta", 79.90, 2, 0.30),
                new ItemPedido("Tênis", 249.90, 1, 1.20)
        ));
        req.setModalidadeEntrega("ECONOMICA");
        req.setFormaPagamento("CARTAO");
        req.setParcelas(6);
        req.setNivelClube("PRATA");
        req.setRegiao("CENTRO_OESTE");

        Object resultado = service.calcularResumo(req);
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
        assertFalse(resposta.getBrinde());
    }

    @Test
    public void exemplo3_MotoboyCupomMenos50() {
        RequisicaoResumo req = new RequisicaoResumo();
        req.setItens(Arrays.asList(
                new ItemPedido("Fone", 199.90, 2, 0.25)
        ));
        req.setModalidadeEntrega("MOTOBOY");
        req.setCupom("MENOS50");
        req.setFormaPagamento("BOLETO");
        req.setNivelClube("BRONZE");
        req.setRegiao("NORDESTE");

        Object resultado = service.calcularResumo(req);
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
        assertFalse(resposta.getBrinde());
    }

    @Test
    public void exemplo4_Leve3Pague2ComRetirada() {
        RequisicaoResumo req = new RequisicaoResumo();
        req.setItens(Arrays.asList(
                new ItemPedido("Meia", 19.90, 7, 0.10),
                new ItemPedido("Camiseta", 79.90, 2, 0.30)
        ));
        req.setModalidadeEntrega("RETIRADA_LOJA");
        req.setCupom("LEVE3PAGUE2");
        req.setFormaPagamento("CARTAO");
        req.setParcelas(3);
        req.setNivelClube("PRATA");
        req.setRegiao("SUL");

        Object resultado = service.calcularResumo(req);
        RespostaResumo resposta = (RespostaResumo) resultado;

        assertEquals(new BigDecimal("299.10"), resposta.getSubtotalProdutos());
        assertEquals(new BigDecimal("39.80"), resposta.getDescontoCupom());
        assertEquals(new BigDecimal("0.00"), resposta.getFrete());
        assertEquals(1, resposta.getPrazoEntregaDias());
        assertEquals(new BigDecimal("2.99"), resposta.getSeguro());
        assertEquals(new BigDecimal("0.00"), resposta.getAjustePagamento());
        assertEquals(new BigDecimal("262.29"), resposta.getTotalFinal());
        assertEquals(3, resposta.getParcelas());
        assertEquals(new BigDecimal("87.43"), resposta.getValorParcela());
        assertEquals(new BigDecimal("5.98"), resposta.getCreditoProximaCompra());
        assertFalse(resposta.getBrinde());
    }

    @Test
    public void exemplo5_OuroSemFrete() {
        RequisicaoResumo req = new RequisicaoResumo();
        req.setItens(Arrays.asList(
                new ItemPedido("Camiseta", 79.90, 2, 0.30),
                new ItemPedido("Tênis", 249.90, 1, 1.20)
        ));
        req.setModalidadeEntrega("EXPRESSA");
        req.setFormaPagamento("PIX");
        req.setNivelClube("OURO");
        req.setRegiao("SUDESTE");

        Object resultado = service.calcularResumo(req);
        RespostaResumo resposta = (RespostaResumo) resultado;

        assertEquals(new BigDecimal("409.70"), resposta.getSubtotalProdutos());
        assertEquals(new BigDecimal("0.00"), resposta.getDescontoCupom());
        assertEquals(new BigDecimal("0.00"), resposta.getFrete());
        assertEquals(2, resposta.getPrazoEntregaDias());
        assertEquals(new BigDecimal("4.10"), resposta.getSeguro());
        assertEquals(new BigDecimal("-20.69"), resposta.getAjustePagamento());
        assertEquals(new BigDecimal("393.11"), resposta.getTotalFinal());
        assertEquals(1, resposta.getParcelas());
        assertEquals(new BigDecimal("393.11"), resposta.getValorParcela());
        assertEquals(new BigDecimal("20.48"), resposta.getCreditoProximaCompra());
        assertFalse(resposta.getBrinde());
    }

    @Test
    public void carrinhoVazio() {
        RequisicaoResumo req = new RequisicaoResumo();
        req.setItens(Arrays.asList());
        req.setModalidadeEntrega("ECONOMICA");
        req.setFormaPagamento("PIX");
        req.setNivelClube("BRONZE");
        req.setRegiao("SUDESTE");

        Object resultado = service.calcularResumo(req);
        assertTrue(resultado instanceof com.loja.model.RespostaErro);
        assertEquals("PEDIDO_INVALIDO", ((com.loja.model.RespostaErro) resultado).getErro());
    }

    @Test
    public void nivelClubeInvalido() {
        RequisicaoResumo req = new RequisicaoResumo();
        req.setItens(Arrays.asList(
                new ItemPedido("Item", 10.00, 1, 0.10)
        ));
        req.setModalidadeEntrega("ECONOMICA");
        req.setFormaPagamento("PIX");
        req.setNivelClube("INVALIDO");
        req.setRegiao("SUDESTE");

        Object resultado = service.calcularResumo(req);
        assertTrue(resultado instanceof com.loja.model.RespostaErro);
        assertEquals("NIVEL_CLUBE_INVALIDO", ((com.loja.model.RespostaErro) resultado).getErro());
    }

    @Test
    public void regiaoInvalida() {
        RequisicaoResumo req = new RequisicaoResumo();
        req.setItens(Arrays.asList(
                new ItemPedido("Item", 10.00, 1, 0.10)
        ));
        req.setModalidadeEntrega("ECONOMICA");
        req.setFormaPagamento("PIX");
        req.setNivelClube("BRONZE");
        req.setRegiao("INVALIDA");

        Object resultado = service.calcularResumo(req);
        assertTrue(resultado instanceof com.loja.model.RespostaErro);
        assertEquals("REGIAO_INVALIDA", ((com.loja.model.RespostaErro) resultado).getErro());
    }

    @Test
    public void modalidadeInvalida() {
        RequisicaoResumo req = new RequisicaoResumo();
        req.setItens(Arrays.asList(
                new ItemPedido("Item", 10.00, 1, 0.10)
        ));
        req.setModalidadeEntrega("INVALIDA");
        req.setFormaPagamento("PIX");
        req.setNivelClube("BRONZE");
        req.setRegiao("SUDESTE");

        Object resultado = service.calcularResumo(req);
        assertTrue(resultado instanceof com.loja.model.RespostaErro);
        assertEquals("MODALIDADE_INVALIDA", ((com.loja.model.RespostaErro) resultado).getErro());
    }

    @Test
    public void motoboyAcimaDoPeso() {
        RequisicaoResumo req = new RequisicaoResumo();
        req.setItens(Arrays.asList(
                new ItemPedido("Item", 100.00, 1, 6.00)
        ));
        req.setModalidadeEntrega("MOTOBOY");
        req.setFormaPagamento("PIX");
        req.setNivelClube("BRONZE");
        req.setRegiao("SUDESTE");

        Object resultado = service.calcularResumo(req);
        assertTrue(resultado instanceof com.loja.model.RespostaErro);
        assertEquals("MODALIDADE_INDISPONIVEL", ((com.loja.model.RespostaErro) resultado).getErro());
    }

    @Test
    public void cupomInvalido() {
        RequisicaoResumo req = new RequisicaoResumo();
        req.setItens(Arrays.asList(
                new ItemPedido("Item", 10.00, 1, 0.10)
        ));
        req.setModalidadeEntrega("ECONOMICA");
        req.setFormaPagamento("PIX");
        req.setNivelClube("BRONZE");
        req.setRegiao("SUDESTE");
        req.setCupom("INVALIDO");

        Object resultado = service.calcularResumo(req);
        assertTrue(resultado instanceof com.loja.model.RespostaErro);
        assertEquals("CUPOM_INVALIDO", ((com.loja.model.RespostaErro) resultado).getErro());
    }

    @Test
    public void cupomMenos50NaoAplicavel() {
        RequisicaoResumo req = new RequisicaoResumo();
        req.setItens(Arrays.asList(
                new ItemPedido("Item", 100.00, 1, 0.10)
        ));
        req.setModalidadeEntrega("ECONOMICA");
        req.setFormaPagamento("PIX");
        req.setNivelClube("BRONZE");
        req.setRegiao("SUDESTE");
        req.setCupom("MENOS50");

        Object resultado = service.calcularResumo(req);
        assertTrue(resultado instanceof com.loja.model.RespostaErro);
        assertEquals("CUPOM_NAO_APLICAVEL", ((com.loja.model.RespostaErro) resultado).getErro());
    }

    @Test
    public void formaPagamentoInvalida() {
        RequisicaoResumo req = new RequisicaoResumo();
        req.setItens(Arrays.asList(
                new ItemPedido("Item", 10.00, 1, 0.10)
        ));
        req.setModalidadeEntrega("ECONOMICA");
        req.setFormaPagamento("INVALIDA");
        req.setNivelClube("BRONZE");
        req.setRegiao("SUDESTE");

        Object resultado = service.calcularResumo(req);
        assertTrue(resultado instanceof com.loja.model.RespostaErro);
        assertEquals("FORMA_PAGAMENTO_INVALIDA", ((com.loja.model.RespostaErro) resultado).getErro());
    }

    @Test
    public void parcelamentoInvalidoPixComParcelas() {
        RequisicaoResumo req = new RequisicaoResumo();
        req.setItens(Arrays.asList(
                new ItemPedido("Item", 100.00, 1, 0.10)
        ));
        req.setModalidadeEntrega("ECONOMICA");
        req.setFormaPagamento("PIX");
        req.setNivelClube("BRONZE");
        req.setRegiao("SUDESTE");
        req.setParcelas(2);

        Object resultado = service.calcularResumo(req);
        assertTrue(resultado instanceof com.loja.model.RespostaErro);
        assertEquals("PARCELAMENTO_INVALIDO", ((com.loja.model.RespostaErro) resultado).getErro());
    }

    @Test
    public void parcelamentoInvalidoCartao13Vezes() {
        RequisicaoResumo req = new RequisicaoResumo();
        req.setItens(Arrays.asList(
                new ItemPedido("Item", 100.00, 1, 0.10)
        ));
        req.setModalidadeEntrega("ECONOMICA");
        req.setFormaPagamento("CARTAO");
        req.setNivelClube("BRONZE");
        req.setRegiao("SUDESTE");
        req.setParcelas(13);

        Object resultado = service.calcularResumo(req);
        assertTrue(resultado instanceof com.loja.model.RespostaErro);
        assertEquals("PARCELAMENTO_INVALIDO", ((com.loja.model.RespostaErro) resultado).getErro());
    }

    @Test
    public void boletoAcimaDoLimite() {
        RequisicaoResumo req = new RequisicaoResumo();
        req.setItens(Arrays.asList(
                new ItemPedido("Item", 1001.00, 1, 0.10)
        ));
        req.setModalidadeEntrega("RETIRADA_LOJA");
        req.setFormaPagamento("BOLETO");
        req.setNivelClube("BRONZE");
        req.setRegiao("SUDESTE");

        Object resultado = service.calcularResumo(req);
        assertTrue(resultado instanceof com.loja.model.RespostaErro);
        assertEquals("FORMA_PAGAMENTO_INDISPONIVEL", ((com.loja.model.RespostaErro) resultado).getErro());
    }

    @Test
    public void ouroComBrinde() {
        RequisicaoResumo req = new RequisicaoResumo();
        req.setItens(Arrays.asList(
                new ItemPedido("Item", 501.00, 1, 0.10)
        ));
        req.setModalidadeEntrega("RETIRADA_LOJA");
        req.setFormaPagamento("PIX");
        req.setNivelClube("OURO");
        req.setRegiao("SUDESTE");

        Object resultado = service.calcularResumo(req);
        RespostaResumo resposta = (RespostaResumo) resultado;
        assertTrue(resposta.getBrinde());
    }

    @Test
    public void ouroSemBrinde() {
        RequisicaoResumo req = new RequisicaoResumo();
        req.setItens(Arrays.asList(
                new ItemPedido("Item", 499.00, 1, 0.10)
        ));
        req.setModalidadeEntrega("RETIRADA_LOJA");
        req.setFormaPagamento("PIX");
        req.setNivelClube("OURO");
        req.setRegiao("SUDESTE");

        Object resultado = service.calcularResumo(req);
        RespostaResumo resposta = (RespostaResumo) resultado;
        assertFalse(resposta.getBrinde());
    }

    @Test
    public void itemComPrecoZero() {
        RequisicaoResumo req = new RequisicaoResumo();
        req.setItens(Arrays.asList(
                new ItemPedido("Item", 0.00, 1, 0.10)
        ));
        req.setModalidadeEntrega("ECONOMICA");
        req.setFormaPagamento("PIX");
        req.setNivelClube("BRONZE");
        req.setRegiao("SUDESTE");

        Object resultado = service.calcularResumo(req);
        assertTrue(resultado instanceof com.loja.model.RespostaErro);
        assertEquals("PEDIDO_INVALIDO", ((com.loja.model.RespostaErro) resultado).getErro());
    }

    @Test
    public void itemComQuantidadeZero() {
        RequisicaoResumo req = new RequisicaoResumo();
        req.setItens(Arrays.asList(
                new ItemPedido("Item", 10.00, 0, 0.10)
        ));
        req.setModalidadeEntrega("ECONOMICA");
        req.setFormaPagamento("PIX");
        req.setNivelClube("BRONZE");
        req.setRegiao("SUDESTE");

        Object resultado = service.calcularResumo(req);
        assertTrue(resultado instanceof com.loja.model.RespostaErro);
        assertEquals("PEDIDO_INVALIDO", ((com.loja.model.RespostaErro) resultado).getErro());
    }

    @Test
    public void fretegratisComCupom() {
        RequisicaoResumo req = new RequisicaoResumo();
        req.setItens(Arrays.asList(
                new ItemPedido("Item", 100.00, 1, 0.10)
        ));
        req.setModalidadeEntrega("ECONOMICA");
        req.setFormaPagamento("PIX");
        req.setNivelClube("BRONZE");
        req.setRegiao("SUDESTE");
        req.setCupom("FRETEGRATIS");

        Object resultado = service.calcularResumo(req);
        RespostaResumo resposta = (RespostaResumo) resultado;

        assertEquals(new BigDecimal("100.00"), resposta.getSubtotalProdutos());
        assertEquals(new BigDecimal("12.20"), resposta.getFrete());
        assertEquals(new BigDecimal("12.20"), resposta.getDescontoCupom());
        assertEquals(7, resposta.getPrazoEntregaDias());
    }
}

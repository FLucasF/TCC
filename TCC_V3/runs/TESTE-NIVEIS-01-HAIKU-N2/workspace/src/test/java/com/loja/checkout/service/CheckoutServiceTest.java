package com.loja.checkout.service;

import com.loja.checkout.enums.FormaPagamento;
import com.loja.checkout.enums.ModalidadeEntrega;
import com.loja.checkout.enums.NivelClube;
import com.loja.checkout.enums.Regiao;
import com.loja.checkout.model.CheckoutRequest;
import com.loja.checkout.model.CheckoutResponse;
import com.loja.checkout.model.ErrorResponse;
import com.loja.checkout.model.ItemCarrinho;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class CheckoutServiceTest {

    private CheckoutService service;

    @BeforeEach
    void setUp() {
        service = new CheckoutService();
    }

    @Test
    @DisplayName("Exemplo 1: Camiseta + Tênis, EXPRESSA, BEMVINDO10, PIX, BRONZE, NORTE")
    void exemplo1() {
        List<ItemCarrinho> itens = new ArrayList<>();
        itens.add(new ItemCarrinho("Camiseta", new BigDecimal("79.90"), 2, new BigDecimal("0.30")));
        itens.add(new ItemCarrinho("Tênis", new BigDecimal("249.90"), 1, new BigDecimal("1.20")));

        CheckoutRequest request = new CheckoutRequest();
        request.setItens(itens);
        request.setModalidadeEntrega(ModalidadeEntrega.EXPRESSA);
        request.setCupom("BEMVINDO10");
        request.setFormaPagamento(FormaPagamento.PIX);
        request.setParcelas(1);
        request.setNivelClube(NivelClube.BRONZE);
        request.setRegiao(Regiao.NORTE);

        Object resultado = service.calcularResumo(request);
        assertInstanceOf(CheckoutResponse.class, resultado);

        CheckoutResponse response = (CheckoutResponse) resultado;
        assertEquals(new BigDecimal("409.70"), response.getSubtotalProdutos());
        assertEquals(new BigDecimal("40.97"), response.getDescontoCupom());
        assertEquals(new BigDecimal("33.10"), response.getFrete());
        assertEquals(2, response.getPrazoEntregaDias());
        assertEquals(new BigDecimal("10.24"), response.getSeguro());
        assertEquals(new BigDecimal("-20.60"), response.getAjustePagamento());
        assertEquals(new BigDecimal("391.47"), response.getTotalFinal());
        assertEquals(1, response.getParcelas());
        assertEquals(new BigDecimal("391.47"), response.getValorParcela());
        assertEquals(new BigDecimal("0.00"), response.getCreditoProximaCompra());
        assertEquals(false, response.getBrinde());
    }

    @Test
    @DisplayName("Exemplo 2: Camiseta + Tênis, ECONOMICA, sem cupom, CARTAO 6x, PRATA, CENTRO_OESTE")
    void exemplo2() {
        List<ItemCarrinho> itens = new ArrayList<>();
        itens.add(new ItemCarrinho("Camiseta", new BigDecimal("79.90"), 2, new BigDecimal("0.30")));
        itens.add(new ItemCarrinho("Tênis", new BigDecimal("249.90"), 1, new BigDecimal("1.20")));

        CheckoutRequest request = new CheckoutRequest();
        request.setItens(itens);
        request.setModalidadeEntrega(ModalidadeEntrega.ECONOMICA);
        request.setCupom(null);
        request.setFormaPagamento(FormaPagamento.CARTAO);
        request.setParcelas(6);
        request.setNivelClube(NivelClube.PRATA);
        request.setRegiao(Regiao.CENTRO_OESTE);

        Object resultado = service.calcularResumo(request);
        assertInstanceOf(CheckoutResponse.class, resultado);

        CheckoutResponse response = (CheckoutResponse) resultado;
        assertEquals(new BigDecimal("409.70"), response.getSubtotalProdutos());
        assertEquals(new BigDecimal("0.00"), response.getDescontoCupom());
        assertEquals(new BigDecimal("15.60"), response.getFrete());
        assertEquals(7, response.getPrazoEntregaDias());
        assertEquals(new BigDecimal("6.15"), response.getSeguro());
        assertEquals(new BigDecimal("30.55"), response.getAjustePagamento());
        assertEquals(new BigDecimal("462.00"), response.getTotalFinal());
        assertEquals(6, response.getParcelas());
        assertEquals(new BigDecimal("77.00"), response.getValorParcela());
        assertEquals(new BigDecimal("8.19"), response.getCreditoProximaCompra());
        assertEquals(false, response.getBrinde());
    }

    @Test
    @DisplayName("Exemplo 3: Fone, MOTOBOY, MENOS50, BOLETO, BRONZE, NORDESTE")
    void exemplo3() {
        List<ItemCarrinho> itens = new ArrayList<>();
        itens.add(new ItemCarrinho("Fone", new BigDecimal("199.90"), 2, new BigDecimal("0.25")));

        CheckoutRequest request = new CheckoutRequest();
        request.setItens(itens);
        request.setModalidadeEntrega(ModalidadeEntrega.MOTOBOY);
        request.setCupom("MENOS50");
        request.setFormaPagamento(FormaPagamento.BOLETO);
        request.setParcelas(1);
        request.setNivelClube(NivelClube.BRONZE);
        request.setRegiao(Regiao.NORDESTE);

        Object resultado = service.calcularResumo(request);
        assertInstanceOf(CheckoutResponse.class, resultado);

        CheckoutResponse response = (CheckoutResponse) resultado;
        assertEquals(new BigDecimal("399.80"), response.getSubtotalProdutos());
        assertEquals(new BigDecimal("50.00"), response.getDescontoCupom());
        assertEquals(new BigDecimal("18.00"), response.getFrete());
        assertEquals(0, response.getPrazoEntregaDias());
        assertEquals(new BigDecimal("8.00"), response.getSeguro());
        assertEquals(new BigDecimal("3.49"), response.getAjustePagamento());
        assertEquals(new BigDecimal("379.29"), response.getTotalFinal());
        assertEquals(1, response.getParcelas());
        assertEquals(new BigDecimal("379.29"), response.getValorParcela());
        assertEquals(new BigDecimal("0.00"), response.getCreditoProximaCompra());
        assertEquals(false, response.getBrinde());
    }

    @Test
    @DisplayName("Exemplo 4: Meia + Camiseta, RETIRADA_LOJA, LEVE3PAGUE2, CARTAO 3x, PRATA, SUL")
    void exemplo4() {
        List<ItemCarrinho> itens = new ArrayList<>();
        itens.add(new ItemCarrinho("Meia", new BigDecimal("19.90"), 7, new BigDecimal("0.10")));
        itens.add(new ItemCarrinho("Camiseta", new BigDecimal("79.90"), 2, new BigDecimal("0.30")));

        CheckoutRequest request = new CheckoutRequest();
        request.setItens(itens);
        request.setModalidadeEntrega(ModalidadeEntrega.RETIRADA_LOJA);
        request.setCupom("LEVE3PAGUE2");
        request.setFormaPagamento(FormaPagamento.CARTAO);
        request.setParcelas(3);
        request.setNivelClube(NivelClube.PRATA);
        request.setRegiao(Regiao.SUL);

        Object resultado = service.calcularResumo(request);
        assertInstanceOf(CheckoutResponse.class, resultado);

        CheckoutResponse response = (CheckoutResponse) resultado;
        assertEquals(new BigDecimal("299.10"), response.getSubtotalProdutos());
        assertEquals(new BigDecimal("39.80"), response.getDescontoCupom());
        assertEquals(new BigDecimal("0.00"), response.getFrete());
        assertEquals(1, response.getPrazoEntregaDias());
        assertEquals(new BigDecimal("2.99"), response.getSeguro());
        assertEquals(new BigDecimal("0.00"), response.getAjustePagamento());
        assertEquals(new BigDecimal("262.29"), response.getTotalFinal());
        assertEquals(3, response.getParcelas());
        assertEquals(new BigDecimal("87.43"), response.getValorParcela());
        assertEquals(new BigDecimal("5.98"), response.getCreditoProximaCompra());
        assertEquals(false, response.getBrinde());
    }

    @Test
    @DisplayName("Exemplo 5: Camiseta + Tênis, EXPRESSA, sem cupom, PIX, OURO, SUDESTE")
    void exemplo5() {
        List<ItemCarrinho> itens = new ArrayList<>();
        itens.add(new ItemCarrinho("Camiseta", new BigDecimal("79.90"), 2, new BigDecimal("0.30")));
        itens.add(new ItemCarrinho("Tênis", new BigDecimal("249.90"), 1, new BigDecimal("1.20")));

        CheckoutRequest request = new CheckoutRequest();
        request.setItens(itens);
        request.setModalidadeEntrega(ModalidadeEntrega.EXPRESSA);
        request.setCupom(null);
        request.setFormaPagamento(FormaPagamento.PIX);
        request.setParcelas(1);
        request.setNivelClube(NivelClube.OURO);
        request.setRegiao(Regiao.SUDESTE);

        Object resultado = service.calcularResumo(request);
        assertInstanceOf(CheckoutResponse.class, resultado);

        CheckoutResponse response = (CheckoutResponse) resultado;
        assertEquals(new BigDecimal("409.70"), response.getSubtotalProdutos());
        assertEquals(new BigDecimal("0.00"), response.getDescontoCupom());
        assertEquals(new BigDecimal("0.00"), response.getFrete());
        assertEquals(2, response.getPrazoEntregaDias());
        assertEquals(new BigDecimal("4.10"), response.getSeguro());
        assertEquals(new BigDecimal("-20.69"), response.getAjustePagamento());
        assertEquals(new BigDecimal("393.11"), response.getTotalFinal());
        assertEquals(1, response.getParcelas());
        assertEquals(new BigDecimal("393.11"), response.getValorParcela());
        assertEquals(new BigDecimal("20.48"), response.getCreditoProximaCompra());
        assertEquals(false, response.getBrinde());
    }

    @Test
    @DisplayName("Erro: Carrinho vazio")
    void erroCarrinhoVazio() {
        CheckoutRequest request = new CheckoutRequest();
        request.setItens(new ArrayList<>());
        request.setModalidadeEntrega(ModalidadeEntrega.EXPRESSA);
        request.setFormaPagamento(FormaPagamento.PIX);
        request.setNivelClube(NivelClube.BRONZE);
        request.setRegiao(Regiao.NORTE);

        Object resultado = service.calcularResumo(request);
        assertInstanceOf(ErrorResponse.class, resultado);
        assertEquals("PEDIDO_INVALIDO", ((ErrorResponse) resultado).getErro());
    }

    @Test
    @DisplayName("Erro: Item com preço zero")
    void erroPrecoZero() {
        List<ItemCarrinho> itens = new ArrayList<>();
        itens.add(new ItemCarrinho("Produto", new BigDecimal("0"), 1, new BigDecimal("1")));

        CheckoutRequest request = new CheckoutRequest();
        request.setItens(itens);
        request.setModalidadeEntrega(ModalidadeEntrega.EXPRESSA);
        request.setFormaPagamento(FormaPagamento.PIX);
        request.setNivelClube(NivelClube.BRONZE);
        request.setRegiao(Regiao.NORTE);

        Object resultado = service.calcularResumo(request);
        assertInstanceOf(ErrorResponse.class, resultado);
        assertEquals("PEDIDO_INVALIDO", ((ErrorResponse) resultado).getErro());
    }

    @Test
    @DisplayName("Erro: Item com quantidade zero")
    void erroQuantidadeZero() {
        List<ItemCarrinho> itens = new ArrayList<>();
        itens.add(new ItemCarrinho("Produto", new BigDecimal("10"), 0, new BigDecimal("1")));

        CheckoutRequest request = new CheckoutRequest();
        request.setItens(itens);
        request.setModalidadeEntrega(ModalidadeEntrega.EXPRESSA);
        request.setFormaPagamento(FormaPagamento.PIX);
        request.setNivelClube(NivelClube.BRONZE);
        request.setRegiao(Regiao.NORTE);

        Object resultado = service.calcularResumo(request);
        assertInstanceOf(ErrorResponse.class, resultado);
        assertEquals("PEDIDO_INVALIDO", ((ErrorResponse) resultado).getErro());
    }

    @Test
    @DisplayName("Erro: Item com peso negativo")
    void erropesoNegativo() {
        List<ItemCarrinho> itens = new ArrayList<>();
        itens.add(new ItemCarrinho("Produto", new BigDecimal("10"), 1, new BigDecimal("-1")));

        CheckoutRequest request = new CheckoutRequest();
        request.setItens(itens);
        request.setModalidadeEntrega(ModalidadeEntrega.EXPRESSA);
        request.setFormaPagamento(FormaPagamento.PIX);
        request.setNivelClube(NivelClube.BRONZE);
        request.setRegiao(Regiao.NORTE);

        Object resultado = service.calcularResumo(request);
        assertInstanceOf(ErrorResponse.class, resultado);
        assertEquals("PEDIDO_INVALIDO", ((ErrorResponse) resultado).getErro());
    }

    @Test
    @DisplayName("Erro: Nível clube não informado")
    void erroNivelClube() {
        List<ItemCarrinho> itens = new ArrayList<>();
        itens.add(new ItemCarrinho("Produto", new BigDecimal("10"), 1, new BigDecimal("1")));

        CheckoutRequest request = new CheckoutRequest();
        request.setItens(itens);
        request.setModalidadeEntrega(ModalidadeEntrega.EXPRESSA);
        request.setFormaPagamento(FormaPagamento.PIX);
        request.setNivelClube(null);
        request.setRegiao(Regiao.NORTE);

        Object resultado = service.calcularResumo(request);
        assertInstanceOf(ErrorResponse.class, resultado);
        assertEquals("NIVEL_CLUBE_INVALIDO", ((ErrorResponse) resultado).getErro());
    }

    @Test
    @DisplayName("Erro: Região não informada")
    void erroRegiao() {
        List<ItemCarrinho> itens = new ArrayList<>();
        itens.add(new ItemCarrinho("Produto", new BigDecimal("10"), 1, new BigDecimal("1")));

        CheckoutRequest request = new CheckoutRequest();
        request.setItens(itens);
        request.setModalidadeEntrega(ModalidadeEntrega.EXPRESSA);
        request.setFormaPagamento(FormaPagamento.PIX);
        request.setNivelClube(NivelClube.BRONZE);
        request.setRegiao(null);

        Object resultado = service.calcularResumo(request);
        assertInstanceOf(ErrorResponse.class, resultado);
        assertEquals("REGIAO_INVALIDA", ((ErrorResponse) resultado).getErro());
    }

    @Test
    @DisplayName("Erro: Modalidade não informada")
    void erroModalidade() {
        List<ItemCarrinho> itens = new ArrayList<>();
        itens.add(new ItemCarrinho("Produto", new BigDecimal("10"), 1, new BigDecimal("1")));

        CheckoutRequest request = new CheckoutRequest();
        request.setItens(itens);
        request.setModalidadeEntrega(null);
        request.setFormaPagamento(FormaPagamento.PIX);
        request.setNivelClube(NivelClube.BRONZE);
        request.setRegiao(Regiao.NORTE);

        Object resultado = service.calcularResumo(request);
        assertInstanceOf(ErrorResponse.class, resultado);
        assertEquals("MODALIDADE_INVALIDA", ((ErrorResponse) resultado).getErro());
    }

    @Test
    @DisplayName("Erro: Motoboy acima de 5kg")
    void erroMotoboySobrecarregado() {
        List<ItemCarrinho> itens = new ArrayList<>();
        itens.add(new ItemCarrinho("Produto", new BigDecimal("10"), 1, new BigDecimal("6")));

        CheckoutRequest request = new CheckoutRequest();
        request.setItens(itens);
        request.setModalidadeEntrega(ModalidadeEntrega.MOTOBOY);
        request.setFormaPagamento(FormaPagamento.PIX);
        request.setNivelClube(NivelClube.BRONZE);
        request.setRegiao(Regiao.NORTE);

        Object resultado = service.calcularResumo(request);
        assertInstanceOf(ErrorResponse.class, resultado);
        assertEquals("MODALIDADE_INDISPONIVEL", ((ErrorResponse) resultado).getErro());
    }

    @Test
    @DisplayName("Erro: Cupom inexistente")
    void erroCupomInvalido() {
        List<ItemCarrinho> itens = new ArrayList<>();
        itens.add(new ItemCarrinho("Produto", new BigDecimal("10"), 1, new BigDecimal("1")));

        CheckoutRequest request = new CheckoutRequest();
        request.setItens(itens);
        request.setModalidadeEntrega(ModalidadeEntrega.EXPRESSA);
        request.setCupom("CUPOMINVALIDO");
        request.setFormaPagamento(FormaPagamento.PIX);
        request.setNivelClube(NivelClube.BRONZE);
        request.setRegiao(Regiao.NORTE);

        Object resultado = service.calcularResumo(request);
        assertInstanceOf(ErrorResponse.class, resultado);
        assertEquals("CUPOM_INVALIDO", ((ErrorResponse) resultado).getErro());
    }

    @Test
    @DisplayName("Erro: MENOS50 em compra abaixo de R$ 300")
    void erroCupomNaoAplicavel() {
        List<ItemCarrinho> itens = new ArrayList<>();
        itens.add(new ItemCarrinho("Produto", new BigDecimal("100"), 1, new BigDecimal("1")));

        CheckoutRequest request = new CheckoutRequest();
        request.setItens(itens);
        request.setModalidadeEntrega(ModalidadeEntrega.EXPRESSA);
        request.setCupom("MENOS50");
        request.setFormaPagamento(FormaPagamento.PIX);
        request.setNivelClube(NivelClube.BRONZE);
        request.setRegiao(Regiao.NORTE);

        Object resultado = service.calcularResumo(request);
        assertInstanceOf(ErrorResponse.class, resultado);
        assertEquals("CUPOM_NAO_APLICAVEL", ((ErrorResponse) resultado).getErro());
    }

    @Test
    @DisplayName("Erro: Forma pagamento não informada")
    void erroFormaPagamento() {
        List<ItemCarrinho> itens = new ArrayList<>();
        itens.add(new ItemCarrinho("Produto", new BigDecimal("10"), 1, new BigDecimal("1")));

        CheckoutRequest request = new CheckoutRequest();
        request.setItens(itens);
        request.setModalidadeEntrega(ModalidadeEntrega.EXPRESSA);
        request.setFormaPagamento(null);
        request.setNivelClube(NivelClube.BRONZE);
        request.setRegiao(Regiao.NORTE);

        Object resultado = service.calcularResumo(request);
        assertInstanceOf(ErrorResponse.class, resultado);
        assertEquals("FORMA_PAGAMENTO_INVALIDA", ((ErrorResponse) resultado).getErro());
    }

    @Test
    @DisplayName("Erro: PIX com múltiplas parcelas")
    void erroParcelamentoPix() {
        List<ItemCarrinho> itens = new ArrayList<>();
        itens.add(new ItemCarrinho("Produto", new BigDecimal("10"), 1, new BigDecimal("1")));

        CheckoutRequest request = new CheckoutRequest();
        request.setItens(itens);
        request.setModalidadeEntrega(ModalidadeEntrega.EXPRESSA);
        request.setFormaPagamento(FormaPagamento.PIX);
        request.setParcelas(2);
        request.setNivelClube(NivelClube.BRONZE);
        request.setRegiao(Regiao.NORTE);

        Object resultado = service.calcularResumo(request);
        assertInstanceOf(ErrorResponse.class, resultado);
        assertEquals("PARCELAMENTO_INVALIDO", ((ErrorResponse) resultado).getErro());
    }

    @Test
    @DisplayName("Erro: Cartão com mais de 12 parcelas")
    void erroParcelamentoCartao() {
        List<ItemCarrinho> itens = new ArrayList<>();
        itens.add(new ItemCarrinho("Produto", new BigDecimal("10"), 1, new BigDecimal("1")));

        CheckoutRequest request = new CheckoutRequest();
        request.setItens(itens);
        request.setModalidadeEntrega(ModalidadeEntrega.EXPRESSA);
        request.setFormaPagamento(FormaPagamento.CARTAO);
        request.setParcelas(13);
        request.setNivelClube(NivelClube.BRONZE);
        request.setRegiao(Regiao.NORTE);

        Object resultado = service.calcularResumo(request);
        assertInstanceOf(ErrorResponse.class, resultado);
        assertEquals("PARCELAMENTO_INVALIDO", ((ErrorResponse) resultado).getErro());
    }

    @Test
    @DisplayName("Erro: Boleto acima de R$ 1.000")
    void erroBoletoLimite() {
        List<ItemCarrinho> itens = new ArrayList<>();
        itens.add(new ItemCarrinho("Produto", new BigDecimal("1001"), 1, new BigDecimal("1")));

        CheckoutRequest request = new CheckoutRequest();
        request.setItens(itens);
        request.setModalidadeEntrega(ModalidadeEntrega.RETIRADA_LOJA);
        request.setFormaPagamento(FormaPagamento.BOLETO);
        request.setNivelClube(NivelClube.BRONZE);
        request.setRegiao(Regiao.NORTE);

        Object resultado = service.calcularResumo(request);
        assertInstanceOf(ErrorResponse.class, resultado);
        assertEquals("FORMA_PAGAMENTO_INDISPONIVEL", ((ErrorResponse) resultado).getErro());
    }
}

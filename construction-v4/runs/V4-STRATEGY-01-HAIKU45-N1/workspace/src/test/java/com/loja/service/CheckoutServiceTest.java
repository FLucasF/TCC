package com.loja.service;

import com.loja.dto.CheckoutRequestDto;
import com.loja.dto.CheckoutResponseDto;
import com.loja.dto.ErrorResponseDto;
import com.loja.dto.ItemDto;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

public class CheckoutServiceTest {
    private CheckoutService service;

    @BeforeEach
    public void setUp() {
        service = new CheckoutService();
    }

    private CheckoutRequestDto criarRequisicao(
            List<ItemDto> itens, String entrega, String cupom,
            String pagamento, Integer parcelas, String nivel, String regiao) {
        CheckoutRequestDto req = new CheckoutRequestDto();
        req.itens = itens;
        req.modalidadeEntrega = entrega;
        req.cupom = cupom;
        req.formaPagamento = pagamento;
        req.parcelas = parcelas;
        req.nivelClube = nivel;
        req.regiao = regiao;
        return req;
    }

    private ItemDto criarItem(String nome, BigDecimal preco, Integer qtd, BigDecimal peso) {
        ItemDto item = new ItemDto();
        item.nome = nome;
        item.precoUnitario = preco;
        item.quantidade = qtd;
        item.pesoKg = peso;
        return item;
    }

    @Test
    public void exemplo1() {
        List<ItemDto> itens = new ArrayList<>();
        itens.add(criarItem("Camiseta", new BigDecimal("79.90"), 2, new BigDecimal("0.30")));
        itens.add(criarItem("Tênis", new BigDecimal("249.90"), 1, new BigDecimal("1.20")));

        Object resultado = service.calcularResumo(
                criarRequisicao(itens, "EXPRESSA", "BEMVINDO10", "PIX", null, "BRONZE", "NORTE")
        );

        CheckoutResponseDto resp = (CheckoutResponseDto) resultado;

        assertEquals(new BigDecimal("409.70"), resp.subtotalProdutos);
        assertEquals(new BigDecimal("40.97"), resp.descontoCupom);
        assertEquals(new BigDecimal("33.10"), resp.frete);
        assertEquals(2, resp.prazoEntregaDias);
        assertEquals(new BigDecimal("10.24"), resp.seguro);
        assertEquals(new BigDecimal("-20.60"), resp.ajustePagamento);
        assertEquals(new BigDecimal("391.47"), resp.totalFinal);
        assertEquals(1, resp.parcelas);
        assertEquals(new BigDecimal("391.47"), resp.valorParcela);
        assertEquals(new BigDecimal("0.00"), resp.creditoProximaCompra);
        assertFalse(resp.brinde);
    }

    @Test
    public void exemplo2() {
        List<ItemDto> itens = new ArrayList<>();
        itens.add(criarItem("Camiseta", new BigDecimal("79.90"), 2, new BigDecimal("0.30")));
        itens.add(criarItem("Tênis", new BigDecimal("249.90"), 1, new BigDecimal("1.20")));

        Object resultado = service.calcularResumo(
                criarRequisicao(itens, "ECONOMICA", null, "CARTAO", 6, "PRATA", "CENTRO_OESTE")
        );

        CheckoutResponseDto resp = (CheckoutResponseDto) resultado;

        assertEquals(new BigDecimal("409.70"), resp.subtotalProdutos);
        assertEquals(new BigDecimal("0.00"), resp.descontoCupom);
        assertEquals(new BigDecimal("15.60"), resp.frete);
        assertEquals(7, resp.prazoEntregaDias);
        assertEquals(new BigDecimal("6.15"), resp.seguro);
        assertEquals(new BigDecimal("30.55"), resp.ajustePagamento);
        assertEquals(new BigDecimal("462.00"), resp.totalFinal);
        assertEquals(6, resp.parcelas);
        assertEquals(new BigDecimal("77.00"), resp.valorParcela);
        assertEquals(new BigDecimal("8.19"), resp.creditoProximaCompra);
        assertFalse(resp.brinde);
    }

    @Test
    public void exemplo3() {
        List<ItemDto> itens = new ArrayList<>();
        itens.add(criarItem("Fone", new BigDecimal("199.90"), 2, new BigDecimal("0.25")));

        Object resultado = service.calcularResumo(
                criarRequisicao(itens, "MOTOBOY", "MENOS50", "BOLETO", null, "BRONZE", "NORDESTE")
        );

        CheckoutResponseDto resp = (CheckoutResponseDto) resultado;

        assertEquals(new BigDecimal("399.80"), resp.subtotalProdutos);
        assertEquals(new BigDecimal("50.00"), resp.descontoCupom);
        assertEquals(new BigDecimal("18.00"), resp.frete);
        assertEquals(0, resp.prazoEntregaDias);
        assertEquals(new BigDecimal("8.00"), resp.seguro);
        assertEquals(new BigDecimal("3.49"), resp.ajustePagamento);
        assertEquals(new BigDecimal("379.29"), resp.totalFinal);
        assertEquals(1, resp.parcelas);
        assertEquals(new BigDecimal("379.29"), resp.valorParcela);
        assertEquals(new BigDecimal("0.00"), resp.creditoProximaCompra);
        assertFalse(resp.brinde);
    }

    @Test
    public void exemplo4() {
        List<ItemDto> itens = new ArrayList<>();
        itens.add(criarItem("Meia", new BigDecimal("19.90"), 7, new BigDecimal("0.10")));
        itens.add(criarItem("Camiseta", new BigDecimal("79.90"), 2, new BigDecimal("0.30")));

        Object resultado = service.calcularResumo(
                criarRequisicao(itens, "RETIRADA_LOJA", "LEVE3PAGUE2", "CARTAO", 3, "PRATA", "SUL")
        );

        CheckoutResponseDto resp = (CheckoutResponseDto) resultado;

        assertEquals(new BigDecimal("299.10"), resp.subtotalProdutos);
        assertEquals(new BigDecimal("39.80"), resp.descontoCupom);
        assertEquals(new BigDecimal("0.00"), resp.frete);
        assertEquals(1, resp.prazoEntregaDias);
        assertEquals(new BigDecimal("2.99"), resp.seguro);
        assertEquals(new BigDecimal("0.00"), resp.ajustePagamento);
        assertEquals(new BigDecimal("262.29"), resp.totalFinal);
        assertEquals(3, resp.parcelas);
        assertEquals(new BigDecimal("87.43"), resp.valorParcela);
        assertEquals(new BigDecimal("5.98"), resp.creditoProximaCompra);
        assertFalse(resp.brinde);
    }

    @Test
    public void exemplo5() {
        List<ItemDto> itens = new ArrayList<>();
        itens.add(criarItem("Camiseta", new BigDecimal("79.90"), 2, new BigDecimal("0.30")));
        itens.add(criarItem("Tênis", new BigDecimal("249.90"), 1, new BigDecimal("1.20")));

        Object resultado = service.calcularResumo(
                criarRequisicao(itens, "EXPRESSA", null, "PIX", null, "OURO", "SUDESTE")
        );

        CheckoutResponseDto resp = (CheckoutResponseDto) resultado;

        assertEquals(new BigDecimal("409.70"), resp.subtotalProdutos);
        assertEquals(new BigDecimal("0.00"), resp.descontoCupom);
        assertEquals(new BigDecimal("0.00"), resp.frete);
        assertEquals(2, resp.prazoEntregaDias);
        assertEquals(new BigDecimal("4.10"), resp.seguro);
        assertEquals(new BigDecimal("-20.69"), resp.ajustePagamento);
        assertEquals(new BigDecimal("393.11"), resp.totalFinal);
        assertEquals(1, resp.parcelas);
        assertEquals(new BigDecimal("393.11"), resp.valorParcela);
        assertEquals(new BigDecimal("20.48"), resp.creditoProximaCompra);
        assertFalse(resp.brinde);
    }

    @Test
    public void testarCarrinhoVazio() {
        CheckoutRequestDto req = new CheckoutRequestDto();
        req.itens = new ArrayList<>();
        req.modalidadeEntrega = "EXPRESSA";
        req.formaPagamento = "PIX";
        req.nivelClube = "BRONZE";
        req.regiao = "SUDESTE";

        Object resultado = service.calcularResumo(req);
        ErrorResponseDto erro = (ErrorResponseDto) resultado;
        assertEquals("PEDIDO_INVALIDO", erro.erro);
    }

    @Test
    public void testarNivelClubeInvalido() {
        List<ItemDto> itens = new ArrayList<>();
        itens.add(criarItem("Item", new BigDecimal("10.00"), 1, new BigDecimal("0.10")));

        CheckoutRequestDto req = new CheckoutRequestDto();
        req.itens = itens;
        req.modalidadeEntrega = "EXPRESSA";
        req.formaPagamento = "PIX";
        req.nivelClube = "INVALIDO";
        req.regiao = "SUDESTE";

        Object resultado = service.calcularResumo(req);
        ErrorResponseDto erro = (ErrorResponseDto) resultado;
        assertEquals("NIVEL_CLUBE_INVALIDO", erro.erro);
    }

    @Test
    public void testarMotoboySobreWeight() {
        List<ItemDto> itens = new ArrayList<>();
        itens.add(criarItem("Item", new BigDecimal("10.00"), 1, new BigDecimal("6.00")));

        Object resultado = service.calcularResumo(
                criarRequisicao(itens, "MOTOBOY", null, "PIX", null, "BRONZE", "SUDESTE")
        );

        ErrorResponseDto erro = (ErrorResponseDto) resultado;
        assertEquals("MODALIDADE_INDISPONIVEL", erro.erro);
    }

    @Test
    public void testarBoletoAcimaDeLimite() {
        List<ItemDto> itens = new ArrayList<>();
        itens.add(criarItem("Item", new BigDecimal("2000.00"), 1, new BigDecimal("1.00")));

        Object resultado = service.calcularResumo(
                criarRequisicao(itens, "RETIRADA_LOJA", null, "BOLETO", null, "BRONZE", "SUDESTE")
        );

        ErrorResponseDto erro = (ErrorResponseDto) resultado;
        assertEquals("FORMA_PAGAMENTO_INDISPONIVEL", erro.erro);
    }

    @Test
    public void testarParcelamentoPixInvalido() {
        List<ItemDto> itens = new ArrayList<>();
        itens.add(criarItem("Item", new BigDecimal("10.00"), 1, new BigDecimal("0.10")));

        CheckoutRequestDto req = new CheckoutRequestDto();
        req.itens = itens;
        req.modalidadeEntrega = "EXPRESSA";
        req.formaPagamento = "PIX";
        req.parcelas = 2;
        req.nivelClube = "BRONZE";
        req.regiao = "SUDESTE";

        Object resultado = service.calcularResumo(req);
        ErrorResponseDto erro = (ErrorResponseDto) resultado;
        assertEquals("PARCELAMENTO_INVALIDO", erro.erro);
    }

    @Test
    public void testarCupomInvalido() {
        List<ItemDto> itens = new ArrayList<>();
        itens.add(criarItem("Item", new BigDecimal("10.00"), 1, new BigDecimal("0.10")));

        Object resultado = service.calcularResumo(
                criarRequisicao(itens, "EXPRESSA", "CUPOMINVALIDO", "PIX", null, "BRONZE", "SUDESTE")
        );

        ErrorResponseDto erro = (ErrorResponseDto) resultado;
        assertEquals("CUPOM_INVALIDO", erro.erro);
    }

    @Test
    public void testarCupomNaoAplicavel() {
        List<ItemDto> itens = new ArrayList<>();
        itens.add(criarItem("Item", new BigDecimal("100.00"), 1, new BigDecimal("0.10")));

        Object resultado = service.calcularResumo(
                criarRequisicao(itens, "EXPRESSA", "MENOS50", "PIX", null, "BRONZE", "SUDESTE")
        );

        ErrorResponseDto erro = (ErrorResponseDto) resultado;
        assertEquals("CUPOM_NAO_APLICAVEL", erro.erro);
    }

    @Test
    public void testarFreteGratis() {
        List<ItemDto> itens = new ArrayList<>();
        itens.add(criarItem("Item", new BigDecimal("100.00"), 1, new BigDecimal("1.00")));

        Object resultado = service.calcularResumo(
                criarRequisicao(itens, "EXPRESSA", "FRETEGRATIS", "PIX", null, "BRONZE", "SUDESTE")
        );

        CheckoutResponseDto resp = (CheckoutResponseDto) resultado;
        assertEquals(new BigDecimal("0.00"), resp.frete);
    }

    @Test
    public void testarOuroBrinde() {
        List<ItemDto> itens = new ArrayList<>();
        itens.add(criarItem("Item", new BigDecimal("600.00"), 1, new BigDecimal("1.00")));

        Object resultado = service.calcularResumo(
                criarRequisicao(itens, "EXPRESSA", null, "PIX", null, "OURO", "SUDESTE")
        );

        CheckoutResponseDto resp = (CheckoutResponseDto) resultado;
        assertTrue(resp.brinde);
    }

    @Test
    public void testarOuroNaoPagaFrete() {
        List<ItemDto> itens = new ArrayList<>();
        itens.add(criarItem("Item", new BigDecimal("100.00"), 1, new BigDecimal("2.00")));

        Object resultado = service.calcularResumo(
                criarRequisicao(itens, "EXPRESSA", null, "PIX", null, "OURO", "SUDESTE")
        );

        CheckoutResponseDto resp = (CheckoutResponseDto) resultado;
        assertEquals(new BigDecimal("0.00"), resp.frete);
    }
}

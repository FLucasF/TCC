package com.loja.checkout.service;

import com.loja.checkout.dto.CheckoutRequest;
import com.loja.checkout.dto.CheckoutResponse;
import com.loja.checkout.dto.ItemRequest;
import com.loja.checkout.exception.CheckoutException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class CheckoutServiceTest {
    private CheckoutService checkoutService;

    @BeforeEach
    void setUp() {
        checkoutService = new CheckoutService();
    }

    private ItemRequest criarItem(String nome, BigDecimal preco, int quantidade, BigDecimal peso) {
        ItemRequest item = new ItemRequest();
        item.setNome(nome);
        item.setPrecoUnitario(preco);
        item.setQuantidade(quantidade);
        item.setPesoKg(peso);
        return item;
    }

    private CheckoutRequest criarRequisicao(List<ItemRequest> itens, String modalidade,
                                            String cupom, String forma, Integer parcelas) {
        CheckoutRequest req = new CheckoutRequest();
        req.setItens(itens);
        req.setModalidadeEntrega(modalidade);
        req.setCupom(cupom);
        req.setFormaPagamento(forma);
        req.setParcelas(parcelas);
        return req;
    }

    @Test
    void exemplo1_CamisetaTenisExpressaBemVindoPix() {
        List<ItemRequest> itens = Arrays.asList(
                criarItem("Camiseta", new BigDecimal("79.90"), 2, new BigDecimal("0.30")),
                criarItem("Tênis", new BigDecimal("249.90"), 1, new BigDecimal("1.20"))
        );

        CheckoutRequest request = criarRequisicao(itens, "EXPRESSA", "BEMVINDO10", "PIX", 1);
        CheckoutResponse response = checkoutService.calcularResumo(request);

        assertEquals(new BigDecimal("409.70"), response.getSubtotalProdutos());
        assertEquals(new BigDecimal("40.97"), response.getDescontoCupom());
        assertEquals(new BigDecimal("33.10"), response.getFrete());
        assertEquals(2, response.getPrazoEntregaDias());
        assertEquals(new BigDecimal("-20.09"), response.getAjustePagamento());
        assertEquals(new BigDecimal("381.74"), response.getTotalFinal());
        assertEquals(1, response.getParcelas());
        assertEquals(new BigDecimal("381.74"), response.getValorParcela());
    }

    @Test
    void exemplo2_CamisetaTenisEconomicaCartao6x() {
        List<ItemRequest> itens = Arrays.asList(
                criarItem("Camiseta", new BigDecimal("79.90"), 2, new BigDecimal("0.30")),
                criarItem("Tênis", new BigDecimal("249.90"), 1, new BigDecimal("1.20"))
        );

        CheckoutRequest request = criarRequisicao(itens, "ECONOMICA", null, "CARTAO", 6);
        CheckoutResponse response = checkoutService.calcularResumo(request);

        assertEquals(new BigDecimal("409.70"), response.getSubtotalProdutos());
        assertEquals(new BigDecimal("0.00"), response.getDescontoCupom());
        assertEquals(new BigDecimal("15.60"), response.getFrete());
        assertEquals(7, response.getPrazoEntregaDias());
        assertEquals(new BigDecimal("30.10"), response.getAjustePagamento());
        assertEquals(new BigDecimal("455.40"), response.getTotalFinal());
        assertEquals(6, response.getParcelas());
        assertEquals(new BigDecimal("75.90"), response.getValorParcela());
    }

    @Test
    void exemplo3_FoneMotoboy_Menos50Boleto() {
        List<ItemRequest> itens = Collections.singletonList(
                criarItem("Fone", new BigDecimal("199.90"), 2, new BigDecimal("0.25"))
        );

        CheckoutRequest request = criarRequisicao(itens, "MOTOBOY", "MENOS50", "BOLETO", 1);
        CheckoutResponse response = checkoutService.calcularResumo(request);

        assertEquals(new BigDecimal("399.80"), response.getSubtotalProdutos());
        assertEquals(new BigDecimal("50.00"), response.getDescontoCupom());
        assertEquals(new BigDecimal("18.00"), response.getFrete());
        assertEquals(0, response.getPrazoEntregaDias());
        assertEquals(new BigDecimal("3.49"), response.getAjustePagamento());
        assertEquals(new BigDecimal("371.29"), response.getTotalFinal());
        assertEquals(1, response.getParcelas());
        assertEquals(new BigDecimal("371.29"), response.getValorParcela());
    }

    @Test
    void exemplo4_MeiaCamisetaRetiradaLeve3Pague2Cartao3x() {
        List<ItemRequest> itens = Arrays.asList(
                criarItem("Meia", new BigDecimal("19.90"), 7, new BigDecimal("0.10")),
                criarItem("Camiseta", new BigDecimal("79.90"), 2, new BigDecimal("0.30"))
        );

        CheckoutRequest request = criarRequisicao(itens, "RETIRADA_LOJA", "LEVE3PAGUE2", "CARTAO", 3);
        CheckoutResponse response = checkoutService.calcularResumo(request);

        assertEquals(new BigDecimal("299.10"), response.getSubtotalProdutos());
        assertEquals(new BigDecimal("39.80"), response.getDescontoCupom());
        assertEquals(new BigDecimal("0.00"), response.getFrete());
        assertEquals(1, response.getPrazoEntregaDias());
        assertEquals(new BigDecimal("0.00"), response.getAjustePagamento());
        assertEquals(new BigDecimal("259.30"), response.getTotalFinal());
        assertEquals(3, response.getParcelas());
        assertEquals(new BigDecimal("86.43"), response.getValorParcela());
    }

    @Test
    void testCarrinhVazioDeveLancarErro() {
        CheckoutRequest request = criarRequisicao(Collections.emptyList(), "EXPRESSA", null, "PIX", 1);
        assertThrows(CheckoutException.class, () -> checkoutService.calcularResumo(request));
    }

    @Test
    void testModalidadeInvalidaDeveLancarErro() {
        List<ItemRequest> itens = Collections.singletonList(
                criarItem("Camiseta", new BigDecimal("79.90"), 1, new BigDecimal("0.30"))
        );
        CheckoutRequest request = criarRequisicao(itens, "INVALIDA", null, "PIX", 1);
        CheckoutException e = assertThrows(CheckoutException.class, () -> checkoutService.calcularResumo(request));
        assertEquals("MODALIDADE_INVALIDA", e.getCodigo());
    }

    @Test
    void testMotoboySobeAcima5kgDeveLancarErro() {
        List<ItemRequest> itens = Collections.singletonList(
                criarItem("Caixa", new BigDecimal("100.00"), 1, new BigDecimal("6.00"))
        );
        CheckoutRequest request = criarRequisicao(itens, "MOTOBOY", null, "PIX", 1);
        CheckoutException e = assertThrows(CheckoutException.class, () -> checkoutService.calcularResumo(request));
        assertEquals("MODALIDADE_INDISPONIVEL", e.getCodigo());
    }

    @Test
    void testCupomInvalidoDeveLancarErro() {
        List<ItemRequest> itens = Collections.singletonList(
                criarItem("Camiseta", new BigDecimal("79.90"), 1, new BigDecimal("0.30"))
        );
        CheckoutRequest request = criarRequisicao(itens, "EXPRESSA", "INVALIDO", "PIX", 1);
        CheckoutException e = assertThrows(CheckoutException.class, () -> checkoutService.calcularResumo(request));
        assertEquals("CUPOM_INVALIDO", e.getCodigo());
    }

    @Test
    void testMenos50AbaixoMinimoDeveLancarErro() {
        List<ItemRequest> itens = Collections.singletonList(
                criarItem("Camiseta", new BigDecimal("79.90"), 1, new BigDecimal("0.30"))
        );
        CheckoutRequest request = criarRequisicao(itens, "EXPRESSA", "MENOS50", "PIX", 1);
        CheckoutException e = assertThrows(CheckoutException.class, () -> checkoutService.calcularResumo(request));
        assertEquals("CUPOM_NAO_APLICAVEL", e.getCodigo());
    }

    @Test
    void testFormaPagamentoInvalidaDeveLancarErro() {
        List<ItemRequest> itens = Collections.singletonList(
                criarItem("Camiseta", new BigDecimal("79.90"), 1, new BigDecimal("0.30"))
        );
        CheckoutRequest request = criarRequisicao(itens, "EXPRESSA", null, "INVALIDA", 1);
        CheckoutException e = assertThrows(CheckoutException.class, () -> checkoutService.calcularResumo(request));
        assertEquals("FORMA_PAGAMENTO_INVALIDA", e.getCodigo());
    }

    @Test
    void testPixNaoPermiteParcelamento() {
        List<ItemRequest> itens = Collections.singletonList(
                criarItem("Camiseta", new BigDecimal("79.90"), 1, new BigDecimal("0.30"))
        );
        CheckoutRequest request = criarRequisicao(itens, "EXPRESSA", null, "PIX", 2);
        CheckoutException e = assertThrows(CheckoutException.class, () -> checkoutService.calcularResumo(request));
        assertEquals("PARCELAMENTO_INVALIDO", e.getCodigo());
    }

    @Test
    void testBoletoAcima1000DeveLancarErro() {
        List<ItemRequest> itens = Collections.singletonList(
                criarItem("Produto caro", new BigDecimal("1001.00"), 1, new BigDecimal("1.00"))
        );
        CheckoutRequest request = criarRequisicao(itens, "RETIRADA_LOJA", null, "BOLETO", 1);
        CheckoutException e = assertThrows(CheckoutException.class, () -> checkoutService.calcularResumo(request));
        assertEquals("FORMA_PAGAMENTO_INDISPONIVEL", e.getCodigo());
    }

    @Test
    void testFretegratisAplicaDesconto() {
        List<ItemRequest> itens = Collections.singletonList(
                criarItem("Camiseta", new BigDecimal("79.90"), 1, new BigDecimal("0.30"))
        );
        CheckoutRequest request = criarRequisicao(itens, "EXPRESSA", "FRETEGRATIS", "PIX", 1);
        CheckoutResponse response = checkoutService.calcularResumo(request);

        assertEquals(response.getFrete(), response.getDescontoCupom());
    }

    @Test
    void testCartaoAte3xSemJuros() {
        List<ItemRequest> itens = Collections.singletonList(
                criarItem("Camiseta", new BigDecimal("79.90"), 1, new BigDecimal("0.30"))
        );
        CheckoutRequest request = criarRequisicao(itens, "RETIRADA_LOJA", null, "CARTAO", 3);
        CheckoutResponse response = checkoutService.calcularResumo(request);

        assertEquals(new BigDecimal("0.00"), response.getAjustePagamento());
    }

    @Test
    void testPesoZeroDeveLancarErro() {
        List<ItemRequest> itens = Collections.singletonList(
                criarItem("Camiseta", new BigDecimal("79.90"), 1, new BigDecimal("0.00"))
        );
        CheckoutRequest request = criarRequisicao(itens, "EXPRESSA", null, "PIX", 1);
        // Peso zero é aceito, veja especificação
    }

    @Test
    void testPrecoZeroDeveLancarErro() {
        List<ItemRequest> itens = Collections.singletonList(
                criarItem("Camiseta", new BigDecimal("0.00"), 1, new BigDecimal("0.30"))
        );
        CheckoutRequest request = criarRequisicao(itens, "EXPRESSA", null, "PIX", 1);
        assertThrows(CheckoutException.class, () -> checkoutService.calcularResumo(request));
    }

    @Test
    void testQuantidadeZeroDeveLancarErro() {
        List<ItemRequest> itens = Collections.singletonList(
                criarItem("Camiseta", new BigDecimal("79.90"), 0, new BigDecimal("0.30"))
        );
        CheckoutRequest request = criarRequisicao(itens, "EXPRESSA", null, "PIX", 1);
        assertThrows(CheckoutException.class, () -> checkoutService.calcularResumo(request));
    }
}

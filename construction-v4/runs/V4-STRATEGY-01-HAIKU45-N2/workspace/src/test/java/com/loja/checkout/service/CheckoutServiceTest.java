package com.loja.checkout.service;

import com.loja.checkout.dto.CheckoutRequest;
import com.loja.checkout.dto.CheckoutResponse;
import com.loja.checkout.dto.Item;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class CheckoutServiceTest {
    private final CheckoutService service = new CheckoutService();

    @Test
    void exemplo1() {
        CheckoutRequest request = criarRequisicao(
                Arrays.asList(
                        new Item("Camiseta", 79.90, 2, 0.30),
                        new Item("Tênis", 249.90, 1, 1.20)
                ),
                "EXPRESSA", "BEMVINDO10", "PIX", 1, "BRONZE", "NORTE"
        );

        CheckoutResponse response = service.calcularResumo(request);

        assertEquals(409.70, response.getSubtotalProdutos());
        assertEquals(40.97, response.getDescontoCupom());
        assertEquals(33.10, response.getFrete());
        assertEquals(2, response.getPrazoEntregaDias());
        assertEquals(10.24, response.getSeguro());
        assertEquals(-20.60, response.getAjustePagamento());
        assertEquals(391.47, response.getTotalFinal());
        assertEquals(1, response.getParcelas());
        assertEquals(391.47, response.getValorParcela());
        assertEquals(0.00, response.getCreditoProximaCompra());
        assertFalse(response.getBrinde());
    }

    @Test
    void exemplo2() {
        CheckoutRequest request = criarRequisicao(
                Arrays.asList(
                        new Item("Camiseta", 79.90, 2, 0.30),
                        new Item("Tênis", 249.90, 1, 1.20)
                ),
                "ECONOMICA", null, "CARTAO", 6, "PRATA", "CENTRO_OESTE"
        );

        CheckoutResponse response = service.calcularResumo(request);

        assertEquals(409.70, response.getSubtotalProdutos());
        assertEquals(0.00, response.getDescontoCupom());
        assertEquals(15.60, response.getFrete());
        assertEquals(7, response.getPrazoEntregaDias());
        assertEquals(6.15, response.getSeguro());
        assertEquals(30.55, response.getAjustePagamento());
        assertEquals(462.00, response.getTotalFinal());
        assertEquals(6, response.getParcelas());
        assertEquals(77.00, response.getValorParcela());
        assertEquals(8.19, response.getCreditoProximaCompra());
        assertFalse(response.getBrinde());
    }

    @Test
    void exemplo3() {
        CheckoutRequest request = criarRequisicao(
                Arrays.asList(
                        new Item("Fone", 199.90, 2, 0.25)
                ),
                "MOTOBOY", "MENOS50", "BOLETO", 1, "BRONZE", "NORDESTE"
        );

        CheckoutResponse response = service.calcularResumo(request);

        assertEquals(399.80, response.getSubtotalProdutos());
        assertEquals(50.00, response.getDescontoCupom());
        assertEquals(18.00, response.getFrete());
        assertEquals(0, response.getPrazoEntregaDias());
        assertEquals(8.00, response.getSeguro());
        assertEquals(3.49, response.getAjustePagamento());
        assertEquals(379.29, response.getTotalFinal());
        assertEquals(1, response.getParcelas());
        assertEquals(379.29, response.getValorParcela());
        assertEquals(0.00, response.getCreditoProximaCompra());
        assertFalse(response.getBrinde());
    }

    @Test
    void exemplo4() {
        CheckoutRequest request = criarRequisicao(
                Arrays.asList(
                        new Item("Meia", 19.90, 7, 0.10),
                        new Item("Camiseta", 79.90, 2, 0.30)
                ),
                "RETIRADA_LOJA", "LEVE3PAGUE2", "CARTAO", 3, "PRATA", "SUL"
        );

        CheckoutResponse response = service.calcularResumo(request);

        assertEquals(299.10, response.getSubtotalProdutos());
        assertEquals(39.80, response.getDescontoCupom());
        assertEquals(0.00, response.getFrete());
        assertEquals(1, response.getPrazoEntregaDias());
        assertEquals(2.99, response.getSeguro());
        assertEquals(0.00, response.getAjustePagamento());
        assertEquals(262.29, response.getTotalFinal());
        assertEquals(3, response.getParcelas());
        assertEquals(87.43, response.getValorParcela());
        assertEquals(5.98, response.getCreditoProximaCompra());
        assertFalse(response.getBrinde());
    }

    @Test
    void exemplo5() {
        CheckoutRequest request = criarRequisicao(
                Arrays.asList(
                        new Item("Camiseta", 79.90, 2, 0.30),
                        new Item("Tênis", 249.90, 1, 1.20)
                ),
                "EXPRESSA", null, "PIX", 1, "OURO", "SUDESTE"
        );

        CheckoutResponse response = service.calcularResumo(request);

        assertEquals(409.70, response.getSubtotalProdutos());
        assertEquals(0.00, response.getDescontoCupom());
        assertEquals(0.00, response.getFrete());
        assertEquals(2, response.getPrazoEntregaDias());
        assertEquals(4.10, response.getSeguro());
        assertEquals(-20.69, response.getAjustePagamento());
        assertEquals(393.11, response.getTotalFinal());
        assertEquals(1, response.getParcelas());
        assertEquals(393.11, response.getValorParcela());
        assertEquals(20.48, response.getCreditoProximaCompra());
        assertFalse(response.getBrinde());
    }

    @Test
    void testMotoboyIndisponivel() {
        CheckoutRequest request = criarRequisicao(
                Arrays.asList(
                        new Item("Produto pesado", 100.00, 10, 1.00)
                ),
                "MOTOBOY", null, "PIX", 1, "BRONZE", "SUDESTE"
        );

        CheckoutResponse response = service.calcularResumo(request);

        assertEquals("MODALIDADE_INDISPONIVEL", response.getErro());
    }

    @Test
    void testCupomNaoAplicavel() {
        CheckoutRequest request = criarRequisicao(
                Arrays.asList(
                        new Item("Produto barato", 50.00, 1, 0.10)
                ),
                "RETIRADA_LOJA", "MENOS50", "PIX", 1, "BRONZE", "SUDESTE"
        );

        CheckoutResponse response = service.calcularResumo(request);

        assertEquals("CUPOM_NAO_APLICAVEL", response.getErro());
    }

    @Test
    void testBoletoIndisponivel() {
        CheckoutRequest request = criarRequisicao(
                Arrays.asList(
                        new Item("Produto caro", 500.00, 3, 0.50)
                ),
                "RETIRADA_LOJA", null, "BOLETO", 1, "BRONZE", "SUDESTE"
        );

        CheckoutResponse response = service.calcularResumo(request);

        assertEquals("FORMA_PAGAMENTO_INDISPONIVEL", response.getErro());
    }

    @Test
    void testParcelamentoInvalido() {
        CheckoutRequest request = criarRequisicao(
                Arrays.asList(
                        new Item("Produto", 100.00, 1, 0.10)
                ),
                "RETIRADA_LOJA", null, "PIX", 2, "BRONZE", "SUDESTE"
        );

        CheckoutResponse response = service.calcularResumo(request);

        assertEquals("PARCELAMENTO_INVALIDO", response.getErro());
    }

    @Test
    void testPedidoInvalido() {
        CheckoutRequest request = criarRequisicao(
                Arrays.asList(
                        new Item("Produto", 0.00, 1, 0.10)
                ),
                "RETIRADA_LOJA", null, "PIX", 1, "BRONZE", "SUDESTE"
        );

        CheckoutResponse response = service.calcularResumo(request);

        assertEquals("PEDIDO_INVALIDO", response.getErro());
    }

    private CheckoutRequest criarRequisicao(List<Item> itens, String modalidade, String cupom,
                                             String formaPagamento, int parcelas, String nivel, String regiao) {
        CheckoutRequest request = new CheckoutRequest();
        request.setItens(itens);
        request.setModalidadeEntrega(modalidade);
        request.setCupom(cupom);
        request.setFormaPagamento(formaPagamento);
        request.setParcelas(parcelas);
        request.setNivelClube(nivel);
        request.setRegiao(regiao);
        return request;
    }
}

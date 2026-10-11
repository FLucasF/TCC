package com.loja.checkout.service;

import com.loja.checkout.dto.CheckoutRequest;
import com.loja.checkout.dto.CheckoutResponse;
import com.loja.checkout.dto.ItemRequest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

public class CheckoutServiceTest {
    private CheckoutService service;

    @BeforeEach
    public void setUp() {
        service = new CheckoutService();
    }

    @Test
    public void exemplo1_CamisetaTenisExpressaBemVindo10PixBronzeNorte() {
        List<ItemRequest> itens = Arrays.asList(
                new ItemRequest("Camiseta", 79.90, 2, 0.30),
                new ItemRequest("Tênis", 249.90, 1, 1.20)
        );

        CheckoutRequest request = new CheckoutRequest(
                itens,
                "EXPRESSA",
                "BEMVINDO10",
                "PIX",
                null,
                "BRONZE",
                "NORTE"
        );

        CheckoutResponse response = service.calcular(request);

        assertEquals(new BigDecimal("409.70"), response.subtotalProdutos());
        assertEquals(new BigDecimal("40.97"), response.descontoCupom());
        assertEquals(new BigDecimal("33.10"), response.frete());
        assertEquals(2, response.prazoEntregaDias());
        assertEquals(new BigDecimal("10.24"), response.seguro());
        assertEquals(new BigDecimal("-20.60"), response.ajustePagamento());
        assertEquals(new BigDecimal("391.47"), response.totalFinal());
        assertEquals(1, response.parcelas());
        assertEquals(new BigDecimal("391.47"), response.valorParcela());
        assertEquals(new BigDecimal("0.00"), response.creditoProximaCompra());
        assertFalse(response.brinde());
    }

    @Test
    public void exemplo2_CamisetaTenisEconomicaSemCupomCartao6xPratacentroOeste() {
        List<ItemRequest> itens = Arrays.asList(
                new ItemRequest("Camiseta", 79.90, 2, 0.30),
                new ItemRequest("Tênis", 249.90, 1, 1.20)
        );

        CheckoutRequest request = new CheckoutRequest(
                itens,
                "ECONOMICA",
                null,
                "CARTAO",
                6,
                "PRATA",
                "CENTRO_OESTE"
        );

        CheckoutResponse response = service.calcular(request);

        assertEquals(new BigDecimal("409.70"), response.subtotalProdutos());
        assertEquals(BigDecimal.ZERO.setScale(2, java.math.RoundingMode.HALF_EVEN), response.descontoCupom());
        assertEquals(new BigDecimal("15.60"), response.frete());
        assertEquals(7, response.prazoEntregaDias());
        assertEquals(new BigDecimal("6.15"), response.seguro());
        assertEquals(new BigDecimal("30.55"), response.ajustePagamento());
        assertEquals(new BigDecimal("462.00"), response.totalFinal());
        assertEquals(6, response.parcelas());
        assertEquals(new BigDecimal("77.00"), response.valorParcela());
        assertEquals(new BigDecimal("8.19"), response.creditoProximaCompra());
        assertFalse(response.brinde());
    }

    @Test
    public void exemplo3_FoneMenos50MotoboyBoletoBronzeNordeste() {
        List<ItemRequest> itens = Arrays.asList(
                new ItemRequest("Fone", 199.90, 2, 0.25)
        );

        CheckoutRequest request = new CheckoutRequest(
                itens,
                "MOTOBOY",
                "MENOS50",
                "BOLETO",
                null,
                "BRONZE",
                "NORDESTE"
        );

        CheckoutResponse response = service.calcular(request);

        assertEquals(new BigDecimal("399.80"), response.subtotalProdutos());
        assertEquals(new BigDecimal("50.00"), response.descontoCupom());
        assertEquals(new BigDecimal("18.00"), response.frete());
        assertEquals(0, response.prazoEntregaDias());
        assertEquals(new BigDecimal("8.00"), response.seguro());
        assertEquals(new BigDecimal("3.49"), response.ajustePagamento());
        assertEquals(new BigDecimal("379.29"), response.totalFinal());
        assertEquals(1, response.parcelas());
        assertEquals(new BigDecimal("379.29"), response.valorParcela());
        assertEquals(new BigDecimal("0.00"), response.creditoProximaCompra());
        assertFalse(response.brinde());
    }

    @Test
    public void exemplo4_MeiasCamisetasLeve3Pague2RetiraLojaCartao3xPrataSul() {
        List<ItemRequest> itens = Arrays.asList(
                new ItemRequest("Meia", 19.90, 7, 0.10),
                new ItemRequest("Camiseta", 79.90, 2, 0.30)
        );

        CheckoutRequest request = new CheckoutRequest(
                itens,
                "RETIRADA_LOJA",
                "LEVE3PAGUE2",
                "CARTAO",
                3,
                "PRATA",
                "SUL"
        );

        CheckoutResponse response = service.calcular(request);

        assertEquals(new BigDecimal("299.10"), response.subtotalProdutos());
        assertEquals(new BigDecimal("39.80"), response.descontoCupom());
        assertEquals(new BigDecimal("0.00"), response.frete()); // 0.00 já tem scale 2
        assertEquals(1, response.prazoEntregaDias());
        assertEquals(new BigDecimal("2.99"), response.seguro());
        assertEquals(new BigDecimal("0.00"), response.ajustePagamento());
        assertEquals(new BigDecimal("262.29"), response.totalFinal());
        assertEquals(3, response.parcelas());
        assertEquals(new BigDecimal("87.43"), response.valorParcela());
        assertEquals(new BigDecimal("5.98"), response.creditoProximaCompra());
        assertFalse(response.brinde());
    }

    @Test
    public void exemplo5_CamisetaTenisExpressaSemCupomPixOuroSudeste() {
        List<ItemRequest> itens = Arrays.asList(
                new ItemRequest("Camiseta", 79.90, 2, 0.30),
                new ItemRequest("Tênis", 249.90, 1, 1.20)
        );

        CheckoutRequest request = new CheckoutRequest(
                itens,
                "EXPRESSA",
                null,
                "PIX",
                null,
                "OURO",
                "SUDESTE"
        );

        CheckoutResponse response = service.calcular(request);

        assertEquals(new BigDecimal("409.70"), response.subtotalProdutos());
        assertEquals(BigDecimal.ZERO.setScale(2, java.math.RoundingMode.HALF_EVEN), response.descontoCupom());
        assertEquals(new BigDecimal("0.00"), response.frete()); // 0.00 já tem scale 2
        assertEquals(2, response.prazoEntregaDias());
        assertEquals(new BigDecimal("4.10"), response.seguro());
        assertEquals(new BigDecimal("-20.69"), response.ajustePagamento());
        assertEquals(new BigDecimal("393.11"), response.totalFinal());
        assertEquals(1, response.parcelas());
        assertEquals(new BigDecimal("393.11"), response.valorParcela());
        assertEquals(new BigDecimal("20.48"), response.creditoProximaCompra());
        assertFalse(response.brinde());
    }

    @Test
    public void testPedidoInvalido() {
        CheckoutRequest request = new CheckoutRequest(
                Arrays.asList(),
                "EXPRESSA",
                null,
                "PIX",
                null,
                "BRONZE",
                "NORTE"
        );

        CheckoutResponse response = service.calcular(request);
        assertEquals("PEDIDO_INVALIDO", response.erro());
    }

    @Test
    public void testNivelClubeInvalido() {
        List<ItemRequest> itens = Arrays.asList(
                new ItemRequest("Camiseta", 79.90, 1, 0.30)
        );

        CheckoutRequest request = new CheckoutRequest(
                itens,
                "EXPRESSA",
                null,
                "PIX",
                null,
                "INVALIDO",
                "NORTE"
        );

        CheckoutResponse response = service.calcular(request);
        assertEquals("NIVEL_CLUBE_INVALIDO", response.erro());
    }

    @Test
    public void testRegiaoInvalida() {
        List<ItemRequest> itens = Arrays.asList(
                new ItemRequest("Camiseta", 79.90, 1, 0.30)
        );

        CheckoutRequest request = new CheckoutRequest(
                itens,
                "EXPRESSA",
                null,
                "PIX",
                null,
                "BRONZE",
                "INVALIDA"
        );

        CheckoutResponse response = service.calcular(request);
        assertEquals("REGIAO_INVALIDA", response.erro());
    }

    @Test
    public void testModalidadeInvalida() {
        List<ItemRequest> itens = Arrays.asList(
                new ItemRequest("Camiseta", 79.90, 1, 0.30)
        );

        CheckoutRequest request = new CheckoutRequest(
                itens,
                "INVALIDA",
                null,
                "PIX",
                null,
                "BRONZE",
                "NORTE"
        );

        CheckoutResponse response = service.calcular(request);
        assertEquals("MODALIDADE_INVALIDA", response.erro());
    }

    @Test
    public void testModalidadeIndisponivel() {
        List<ItemRequest> itens = Arrays.asList(
                new ItemRequest("Produto pesado", 100.00, 1, 10.00)
        );

        CheckoutRequest request = new CheckoutRequest(
                itens,
                "MOTOBOY",
                null,
                "PIX",
                null,
                "BRONZE",
                "NORTE"
        );

        CheckoutResponse response = service.calcular(request);
        assertEquals("MODALIDADE_INDISPONIVEL", response.erro());
    }

    @Test
    public void testCupomInvalido() {
        List<ItemRequest> itens = Arrays.asList(
                new ItemRequest("Camiseta", 79.90, 1, 0.30)
        );

        CheckoutRequest request = new CheckoutRequest(
                itens,
                "EXPRESSA",
                "CUPOMINVALIDO",
                "PIX",
                null,
                "BRONZE",
                "NORTE"
        );

        CheckoutResponse response = service.calcular(request);
        assertEquals("CUPOM_INVALIDO", response.erro());
    }

    @Test
    public void testCupomNaoAplicavel() {
        List<ItemRequest> itens = Arrays.asList(
                new ItemRequest("Camiseta", 79.90, 1, 0.30)
        );

        CheckoutRequest request = new CheckoutRequest(
                itens,
                "EXPRESSA",
                "MENOS50",
                "PIX",
                null,
                "BRONZE",
                "NORTE"
        );

        CheckoutResponse response = service.calcular(request);
        assertEquals("CUPOM_NAO_APLICAVEL", response.erro());
    }

    @Test
    public void testFormaPagamentoInvalida() {
        List<ItemRequest> itens = Arrays.asList(
                new ItemRequest("Camiseta", 79.90, 1, 0.30)
        );

        CheckoutRequest request = new CheckoutRequest(
                itens,
                "EXPRESSA",
                null,
                "INVALIDA",
                null,
                "BRONZE",
                "NORTE"
        );

        CheckoutResponse response = service.calcular(request);
        assertEquals("FORMA_PAGAMENTO_INVALIDA", response.erro());
    }

    @Test
    public void testParcelamentoInvalido_Pix() {
        List<ItemRequest> itens = Arrays.asList(
                new ItemRequest("Camiseta", 79.90, 1, 0.30)
        );

        CheckoutRequest request = new CheckoutRequest(
                itens,
                "EXPRESSA",
                null,
                "PIX",
                2,
                "BRONZE",
                "NORTE"
        );

        CheckoutResponse response = service.calcular(request);
        assertEquals("PARCELAMENTO_INVALIDO", response.erro());
    }

    @Test
    public void testParcelamentoInvalido_CartaoAcima12() {
        List<ItemRequest> itens = Arrays.asList(
                new ItemRequest("Camiseta", 79.90, 1, 0.30)
        );

        CheckoutRequest request = new CheckoutRequest(
                itens,
                "EXPRESSA",
                null,
                "CARTAO",
                13,
                "BRONZE",
                "NORTE"
        );

        CheckoutResponse response = service.calcular(request);
        assertEquals("PARCELAMENTO_INVALIDO", response.erro());
    }

    @Test
    public void testFormaPagamentoIndisponivel_BoletoAcimaDoLimite() {
        List<ItemRequest> itens = Arrays.asList(
                new ItemRequest("Produto", 1001.00, 1, 0.30)
        );

        CheckoutRequest request = new CheckoutRequest(
                itens,
                "RETIRADA_LOJA",
                null,
                "BOLETO",
                null,
                "BRONZE",
                "NORTE"
        );

        CheckoutResponse response = service.calcular(request);
        assertEquals("FORMA_PAGAMENTO_INDISPONIVEL", response.erro());
    }

    @Test
    public void testBrindeOuro() {
        List<ItemRequest> itens = Arrays.asList(
                new ItemRequest("Produto", 501.00, 1, 0.30)
        );

        CheckoutRequest request = new CheckoutRequest(
                itens,
                "RETIRADA_LOJA",
                null,
                "PIX",
                null,
                "OURO",
                "NORTE"
        );

        CheckoutResponse response = service.calcular(request);
        assertTrue(response.brinde());
    }

    @Test
    public void testFretegratisDesconto() {
        List<ItemRequest> itens = Arrays.asList(
                new ItemRequest("Camiseta", 79.90, 2, 0.30),
                new ItemRequest("Tênis", 249.90, 1, 1.20)
        );

        CheckoutRequest request = new CheckoutRequest(
                itens,
                "EXPRESSA",
                "FRETEGRATIS",
                "PIX",
                null,
                "BRONZE",
                "NORTE"
        );

        CheckoutResponse response = service.calcular(request);

        // O desconto deve ser igual ao valor do frete
        BigDecimal freteSemDesconto = new BigDecimal("33.10");
        assertEquals(freteSemDesconto, response.descontoCupom());
        assertEquals(BigDecimal.ZERO.setScale(2, java.math.RoundingMode.HALF_EVEN), response.frete());
    }
}

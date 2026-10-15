package br.tcc.checkout.service;

import static org.junit.jupiter.api.Assertions.*;
import java.math.BigDecimal;
import java.util.Arrays;
import org.junit.jupiter.api.Test;
import br.tcc.checkout.dto.CheckoutRequest;
import br.tcc.checkout.dto.CheckoutResponse;
import br.tcc.checkout.dto.Item;

class CheckoutServiceTest {

    private CheckoutService service = new CheckoutService();

    @Test
    void exemplo1_CamisetaTenisExpressaBemvindo10Pix() throws CheckoutException {
        Item camiseta = new Item("Camiseta", new BigDecimal("79.90"), 2, new BigDecimal("0.30"));
        Item tenis = new Item("Tênis", new BigDecimal("249.90"), 1, new BigDecimal("1.20"));

        CheckoutRequest request = new CheckoutRequest(
            Arrays.asList(camiseta, tenis),
            "EXPRESSA",
            "BEMVINDO10",
            "PIX",
            1
        );

        CheckoutResponse response = service.calcularResumo(request);

        assertEquals(new BigDecimal("409.70"), response.subtotalProdutos());
        assertEquals(new BigDecimal("40.97"), response.descontoCupom());
        assertEquals(new BigDecimal("33.10"), response.frete());
        assertEquals(2, response.prazoEntregaDias());
        assertEquals(new BigDecimal("-20.09"), response.ajustePagamento());
        assertEquals(new BigDecimal("381.74"), response.totalFinal());
        assertEquals(1, response.parcelas());
        assertEquals(new BigDecimal("381.74"), response.valorParcela());
    }

    @Test
    void exemplo2_CamisetaTenisEconomicaSemCupomCartao6x() throws CheckoutException {
        Item camiseta = new Item("Camiseta", new BigDecimal("79.90"), 2, new BigDecimal("0.30"));
        Item tenis = new Item("Tênis", new BigDecimal("249.90"), 1, new BigDecimal("1.20"));

        CheckoutRequest request = new CheckoutRequest(
            Arrays.asList(camiseta, tenis),
            "ECONOMICA",
            null,
            "CARTAO",
            6
        );

        CheckoutResponse response = service.calcularResumo(request);

        assertEquals(new BigDecimal("409.70"), response.subtotalProdutos());
        assertEquals(new BigDecimal("0.00"), response.descontoCupom());
        assertEquals(new BigDecimal("15.60"), response.frete());
        assertEquals(7, response.prazoEntregaDias());
        assertEquals(new BigDecimal("30.10"), response.ajustePagamento());
        assertEquals(new BigDecimal("455.40"), response.totalFinal());
        assertEquals(6, response.parcelas());
        assertEquals(new BigDecimal("75.90"), response.valorParcela());
    }

    @Test
    void exemplo3_FoneMotoboyCupomMenos50Boleto() throws CheckoutException {
        Item fone = new Item("Fone", new BigDecimal("199.90"), 2, new BigDecimal("0.25"));

        CheckoutRequest request = new CheckoutRequest(
            Arrays.asList(fone),
            "MOTOBOY",
            "MENOS50",
            "BOLETO",
            1
        );

        CheckoutResponse response = service.calcularResumo(request);

        assertEquals(new BigDecimal("399.80"), response.subtotalProdutos());
        assertEquals(new BigDecimal("50.00"), response.descontoCupom());
        assertEquals(new BigDecimal("18.00"), response.frete());
        assertEquals(0, response.prazoEntregaDias());
        assertEquals(new BigDecimal("3.49"), response.ajustePagamento());
        assertEquals(new BigDecimal("371.29"), response.totalFinal());
        assertEquals(1, response.parcelas());
        assertEquals(new BigDecimal("371.29"), response.valorParcela());
    }

    @Test
    void exemplo4_MeiaCamisetaRetirLojaCupomLeve3Pague2Cartao3x() throws CheckoutException {
        Item meia = new Item("Meia", new BigDecimal("19.90"), 7, new BigDecimal("0.10"));
        Item camiseta = new Item("Camiseta", new BigDecimal("79.90"), 2, new BigDecimal("0.30"));

        CheckoutRequest request = new CheckoutRequest(
            Arrays.asList(meia, camiseta),
            "RETIRADA_LOJA",
            "LEVE3PAGUE2",
            "CARTAO",
            3
        );

        CheckoutResponse response = service.calcularResumo(request);

        assertEquals(new BigDecimal("299.10"), response.subtotalProdutos());
        assertEquals(new BigDecimal("39.80"), response.descontoCupom());
        assertEquals(new BigDecimal("0.00"), response.frete());
        assertEquals(1, response.prazoEntregaDias());
        assertEquals(new BigDecimal("0.00"), response.ajustePagamento());
        assertEquals(new BigDecimal("259.30"), response.totalFinal());
        assertEquals(3, response.parcelas());
        assertEquals(new BigDecimal("86.43"), response.valorParcela());
    }

    @Test
    void validacaoPedidoVazio() {
        CheckoutRequest request = new CheckoutRequest(
            Arrays.asList(),
            "ECONOMICA",
            null,
            "PIX",
            1
        );

        CheckoutException exception = assertThrows(CheckoutException.class, () -> {
            service.calcularResumo(request);
        });

        assertEquals("PEDIDO_INVALIDO", exception.getCodigo());
    }

    @Test
    void validacaoModalidadeInexistente() {
        Item item = new Item("Item", new BigDecimal("10.00"), 1, new BigDecimal("0.10"));

        CheckoutRequest request = new CheckoutRequest(
            Arrays.asList(item),
            "INVALIDA",
            null,
            "PIX",
            1
        );

        CheckoutException exception = assertThrows(CheckoutException.class, () -> {
            service.calcularResumo(request);
        });

        assertEquals("MODALIDADE_INVALIDA", exception.getCodigo());
    }

    @Test
    void validacaoMotoboySobre5kg() {
        Item item = new Item("Item", new BigDecimal("10.00"), 1, new BigDecimal("6.00"));

        CheckoutRequest request = new CheckoutRequest(
            Arrays.asList(item),
            "MOTOBOY",
            null,
            "PIX",
            1
        );

        CheckoutException exception = assertThrows(CheckoutException.class, () -> {
            service.calcularResumo(request);
        });

        assertEquals("MODALIDADE_INDISPONIVEL", exception.getCodigo());
    }

    @Test
    void validacaoCupomInexistente() {
        Item item = new Item("Item", new BigDecimal("10.00"), 1, new BigDecimal("0.10"));

        CheckoutRequest request = new CheckoutRequest(
            Arrays.asList(item),
            "ECONOMICA",
            "CUPOMINVALIDO",
            "PIX",
            1
        );

        CheckoutException exception = assertThrows(CheckoutException.class, () -> {
            service.calcularResumo(request);
        });

        assertEquals("CUPOM_INVALIDO", exception.getCodigo());
    }

    @Test
    void validacaoCupomMenos50AbaixoDe300() {
        Item item = new Item("Item", new BigDecimal("100.00"), 1, new BigDecimal("0.10"));

        CheckoutRequest request = new CheckoutRequest(
            Arrays.asList(item),
            "ECONOMICA",
            "MENOS50",
            "PIX",
            1
        );

        CheckoutException exception = assertThrows(CheckoutException.class, () -> {
            service.calcularResumo(request);
        });

        assertEquals("CUPOM_NAO_APLICAVEL", exception.getCodigo());
    }

    @Test
    void validacaoFormaPagamentoInvalida() {
        Item item = new Item("Item", new BigDecimal("10.00"), 1, new BigDecimal("0.10"));

        CheckoutRequest request = new CheckoutRequest(
            Arrays.asList(item),
            "ECONOMICA",
            null,
            "INVALIDA",
            1
        );

        CheckoutException exception = assertThrows(CheckoutException.class, () -> {
            service.calcularResumo(request);
        });

        assertEquals("FORMA_PAGAMENTO_INVALIDA", exception.getCodigo());
    }

    @Test
    void validacaoParcelamentoPixInvalido() {
        Item item = new Item("Item", new BigDecimal("10.00"), 1, new BigDecimal("0.10"));

        CheckoutRequest request = new CheckoutRequest(
            Arrays.asList(item),
            "ECONOMICA",
            null,
            "PIX",
            2
        );

        CheckoutException exception = assertThrows(CheckoutException.class, () -> {
            service.calcularResumo(request);
        });

        assertEquals("PARCELAMENTO_INVALIDO", exception.getCodigo());
    }

    @Test
    void validacaoBoletoAcima1000() {
        Item item = new Item("Item", new BigDecimal("2000.00"), 1, new BigDecimal("0.10"));

        CheckoutRequest request = new CheckoutRequest(
            Arrays.asList(item),
            "RETIRADA_LOJA",
            null,
            "BOLETO",
            1
        );

        CheckoutException exception = assertThrows(CheckoutException.class, () -> {
            service.calcularResumo(request);
        });

        assertEquals("FORMA_PAGAMENTO_INDISPONIVEL", exception.getCodigo());
    }
}

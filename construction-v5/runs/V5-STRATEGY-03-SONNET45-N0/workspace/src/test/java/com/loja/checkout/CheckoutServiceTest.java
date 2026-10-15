package com.loja.checkout;

import com.loja.checkout.dto.CheckoutRequest;
import com.loja.checkout.dto.CheckoutResponse;
import com.loja.checkout.dto.ItemCarrinho;
import com.loja.checkout.exception.CheckoutException;
import com.loja.checkout.service.CheckoutService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class CheckoutServiceTest {

    private CheckoutService checkoutService;

    @BeforeEach
    void setUp() {
        checkoutService = new CheckoutService();
    }

    @Test
    void exemplo1_camisetaTenisExpressaBemvindo10PixBronzeNorte() {
        CheckoutRequest request = new CheckoutRequest();

        List<ItemCarrinho> itens = new ArrayList<>();
        itens.add(new ItemCarrinho("Camiseta", new BigDecimal("79.90"), 2, new BigDecimal("0.30")));
        itens.add(new ItemCarrinho("Tênis", new BigDecimal("249.90"), 1, new BigDecimal("1.20")));

        request.setItens(itens);
        request.setModalidadeEntrega("EXPRESSA");
        request.setCupom("BEMVINDO10");
        request.setFormaPagamento("PIX");
        request.setNivelClube("BRONZE");
        request.setRegiao("NORTE");

        CheckoutResponse response = checkoutService.calcularResumo(request);

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
    void exemplo2_camisetaTenisEconomicaSemCupomCartao6xPrataCentroOeste() {
        CheckoutRequest request = new CheckoutRequest();

        List<ItemCarrinho> itens = new ArrayList<>();
        itens.add(new ItemCarrinho("Camiseta", new BigDecimal("79.90"), 2, new BigDecimal("0.30")));
        itens.add(new ItemCarrinho("Tênis", new BigDecimal("249.90"), 1, new BigDecimal("1.20")));

        request.setItens(itens);
        request.setModalidadeEntrega("ECONOMICA");
        request.setFormaPagamento("CARTAO");
        request.setParcelas(6);
        request.setNivelClube("PRATA");
        request.setRegiao("CENTRO_OESTE");

        CheckoutResponse response = checkoutService.calcularResumo(request);

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
    void exemplo3_fonemotoboyMenos50BoletoBronzeNordeste() {
        CheckoutRequest request = new CheckoutRequest();

        List<ItemCarrinho> itens = new ArrayList<>();
        itens.add(new ItemCarrinho("Fone", new BigDecimal("199.90"), 2, new BigDecimal("0.25")));

        request.setItens(itens);
        request.setModalidadeEntrega("MOTOBOY");
        request.setCupom("MENOS50");
        request.setFormaPagamento("BOLETO");
        request.setNivelClube("BRONZE");
        request.setRegiao("NORDESTE");

        CheckoutResponse response = checkoutService.calcularResumo(request);

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
    void exemplo4_meiaCamisetaRetiradaLeve3Pague2Cartao3xPrataSul() {
        CheckoutRequest request = new CheckoutRequest();

        List<ItemCarrinho> itens = new ArrayList<>();
        itens.add(new ItemCarrinho("Meia", new BigDecimal("19.90"), 7, new BigDecimal("0.10")));
        itens.add(new ItemCarrinho("Camiseta", new BigDecimal("79.90"), 2, new BigDecimal("0.30")));

        request.setItens(itens);
        request.setModalidadeEntrega("RETIRADA_LOJA");
        request.setCupom("LEVE3PAGUE2");
        request.setFormaPagamento("CARTAO");
        request.setParcelas(3);
        request.setNivelClube("PRATA");
        request.setRegiao("SUL");

        CheckoutResponse response = checkoutService.calcularResumo(request);

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
    void exemplo5_camisetaTenisExpressaSemCupomPixOuroSudeste() {
        CheckoutRequest request = new CheckoutRequest();

        List<ItemCarrinho> itens = new ArrayList<>();
        itens.add(new ItemCarrinho("Camiseta", new BigDecimal("79.90"), 2, new BigDecimal("0.30")));
        itens.add(new ItemCarrinho("Tênis", new BigDecimal("249.90"), 1, new BigDecimal("1.20")));

        request.setItens(itens);
        request.setModalidadeEntrega("EXPRESSA");
        request.setFormaPagamento("PIX");
        request.setNivelClube("OURO");
        request.setRegiao("SUDESTE");

        CheckoutResponse response = checkoutService.calcularResumo(request);

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
    void erro_carrinhoVazio() {
        CheckoutRequest request = new CheckoutRequest();
        request.setItens(new ArrayList<>());
        request.setModalidadeEntrega("EXPRESSA");
        request.setFormaPagamento("PIX");
        request.setNivelClube("BRONZE");
        request.setRegiao("SUDESTE");

        CheckoutException exception = assertThrows(CheckoutException.class, () -> {
            checkoutService.calcularResumo(request);
        });

        assertEquals("PEDIDO_INVALIDO", exception.getCodigoErro());
    }

    @Test
    void erro_nivelClubeInvalido() {
        CheckoutRequest request = new CheckoutRequest();

        List<ItemCarrinho> itens = new ArrayList<>();
        itens.add(new ItemCarrinho("Camiseta", new BigDecimal("79.90"), 1, new BigDecimal("0.30")));

        request.setItens(itens);
        request.setModalidadeEntrega("EXPRESSA");
        request.setFormaPagamento("PIX");
        request.setNivelClube("PLATINA");
        request.setRegiao("SUDESTE");

        CheckoutException exception = assertThrows(CheckoutException.class, () -> {
            checkoutService.calcularResumo(request);
        });

        assertEquals("NIVEL_CLUBE_INVALIDO", exception.getCodigoErro());
    }

    @Test
    void erro_motoboyAcima5kg() {
        CheckoutRequest request = new CheckoutRequest();

        List<ItemCarrinho> itens = new ArrayList<>();
        itens.add(new ItemCarrinho("Camiseta", new BigDecimal("79.90"), 20, new BigDecimal("0.30")));

        request.setItens(itens);
        request.setModalidadeEntrega("MOTOBOY");
        request.setFormaPagamento("PIX");
        request.setNivelClube("BRONZE");
        request.setRegiao("SUDESTE");

        CheckoutException exception = assertThrows(CheckoutException.class, () -> {
            checkoutService.calcularResumo(request);
        });

        assertEquals("MODALIDADE_INDISPONIVEL", exception.getCodigoErro());
    }

    @Test
    void erro_cupomInvalido() {
        CheckoutRequest request = new CheckoutRequest();

        List<ItemCarrinho> itens = new ArrayList<>();
        itens.add(new ItemCarrinho("Camiseta", new BigDecimal("79.90"), 1, new BigDecimal("0.30")));

        request.setItens(itens);
        request.setModalidadeEntrega("EXPRESSA");
        request.setCupom("CUPOMINEXISTENTE");
        request.setFormaPagamento("PIX");
        request.setNivelClube("BRONZE");
        request.setRegiao("SUDESTE");

        CheckoutException exception = assertThrows(CheckoutException.class, () -> {
            checkoutService.calcularResumo(request);
        });

        assertEquals("CUPOM_INVALIDO", exception.getCodigoErro());
    }

    @Test
    void erro_menos50Abaixo300() {
        CheckoutRequest request = new CheckoutRequest();

        List<ItemCarrinho> itens = new ArrayList<>();
        itens.add(new ItemCarrinho("Camiseta", new BigDecimal("79.90"), 1, new BigDecimal("0.30")));

        request.setItens(itens);
        request.setModalidadeEntrega("EXPRESSA");
        request.setCupom("MENOS50");
        request.setFormaPagamento("PIX");
        request.setNivelClube("BRONZE");
        request.setRegiao("SUDESTE");

        CheckoutException exception = assertThrows(CheckoutException.class, () -> {
            checkoutService.calcularResumo(request);
        });

        assertEquals("CUPOM_NAO_APLICAVEL", exception.getCodigoErro());
    }

    @Test
    void erro_boletoAcima1000() {
        CheckoutRequest request = new CheckoutRequest();

        List<ItemCarrinho> itens = new ArrayList<>();
        itens.add(new ItemCarrinho("Notebook", new BigDecimal("3000.00"), 1, new BigDecimal("2.00")));

        request.setItens(itens);
        request.setModalidadeEntrega("EXPRESSA");
        request.setFormaPagamento("BOLETO");
        request.setNivelClube("BRONZE");
        request.setRegiao("SUDESTE");

        CheckoutException exception = assertThrows(CheckoutException.class, () -> {
            checkoutService.calcularResumo(request);
        });

        assertEquals("FORMA_PAGAMENTO_INDISPONIVEL", exception.getCodigoErro());
    }

    @Test
    void erro_parcelamentoInvalido() {
        CheckoutRequest request = new CheckoutRequest();

        List<ItemCarrinho> itens = new ArrayList<>();
        itens.add(new ItemCarrinho("Camiseta", new BigDecimal("79.90"), 1, new BigDecimal("0.30")));

        request.setItens(itens);
        request.setModalidadeEntrega("EXPRESSA");
        request.setFormaPagamento("PIX");
        request.setParcelas(3);
        request.setNivelClube("BRONZE");
        request.setRegiao("SUDESTE");

        CheckoutException exception = assertThrows(CheckoutException.class, () -> {
            checkoutService.calcularResumo(request);
        });

        assertEquals("PARCELAMENTO_INVALIDO", exception.getCodigoErro());
    }
}

package com.loja.checkout;

import com.loja.checkout.dto.CheckoutRequest;
import com.loja.checkout.dto.CheckoutResponse;
import com.loja.checkout.dto.ItemRequest;
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

        List<ItemRequest> itens = new ArrayList<>();
        ItemRequest camiseta = new ItemRequest();
        camiseta.setNome("Camiseta");
        camiseta.setPrecoUnitario(new BigDecimal("79.90"));
        camiseta.setQuantidade(2);
        camiseta.setPesoKg(new BigDecimal("0.30"));
        itens.add(camiseta);

        ItemRequest tenis = new ItemRequest();
        tenis.setNome("Tênis");
        tenis.setPrecoUnitario(new BigDecimal("249.90"));
        tenis.setQuantidade(1);
        tenis.setPesoKg(new BigDecimal("1.20"));
        itens.add(tenis);

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
        assertFalse(response.getBrinde());
    }

    @Test
    void exemplo2_camisetaTenisEconomicaSemCupomCartao6xPrataCentroOeste() {
        CheckoutRequest request = new CheckoutRequest();

        List<ItemRequest> itens = new ArrayList<>();
        ItemRequest camiseta = new ItemRequest();
        camiseta.setNome("Camiseta");
        camiseta.setPrecoUnitario(new BigDecimal("79.90"));
        camiseta.setQuantidade(2);
        camiseta.setPesoKg(new BigDecimal("0.30"));
        itens.add(camiseta);

        ItemRequest tenis = new ItemRequest();
        tenis.setNome("Tênis");
        tenis.setPrecoUnitario(new BigDecimal("249.90"));
        tenis.setQuantidade(1);
        tenis.setPesoKg(new BigDecimal("1.20"));
        itens.add(tenis);

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
        assertFalse(response.getBrinde());
    }

    @Test
    void exemplo3_foneMotoboy2Menos50BoletoBronzeNordeste() {
        CheckoutRequest request = new CheckoutRequest();

        List<ItemRequest> itens = new ArrayList<>();
        ItemRequest fone = new ItemRequest();
        fone.setNome("Fone");
        fone.setPrecoUnitario(new BigDecimal("199.90"));
        fone.setQuantidade(2);
        fone.setPesoKg(new BigDecimal("0.25"));
        itens.add(fone);

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
        assertFalse(response.getBrinde());
    }

    @Test
    void exemplo4_meiaCamisetaRetiradaLeve3Pague2Cartao3xPrataSul() {
        CheckoutRequest request = new CheckoutRequest();

        List<ItemRequest> itens = new ArrayList<>();
        ItemRequest meia = new ItemRequest();
        meia.setNome("Meia");
        meia.setPrecoUnitario(new BigDecimal("19.90"));
        meia.setQuantidade(7);
        meia.setPesoKg(new BigDecimal("0.10"));
        itens.add(meia);

        ItemRequest camiseta = new ItemRequest();
        camiseta.setNome("Camiseta");
        camiseta.setPrecoUnitario(new BigDecimal("79.90"));
        camiseta.setQuantidade(2);
        camiseta.setPesoKg(new BigDecimal("0.30"));
        itens.add(camiseta);

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
        assertFalse(response.getBrinde());
    }

    @Test
    void exemplo5_camisetaTenisExpressaSemCupomPixOuroSudeste() {
        CheckoutRequest request = new CheckoutRequest();

        List<ItemRequest> itens = new ArrayList<>();
        ItemRequest camiseta = new ItemRequest();
        camiseta.setNome("Camiseta");
        camiseta.setPrecoUnitario(new BigDecimal("79.90"));
        camiseta.setQuantidade(2);
        camiseta.setPesoKg(new BigDecimal("0.30"));
        itens.add(camiseta);

        ItemRequest tenis = new ItemRequest();
        tenis.setNome("Tênis");
        tenis.setPrecoUnitario(new BigDecimal("249.90"));
        tenis.setQuantidade(1);
        tenis.setPesoKg(new BigDecimal("1.20"));
        itens.add(tenis);

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
        assertFalse(response.getBrinde());
    }

    @Test
    void erro_pedidoInvalido_carrinhoVazio() {
        CheckoutRequest request = new CheckoutRequest();
        request.setItens(new ArrayList<>());
        request.setModalidadeEntrega("EXPRESSA");
        request.setFormaPagamento("PIX");
        request.setNivelClube("BRONZE");
        request.setRegiao("SUDESTE");

        CheckoutService.CheckoutException exception = assertThrows(
            CheckoutService.CheckoutException.class,
            () -> checkoutService.calcularResumo(request)
        );
        assertEquals("PEDIDO_INVALIDO", exception.getCodigo());
    }

    @Test
    void erro_nivelClubeInvalido() {
        CheckoutRequest request = new CheckoutRequest();

        List<ItemRequest> itens = new ArrayList<>();
        ItemRequest item = new ItemRequest();
        item.setNome("Produto");
        item.setPrecoUnitario(new BigDecimal("100.00"));
        item.setQuantidade(1);
        item.setPesoKg(new BigDecimal("1.00"));
        itens.add(item);

        request.setItens(itens);
        request.setModalidadeEntrega("EXPRESSA");
        request.setFormaPagamento("PIX");
        request.setNivelClube("PLATINA");
        request.setRegiao("SUDESTE");

        CheckoutService.CheckoutException exception = assertThrows(
            CheckoutService.CheckoutException.class,
            () -> checkoutService.calcularResumo(request)
        );
        assertEquals("NIVEL_CLUBE_INVALIDO", exception.getCodigo());
    }

    @Test
    void erro_modalidadeIndisponivel_motoboyAcima5Kg() {
        CheckoutRequest request = new CheckoutRequest();

        List<ItemRequest> itens = new ArrayList<>();
        ItemRequest item = new ItemRequest();
        item.setNome("Produto Pesado");
        item.setPrecoUnitario(new BigDecimal("100.00"));
        item.setQuantidade(1);
        item.setPesoKg(new BigDecimal("6.00"));
        itens.add(item);

        request.setItens(itens);
        request.setModalidadeEntrega("MOTOBOY");
        request.setFormaPagamento("PIX");
        request.setNivelClube("BRONZE");
        request.setRegiao("SUDESTE");

        CheckoutService.CheckoutException exception = assertThrows(
            CheckoutService.CheckoutException.class,
            () -> checkoutService.calcularResumo(request)
        );
        assertEquals("MODALIDADE_INDISPONIVEL", exception.getCodigo());
    }

    @Test
    void erro_cupomNaoAplicavel_menos50Abaixo300() {
        CheckoutRequest request = new CheckoutRequest();

        List<ItemRequest> itens = new ArrayList<>();
        ItemRequest item = new ItemRequest();
        item.setNome("Produto");
        item.setPrecoUnitario(new BigDecimal("100.00"));
        item.setQuantidade(2);
        item.setPesoKg(new BigDecimal("1.00"));
        itens.add(item);

        request.setItens(itens);
        request.setModalidadeEntrega("EXPRESSA");
        request.setCupom("MENOS50");
        request.setFormaPagamento("PIX");
        request.setNivelClube("BRONZE");
        request.setRegiao("SUDESTE");

        CheckoutService.CheckoutException exception = assertThrows(
            CheckoutService.CheckoutException.class,
            () -> checkoutService.calcularResumo(request)
        );
        assertEquals("CUPOM_NAO_APLICAVEL", exception.getCodigo());
    }

    @Test
    void erro_formaPagamentoIndisponivel_boletoAcima1000() {
        CheckoutRequest request = new CheckoutRequest();

        List<ItemRequest> itens = new ArrayList<>();
        ItemRequest item = new ItemRequest();
        item.setNome("Produto Caro");
        item.setPrecoUnitario(new BigDecimal("1100.00"));
        item.setQuantidade(1);
        item.setPesoKg(new BigDecimal("1.00"));
        itens.add(item);

        request.setItens(itens);
        request.setModalidadeEntrega("RETIRADA_LOJA");
        request.setFormaPagamento("BOLETO");
        request.setNivelClube("BRONZE");
        request.setRegiao("SUDESTE");

        CheckoutService.CheckoutException exception = assertThrows(
            CheckoutService.CheckoutException.class,
            () -> checkoutService.calcularResumo(request)
        );
        assertEquals("FORMA_PAGAMENTO_INDISPONIVEL", exception.getCodigo());
    }
}

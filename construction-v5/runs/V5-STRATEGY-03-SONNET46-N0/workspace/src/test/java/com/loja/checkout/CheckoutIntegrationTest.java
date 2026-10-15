package com.loja.checkout;

import tools.jackson.databind.ObjectMapper;
import com.loja.checkout.dto.CheckoutRequest;
import com.loja.checkout.dto.CheckoutResponse;
import com.loja.checkout.dto.ErroResponse;
import com.loja.checkout.dto.ItemCarrinhoDto;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class CheckoutIntegrationTest {

    @Autowired
    MockMvc mvc;

    @Autowired
    ObjectMapper mapper;

    private CheckoutResponse calcular(CheckoutRequest req) throws Exception {
        MvcResult result = mvc.perform(post("/checkout/resumo")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(mapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andReturn();
        return mapper.readValue(result.getResponse().getContentAsString(), CheckoutResponse.class);
    }

    private ErroResponse calcularEsperandoErro(CheckoutRequest req) throws Exception {
        MvcResult result = mvc.perform(post("/checkout/resumo")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(mapper.writeValueAsString(req)))
                .andExpect(status().isUnprocessableEntity())
                .andReturn();
        return mapper.readValue(result.getResponse().getContentAsString(), ErroResponse.class);
    }

    private static final ItemCarrinhoDto CAMISETA = new ItemCarrinhoDto("Camiseta", new BigDecimal("79.90"), 2, new BigDecimal("0.30"));
    private static final ItemCarrinhoDto TENIS = new ItemCarrinhoDto("Tênis", new BigDecimal("249.90"), 1, new BigDecimal("1.20"));

    // Exemplo 1: Camiseta×2 + Tênis×1, EXPRESSA, BEMVINDO10, PIX, BRONZE, NORTE
    // subtotal 409,70 · cupom 40,97 · frete 33,10 · prazo 2 · seguro 10,24 · ajuste −20,60 · total 391,47 · 1×391,47 · crédito 0,00 · brinde não
    @Test
    void exemplo1() throws Exception {
        CheckoutRequest req = new CheckoutRequest(
                List.of(CAMISETA, TENIS),
                "EXPRESSA", "BEMVINDO10", "PIX", 1, "BRONZE", "NORTE");
        CheckoutResponse r = calcular(req);
        assertEquals(bd("409.70"), r.subtotalProdutos());
        assertEquals(bd("40.97"), r.descontoCupom());
        assertEquals(bd("33.10"), r.frete());
        assertEquals(2, r.prazoEntregaDias());
        assertEquals(bd("10.24"), r.seguro());
        assertEquals(bd("-20.60"), r.ajustePagamento());
        assertEquals(bd("391.47"), r.totalFinal());
        assertEquals(1, r.parcelas());
        assertEquals(bd("391.47"), r.valorParcela());
        assertEquals(bd("0.00"), r.creditoProximaCompra());
        assertFalse(r.brinde());
    }

    // Exemplo 2: Camiseta×2 + Tênis×1, ECONOMICA, sem cupom, CARTAO 6×, PRATA, CENTRO_OESTE
    // subtotal 409,70 · cupom 0,00 · frete 15,60 · prazo 7 · seguro 6,15 · ajuste 30,55 · total 462,00 · 6×77,00 · crédito 8,19 · brinde não
    @Test
    void exemplo2() throws Exception {
        CheckoutRequest req = new CheckoutRequest(
                List.of(CAMISETA, TENIS),
                "ECONOMICA", null, "CARTAO", 6, "PRATA", "CENTRO_OESTE");
        CheckoutResponse r = calcular(req);
        assertEquals(bd("409.70"), r.subtotalProdutos());
        assertEquals(bd("0.00"), r.descontoCupom());
        assertEquals(bd("15.60"), r.frete());
        assertEquals(7, r.prazoEntregaDias());
        assertEquals(bd("6.15"), r.seguro());
        assertEquals(bd("30.55"), r.ajustePagamento());
        assertEquals(bd("462.00"), r.totalFinal());
        assertEquals(6, r.parcelas());
        assertEquals(bd("77.00"), r.valorParcela());
        assertEquals(bd("8.19"), r.creditoProximaCompra());
        assertFalse(r.brinde());
    }

    // Exemplo 3: Fone×2, MOTOBOY, MENOS50, BOLETO, BRONZE, NORDESTE
    // subtotal 399,80 · cupom 50,00 · frete 18,00 · prazo 0 · seguro 8,00 · ajuste 3,49 · total 379,29 · 1×379,29 · crédito 0,00 · brinde não
    @Test
    void exemplo3() throws Exception {
        ItemCarrinhoDto fone = new ItemCarrinhoDto("Fone", new BigDecimal("199.90"), 2, new BigDecimal("0.25"));
        CheckoutRequest req = new CheckoutRequest(
                List.of(fone),
                "MOTOBOY", "MENOS50", "BOLETO", 1, "BRONZE", "NORDESTE");
        CheckoutResponse r = calcular(req);
        assertEquals(bd("399.80"), r.subtotalProdutos());
        assertEquals(bd("50.00"), r.descontoCupom());
        assertEquals(bd("18.00"), r.frete());
        assertEquals(0, r.prazoEntregaDias());
        assertEquals(bd("8.00"), r.seguro());
        assertEquals(bd("3.49"), r.ajustePagamento());
        assertEquals(bd("379.29"), r.totalFinal());
        assertEquals(1, r.parcelas());
        assertEquals(bd("379.29"), r.valorParcela());
        assertEquals(bd("0.00"), r.creditoProximaCompra());
        assertFalse(r.brinde());
    }

    // Exemplo 4: Meia×7 + Camiseta×2, RETIRADA_LOJA, LEVE3PAGUE2, CARTAO 3×, PRATA, SUL
    // subtotal 299,10 · cupom 39,80 · frete 0,00 · prazo 1 · seguro 2,99 · ajuste 0,00 · total 262,29 · 3×87,43 · crédito 5,98 · brinde não
    @Test
    void exemplo4() throws Exception {
        ItemCarrinhoDto meia = new ItemCarrinhoDto("Meia", new BigDecimal("19.90"), 7, new BigDecimal("0.10"));
        ItemCarrinhoDto camiseta = new ItemCarrinhoDto("Camiseta", new BigDecimal("79.90"), 2, new BigDecimal("0.30"));
        CheckoutRequest req = new CheckoutRequest(
                List.of(meia, camiseta),
                "RETIRADA_LOJA", "LEVE3PAGUE2", "CARTAO", 3, "PRATA", "SUL");
        CheckoutResponse r = calcular(req);
        assertEquals(bd("299.10"), r.subtotalProdutos());
        assertEquals(bd("39.80"), r.descontoCupom());
        assertEquals(bd("0.00"), r.frete());
        assertEquals(1, r.prazoEntregaDias());
        assertEquals(bd("2.99"), r.seguro());
        assertEquals(bd("0.00"), r.ajustePagamento());
        assertEquals(bd("262.29"), r.totalFinal());
        assertEquals(3, r.parcelas());
        assertEquals(bd("87.43"), r.valorParcela());
        assertEquals(bd("5.98"), r.creditoProximaCompra());
        assertFalse(r.brinde());
    }

    // Exemplo 5: Camiseta×2 + Tênis×1, EXPRESSA, sem cupom, PIX, OURO, SUDESTE
    // subtotal 409,70 · cupom 0,00 · frete 0,00 · prazo 2 · seguro 4,10 · ajuste −20,69 · total 393,11 · 1×393,11 · crédito 20,48 · brinde não
    @Test
    void exemplo5() throws Exception {
        CheckoutRequest req = new CheckoutRequest(
                List.of(CAMISETA, TENIS),
                "EXPRESSA", null, "PIX", 1, "OURO", "SUDESTE");
        CheckoutResponse r = calcular(req);
        assertEquals(bd("409.70"), r.subtotalProdutos());
        assertEquals(bd("0.00"), r.descontoCupom());
        assertEquals(bd("0.00"), r.frete());
        assertEquals(2, r.prazoEntregaDias());
        assertEquals(bd("4.10"), r.seguro());
        assertEquals(bd("-20.69"), r.ajustePagamento());
        assertEquals(bd("393.11"), r.totalFinal());
        assertEquals(1, r.parcelas());
        assertEquals(bd("393.11"), r.valorParcela());
        assertEquals(bd("20.48"), r.creditoProximaCompra());
        assertFalse(r.brinde());
    }

    @Test
    void erroCarrinhoVazio() throws Exception {
        CheckoutRequest req = new CheckoutRequest(
                List.of(), "ECONOMICA", null, "PIX", 1, "BRONZE", "SUDESTE");
        ErroResponse e = calcularEsperandoErro(req);
        assertEquals("PEDIDO_INVALIDO", e.erro());
    }

    @Test
    void erroItemPrecoZero() throws Exception {
        ItemCarrinhoDto item = new ItemCarrinhoDto("X", BigDecimal.ZERO, 1, new BigDecimal("0.5"));
        CheckoutRequest req = new CheckoutRequest(
                List.of(item), "ECONOMICA", null, "PIX", 1, "BRONZE", "SUDESTE");
        ErroResponse e = calcularEsperandoErro(req);
        assertEquals("PEDIDO_INVALIDO", e.erro());
    }

    @Test
    void erroNivelClubeInvalido() throws Exception {
        CheckoutRequest req = new CheckoutRequest(
                List.of(CAMISETA), "ECONOMICA", null, "PIX", 1, "DIAMANTE", "SUDESTE");
        ErroResponse e = calcularEsperandoErro(req);
        assertEquals("NIVEL_CLUBE_INVALIDO", e.erro());
    }

    @Test
    void erroRegiaoInvalida() throws Exception {
        CheckoutRequest req = new CheckoutRequest(
                List.of(CAMISETA), "ECONOMICA", null, "PIX", 1, "BRONZE", "AMAZONIA");
        ErroResponse e = calcularEsperandoErro(req);
        assertEquals("REGIAO_INVALIDA", e.erro());
    }

    @Test
    void erroModalidadeInvalida() throws Exception {
        CheckoutRequest req = new CheckoutRequest(
                List.of(CAMISETA), "DRONE", null, "PIX", 1, "BRONZE", "SUDESTE");
        ErroResponse e = calcularEsperandoErro(req);
        assertEquals("MODALIDADE_INVALIDA", e.erro());
    }

    @Test
    void erroMotoboyPesoExcedido() throws Exception {
        ItemCarrinhoDto pesado = new ItemCarrinhoDto("Sofá", new BigDecimal("999.00"), 1, new BigDecimal("6.0"));
        CheckoutRequest req = new CheckoutRequest(
                List.of(pesado), "MOTOBOY", null, "PIX", 1, "BRONZE", "SUDESTE");
        ErroResponse e = calcularEsperandoErro(req);
        assertEquals("MODALIDADE_INDISPONIVEL", e.erro());
    }

    @Test
    void erroCupomInvalido() throws Exception {
        CheckoutRequest req = new CheckoutRequest(
                List.of(CAMISETA), "ECONOMICA", "XPTO99", "PIX", 1, "BRONZE", "SUDESTE");
        ErroResponse e = calcularEsperandoErro(req);
        assertEquals("CUPOM_INVALIDO", e.erro());
    }

    @Test
    void erroCupomNaoAplicavel() throws Exception {
        ItemCarrinhoDto barato = new ItemCarrinhoDto("Meia", new BigDecimal("10.00"), 1, new BigDecimal("0.1"));
        CheckoutRequest req = new CheckoutRequest(
                List.of(barato), "ECONOMICA", "MENOS50", "PIX", 1, "BRONZE", "SUDESTE");
        ErroResponse e = calcularEsperandoErro(req);
        assertEquals("CUPOM_NAO_APLICAVEL", e.erro());
    }

    @Test
    void erroFormaPagamentoInvalida() throws Exception {
        CheckoutRequest req = new CheckoutRequest(
                List.of(CAMISETA), "ECONOMICA", null, "CRIPTO", 1, "BRONZE", "SUDESTE");
        ErroResponse e = calcularEsperandoErro(req);
        assertEquals("FORMA_PAGAMENTO_INVALIDA", e.erro());
    }

    @Test
    void erroParcelamentoPixInvalido() throws Exception {
        CheckoutRequest req = new CheckoutRequest(
                List.of(CAMISETA), "ECONOMICA", null, "PIX", 3, "BRONZE", "SUDESTE");
        ErroResponse e = calcularEsperandoErro(req);
        assertEquals("PARCELAMENTO_INVALIDO", e.erro());
    }

    @Test
    void erroBoletoPedidoAcimaLimite() throws Exception {
        ItemCarrinhoDto caro = new ItemCarrinhoDto("TV", new BigDecimal("1200.00"), 1, new BigDecimal("2.0"));
        CheckoutRequest req = new CheckoutRequest(
                List.of(caro), "ECONOMICA", null, "BOLETO", 1, "BRONZE", "SUDESTE");
        ErroResponse e = calcularEsperandoErro(req);
        assertEquals("FORMA_PAGAMENTO_INDISPONIVEL", e.erro());
    }

    @Test
    void erroCartaoParcelasAcimaLimite() throws Exception {
        CheckoutRequest req = new CheckoutRequest(
                List.of(CAMISETA), "ECONOMICA", null, "CARTAO", 13, "BRONZE", "SUDESTE");
        ErroResponse e = calcularEsperandoErro(req);
        assertEquals("PARCELAMENTO_INVALIDO", e.erro());
    }

    private BigDecimal bd(String val) {
        return new BigDecimal(val);
    }
}

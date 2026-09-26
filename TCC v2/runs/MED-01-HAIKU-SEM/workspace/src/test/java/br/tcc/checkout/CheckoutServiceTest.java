package br.tcc.checkout;

import br.tcc.checkout.dto.ItemCarrinho;
import br.tcc.checkout.dto.RequisicaoResumo;
import br.tcc.checkout.dto.RespostaResumo;
import br.tcc.checkout.service.CheckoutService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class CheckoutServiceTest {

    private CheckoutService checkoutService;

    @BeforeEach
    void setup() {
        checkoutService = new CheckoutService();
    }

    @Test
    void testExemplo1() {
        // Camiseta 79,90 × 2 (0,30 kg) + Tênis 249,90 × 1 (1,20 kg)
        // EXPRESSA, cupom BEMVINDO10, PIX
        // Esperado: subtotal 409,70 · cupom 40,97 · frete 33,10 · prazo 2 · ajuste −20,09 · total 381,74 · 1×381,74

        List<ItemCarrinho> itens = Arrays.asList(
                new ItemCarrinho("Camiseta", 79.90, 2, 0.30),
                new ItemCarrinho("Tênis", 249.90, 1, 1.20)
        );

        RequisicaoResumo requisicao = new RequisicaoResumo();
        requisicao.setItens(itens);
        requisicao.setModalidadeEntrega("EXPRESSA");
        requisicao.setCupom("BEMVINDO10");
        requisicao.setFormaPagamento("PIX");
        requisicao.setParcelas(1);

        RespostaResumo resposta = checkoutService.calcularResumo(requisicao);

        assertEquals(new BigDecimal("409.70"), resposta.getSubtotalProdutos());
        assertEquals(new BigDecimal("40.97"), resposta.getDescontoCupom());
        assertEquals(new BigDecimal("33.10"), resposta.getFrete());
        assertEquals(2, resposta.getPrazoEntregaDias());
        assertEquals(new BigDecimal("-20.09"), resposta.getAjustePagamento());
        assertEquals(new BigDecimal("381.74"), resposta.getTotalFinal());
        assertEquals(1, resposta.getParcelas());
        assertEquals(new BigDecimal("381.74"), resposta.getValorParcela());
    }

    @Test
    void testExemplo2() {
        // Camiseta 79,90 × 2 (0,30 kg) + Tênis 249,90 × 1 (1,20 kg)
        // ECONOMICA, sem cupom, CARTAO 6×
        // Esperado: subtotal 409,70 · cupom 0,00 · frete 15,60 · prazo 7 · ajuste 30,10 · total 455,40 · 6×75,90

        List<ItemCarrinho> itens = Arrays.asList(
                new ItemCarrinho("Camiseta", 79.90, 2, 0.30),
                new ItemCarrinho("Tênis", 249.90, 1, 1.20)
        );

        RequisicaoResumo requisicao = new RequisicaoResumo();
        requisicao.setItens(itens);
        requisicao.setModalidadeEntrega("ECONOMICA");
        requisicao.setCupom(null);
        requisicao.setFormaPagamento("CARTAO");
        requisicao.setParcelas(6);

        RespostaResumo resposta = checkoutService.calcularResumo(requisicao);

        assertEquals(new BigDecimal("409.70"), resposta.getSubtotalProdutos());
        assertEquals(0, resposta.getDescontoCupom().compareTo(new BigDecimal("0.00")));
        assertEquals(new BigDecimal("15.60"), resposta.getFrete());
        assertEquals(7, resposta.getPrazoEntregaDias());
        assertEquals(new BigDecimal("30.10"), resposta.getAjustePagamento());
        assertEquals(new BigDecimal("455.40"), resposta.getTotalFinal());
        assertEquals(6, resposta.getParcelas());
        assertEquals(new BigDecimal("75.90"), resposta.getValorParcela());
    }

    @Test
    void testExemplo3() {
        // Fone 199,90 × 2 (0,25 kg), MOTOBOY, cupom MENOS50, BOLETO
        // Esperado: subtotal 399,80 · cupom 50,00 · frete 18,00 · prazo 0 · ajuste 3,49 · total 371,29 · 1×371,29

        List<ItemCarrinho> itens = Arrays.asList(
                new ItemCarrinho("Fone", 199.90, 2, 0.25)
        );

        RequisicaoResumo requisicao = new RequisicaoResumo();
        requisicao.setItens(itens);
        requisicao.setModalidadeEntrega("MOTOBOY");
        requisicao.setCupom("MENOS50");
        requisicao.setFormaPagamento("BOLETO");
        requisicao.setParcelas(1);

        RespostaResumo resposta = checkoutService.calcularResumo(requisicao);

        assertEquals(new BigDecimal("399.80"), resposta.getSubtotalProdutos());
        assertEquals(new BigDecimal("50.00"), resposta.getDescontoCupom());
        assertEquals(new BigDecimal("18.00"), resposta.getFrete());
        assertEquals(0, resposta.getPrazoEntregaDias());
        assertEquals(new BigDecimal("3.49"), resposta.getAjustePagamento());
        assertEquals(new BigDecimal("371.29"), resposta.getTotalFinal());
        assertEquals(1, resposta.getParcelas());
        assertEquals(new BigDecimal("371.29"), resposta.getValorParcela());
    }

    @Test
    void testExemplo4() {
        // Meia 19,90 × 7 (0,10 kg) + Camiseta 79,90 × 2 (0,30 kg)
        // RETIRADA_LOJA, cupom LEVE3PAGUE2, CARTAO 3×
        // Esperado: subtotal 299,10 · cupom 39,80 · frete 0,00 · prazo 1 · ajuste 0,00 · total 259,30 · 3×86,43

        List<ItemCarrinho> itens = Arrays.asList(
                new ItemCarrinho("Meia", 19.90, 7, 0.10),
                new ItemCarrinho("Camiseta", 79.90, 2, 0.30)
        );

        RequisicaoResumo requisicao = new RequisicaoResumo();
        requisicao.setItens(itens);
        requisicao.setModalidadeEntrega("RETIRADA_LOJA");
        requisicao.setCupom("LEVE3PAGUE2");
        requisicao.setFormaPagamento("CARTAO");
        requisicao.setParcelas(3);

        RespostaResumo resposta = checkoutService.calcularResumo(requisicao);

        assertEquals(new BigDecimal("299.10"), resposta.getSubtotalProdutos());
        assertEquals(new BigDecimal("39.80"), resposta.getDescontoCupom());
        assertEquals(new BigDecimal("0.00"), resposta.getFrete());
        assertEquals(1, resposta.getPrazoEntregaDias());
        assertEquals(new BigDecimal("0.00"), resposta.getAjustePagamento());
        assertEquals(new BigDecimal("259.30"), resposta.getTotalFinal());
        assertEquals(3, resposta.getParcelas());
        assertEquals(new BigDecimal("86.43"), resposta.getValorParcela());
    }
}

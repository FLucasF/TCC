package br.tcc.checkout.service;

import br.tcc.checkout.dto.CheckoutRequisicaoDto;
import br.tcc.checkout.dto.CheckoutRespostaDto;
import br.tcc.checkout.dto.ItemDto;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

@SpringBootTest
class CheckoutServiceTest {

    @Autowired
    private CheckoutService checkoutService;

    @Test
    void exemplo1() {
        CheckoutRequisicaoDto requisicao = new CheckoutRequisicaoDto(
            List.of(
                new ItemDto("Camiseta", 79.90, 2, 0.30),
                new ItemDto("Tênis", 249.90, 1, 1.20)
            ),
            "EXPRESSA",
            "BEMVINDO10",
            "PIX",
            1
        );

        CheckoutRespostaDto resposta = checkoutService.calcularResumo(requisicao);

        assertEquals(409.70, resposta.subtotalProdutos());
        assertEquals(40.97, resposta.descontoCupom());
        assertEquals(33.10, resposta.frete());
        assertEquals(2, resposta.prazoEntregaDias());
        assertEquals(-20.09, resposta.ajustePagamento());
        assertEquals(381.74, resposta.totalFinal());
        assertEquals(1, resposta.parcelas());
        assertEquals(381.74, resposta.valorParcela());
    }

    @Test
    void exemplo2() {
        CheckoutRequisicaoDto requisicao = new CheckoutRequisicaoDto(
            List.of(
                new ItemDto("Camiseta", 79.90, 2, 0.30),
                new ItemDto("Tênis", 249.90, 1, 1.20)
            ),
            "ECONOMICA",
            null,
            "CARTAO",
            6
        );

        CheckoutRespostaDto resposta = checkoutService.calcularResumo(requisicao);

        assertEquals(409.70, resposta.subtotalProdutos());
        assertEquals(0.00, resposta.descontoCupom());
        assertEquals(15.60, resposta.frete());
        assertEquals(7, resposta.prazoEntregaDias());
        assertEquals(30.10, resposta.ajustePagamento());
        assertEquals(455.40, resposta.totalFinal());
        assertEquals(6, resposta.parcelas());
        assertEquals(75.90, resposta.valorParcela());
    }

    @Test
    void exemplo3() {
        CheckoutRequisicaoDto requisicao = new CheckoutRequisicaoDto(
            List.of(
                new ItemDto("Fone", 199.90, 2, 0.25)
            ),
            "MOTOBOY",
            "MENOS50",
            "BOLETO",
            1
        );

        CheckoutRespostaDto resposta = checkoutService.calcularResumo(requisicao);

        assertEquals(399.80, resposta.subtotalProdutos());
        assertEquals(50.00, resposta.descontoCupom());
        assertEquals(18.00, resposta.frete());
        assertEquals(0, resposta.prazoEntregaDias());
        assertEquals(3.49, resposta.ajustePagamento());
        assertEquals(371.29, resposta.totalFinal());
        assertEquals(1, resposta.parcelas());
        assertEquals(371.29, resposta.valorParcela());
    }

    @Test
    void exemplo4() {
        CheckoutRequisicaoDto requisicao = new CheckoutRequisicaoDto(
            List.of(
                new ItemDto("Meia", 19.90, 7, 0.10),
                new ItemDto("Camiseta", 79.90, 2, 0.30)
            ),
            "RETIRADA_LOJA",
            "LEVE3PAGUE2",
            "CARTAO",
            3
        );

        CheckoutRespostaDto resposta = checkoutService.calcularResumo(requisicao);

        assertEquals(299.10, resposta.subtotalProdutos());
        assertEquals(39.80, resposta.descontoCupom());
        assertEquals(0.00, resposta.frete());
        assertEquals(1, resposta.prazoEntregaDias());
        assertEquals(0.00, resposta.ajustePagamento());
        assertEquals(259.30, resposta.totalFinal());
        assertEquals(3, resposta.parcelas());
        assertEquals(86.43, resposta.valorParcela());
    }
}

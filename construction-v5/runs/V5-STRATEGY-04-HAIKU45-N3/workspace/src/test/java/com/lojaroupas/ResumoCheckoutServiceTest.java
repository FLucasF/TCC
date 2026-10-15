package com.lojaroupas;

import com.lojaroupas.dto.ItemCarrinho;
import com.lojaroupas.dto.RequisicaoCheckout;
import com.lojaroupas.dto.RespostaCheckout;
import com.lojaroupas.servico.ResumoCheckoutService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import static org.junit.jupiter.api.Assertions.*;

import java.util.ArrayList;
import java.util.List;

public class ResumoCheckoutServiceTest {

    private ResumoCheckoutService service;

    @BeforeEach
    public void setUp() {
        service = new ResumoCheckoutService();
    }

    @Test
    public void testExemplo1() {
        RequisicaoCheckout req = new RequisicaoCheckout();
        req.itens = new ArrayList<>();
        req.itens.add(new ItemCarrinho("Camiseta", 79.90, 2, 0.30));
        req.itens.add(new ItemCarrinho("Tênis", 249.90, 1, 1.20));
        req.modalidadeEntrega = "EXPRESSA";
        req.cupom = "BEMVINDO10";
        req.formaPagamento = "PIX";
        req.parcelas = 1;
        req.nivelClube = "BRONZE";
        req.regiao = "NORTE";

        RespostaCheckout resposta = service.calcular(req);

        assertEquals(409.70, resposta.subtotalProdutos, 0.01);
        assertEquals(40.97, resposta.descontoCupom, 0.01);
        assertEquals(33.10, resposta.frete, 0.01);
        assertEquals(2, resposta.prazoEntregaDias);
        assertEquals(10.24, resposta.seguro, 0.01);
        assertEquals(-20.60, resposta.ajustePagamento, 0.01);
        assertEquals(391.47, resposta.totalFinal, 0.01);
        assertEquals(1, resposta.parcelas);
        assertEquals(391.47, resposta.valorParcela, 0.01);
        assertEquals(0.0, resposta.creditoProximaCompra, 0.01);
        assertFalse(resposta.brinde);
    }

    @Test
    public void testExemplo2() {
        RequisicaoCheckout req = new RequisicaoCheckout();
        req.itens = new ArrayList<>();
        req.itens.add(new ItemCarrinho("Camiseta", 79.90, 2, 0.30));
        req.itens.add(new ItemCarrinho("Tênis", 249.90, 1, 1.20));
        req.modalidadeEntrega = "ECONOMICA";
        req.cupom = null;
        req.formaPagamento = "CARTAO";
        req.parcelas = 6;
        req.nivelClube = "PRATA";
        req.regiao = "CENTRO_OESTE";

        RespostaCheckout resposta = service.calcular(req);

        assertEquals(409.70, resposta.subtotalProdutos, 0.01);
        assertEquals(0.0, resposta.descontoCupom, 0.01);
        assertEquals(15.60, resposta.frete, 0.01);
        assertEquals(7, resposta.prazoEntregaDias);
        assertEquals(6.15, resposta.seguro, 0.01);
        assertEquals(30.55, resposta.ajustePagamento, 0.02);
        assertEquals(462.00, resposta.totalFinal, 0.01);
        assertEquals(6, resposta.parcelas);
        assertEquals(77.00, resposta.valorParcela, 0.01);
        assertEquals(8.19, resposta.creditoProximaCompra, 0.01);
        assertFalse(resposta.brinde);
    }

    @Test
    public void testExemplo3() {
        RequisicaoCheckout req = new RequisicaoCheckout();
        req.itens = new ArrayList<>();
        req.itens.add(new ItemCarrinho("Fone", 199.90, 2, 0.25));
        req.modalidadeEntrega = "MOTOBOY";
        req.cupom = "MENOS50";
        req.formaPagamento = "BOLETO";
        req.parcelas = 1;
        req.nivelClube = "BRONZE";
        req.regiao = "NORDESTE";

        RespostaCheckout resposta = service.calcular(req);

        assertEquals(399.80, resposta.subtotalProdutos, 0.01);
        assertEquals(50.0, resposta.descontoCupom, 0.01);
        assertEquals(18.0, resposta.frete, 0.01);
        assertEquals(0, resposta.prazoEntregaDias);
        assertEquals(8.0, resposta.seguro, 0.01);
        assertEquals(3.49, resposta.ajustePagamento, 0.01);
        assertEquals(379.29, resposta.totalFinal, 0.01);
        assertEquals(1, resposta.parcelas);
        assertEquals(379.29, resposta.valorParcela, 0.01);
        assertEquals(0.0, resposta.creditoProximaCompra, 0.01);
        assertFalse(resposta.brinde);
    }

    @Test
    public void testExemplo4() {
        RequisicaoCheckout req = new RequisicaoCheckout();
        req.itens = new ArrayList<>();
        req.itens.add(new ItemCarrinho("Meia", 19.90, 7, 0.10));
        req.itens.add(new ItemCarrinho("Camiseta", 79.90, 2, 0.30));
        req.modalidadeEntrega = "RETIRADA_LOJA";
        req.cupom = "LEVE3PAGUE2";
        req.formaPagamento = "CARTAO";
        req.parcelas = 3;
        req.nivelClube = "PRATA";
        req.regiao = "SUL";

        RespostaCheckout resposta = service.calcular(req);

        assertEquals(299.10, resposta.subtotalProdutos, 0.01);
        assertEquals(39.80, resposta.descontoCupom, 0.01);
        assertEquals(0.0, resposta.frete, 0.01);
        assertEquals(1, resposta.prazoEntregaDias);
        assertEquals(2.99, resposta.seguro, 0.01);
        assertEquals(0.0, resposta.ajustePagamento, 0.01);
        assertEquals(262.29, resposta.totalFinal, 0.01);
        assertEquals(3, resposta.parcelas);
        assertEquals(87.43, resposta.valorParcela, 0.01);
        assertEquals(5.98, resposta.creditoProximaCompra, 0.01);
        assertFalse(resposta.brinde);
    }

    @Test
    public void testExemplo5() {
        RequisicaoCheckout req = new RequisicaoCheckout();
        req.itens = new ArrayList<>();
        req.itens.add(new ItemCarrinho("Camiseta", 79.90, 2, 0.30));
        req.itens.add(new ItemCarrinho("Tênis", 249.90, 1, 1.20));
        req.modalidadeEntrega = "EXPRESSA";
        req.cupom = null;
        req.formaPagamento = "PIX";
        req.parcelas = 1;
        req.nivelClube = "OURO";
        req.regiao = "SUDESTE";

        RespostaCheckout resposta = service.calcular(req);

        assertEquals(409.70, resposta.subtotalProdutos, 0.01);
        assertEquals(0.0, resposta.descontoCupom, 0.01);
        assertEquals(0.0, resposta.frete, 0.01);
        assertEquals(2, resposta.prazoEntregaDias);
        assertEquals(4.10, resposta.seguro, 0.01);
        assertEquals(-20.69, resposta.ajustePagamento, 0.01);
        assertEquals(393.11, resposta.totalFinal, 0.01);
        assertEquals(1, resposta.parcelas);
        assertEquals(393.11, resposta.valorParcela, 0.01);
        assertEquals(20.48, resposta.creditoProximaCompra, 0.01);
        assertFalse(resposta.brinde);
    }

    @Test
    public void testFreteGratisCupom() {
        RequisicaoCheckout req = new RequisicaoCheckout();
        req.itens = new ArrayList<>();
        req.itens.add(new ItemCarrinho("Camiseta", 79.90, 1, 0.30));
        req.modalidadeEntrega = "EXPRESSA";
        req.cupom = "FRETEGRATIS";
        req.formaPagamento = "PIX";
        req.parcelas = 1;
        req.nivelClube = "BRONZE";
        req.regiao = "SUDESTE";

        RespostaCheckout resposta = service.calcular(req);

        assertEquals(79.90, resposta.subtotalProdutos, 0.01);
        assertEquals(0.0, resposta.descontoCupom, 0.01);
        assertEquals(0.0, resposta.frete, 0.01);
        assertEquals(2, resposta.prazoEntregaDias);
        assertEquals(0.80, resposta.seguro, 0.01);
    }

    @Test
    public void testOuroComBrinde() {
        RequisicaoCheckout req = new RequisicaoCheckout();
        req.itens = new ArrayList<>();
        req.itens.add(new ItemCarrinho("Produto Caro", 600.0, 1, 1.0));
        req.modalidadeEntrega = "RETIRADA_LOJA";
        req.cupom = null;
        req.formaPagamento = "PIX";
        req.parcelas = 1;
        req.nivelClube = "OURO";
        req.regiao = "SUDESTE";

        RespostaCheckout resposta = service.calcular(req);

        assertTrue(resposta.brinde);
        assertEquals(30.0, resposta.creditoProximaCompra, 0.01);
    }
}

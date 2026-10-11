package com.lojaroupas;

import com.lojaroupas.dto.ItemCarrinho;
import com.lojaroupas.dto.RequisicaoCheckout;
import com.lojaroupas.validador.ValidadorRequisicao;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import static org.junit.jupiter.api.Assertions.*;

import java.util.ArrayList;

public class ValidadorRequisicaoTest {

    private ValidadorRequisicao validador;

    @BeforeEach
    public void setUp() {
        validador = new ValidadorRequisicao();
    }

    @Test
    public void testPedidoValido() {
        RequisicaoCheckout req = new RequisicaoCheckout();
        req.itens = new ArrayList<>();
        req.itens.add(new ItemCarrinho("Camiseta", 79.90, 2, 0.30));
        req.modalidadeEntrega = "EXPRESSA";
        req.cupom = null;
        req.formaPagamento = "PIX";
        req.parcelas = 1;
        req.nivelClube = "BRONZE";
        req.regiao = "SUDESTE";

        String erro = validador.validar(req);
        assertNull(erro);
    }

    @Test
    public void testCarrinhoVazio() {
        RequisicaoCheckout req = new RequisicaoCheckout();
        req.itens = new ArrayList<>();
        req.modalidadeEntrega = "EXPRESSA";
        req.formaPagamento = "PIX";
        req.nivelClube = "BRONZE";
        req.regiao = "SUDESTE";

        String erro = validador.validar(req);
        assertEquals("PEDIDO_INVALIDO", erro);
    }

    @Test
    public void testItemComPrecoZero() {
        RequisicaoCheckout req = new RequisicaoCheckout();
        req.itens = new ArrayList<>();
        req.itens.add(new ItemCarrinho("Camiseta", 0.0, 2, 0.30));
        req.modalidadeEntrega = "EXPRESSA";
        req.formaPagamento = "PIX";
        req.nivelClube = "BRONZE";
        req.regiao = "SUDESTE";

        String erro = validador.validar(req);
        assertEquals("PEDIDO_INVALIDO", erro);
    }

    @Test
    public void testItemComQuantidadeZero() {
        RequisicaoCheckout req = new RequisicaoCheckout();
        req.itens = new ArrayList<>();
        req.itens.add(new ItemCarrinho("Camiseta", 79.90, 0, 0.30));
        req.modalidadeEntrega = "EXPRESSA";
        req.formaPagamento = "PIX";
        req.nivelClube = "BRONZE";
        req.regiao = "SUDESTE";

        String erro = validador.validar(req);
        assertEquals("PEDIDO_INVALIDO", erro);
    }

    @Test
    public void testNivelClubeInvalido() {
        RequisicaoCheckout req = new RequisicaoCheckout();
        req.itens = new ArrayList<>();
        req.itens.add(new ItemCarrinho("Camiseta", 79.90, 2, 0.30));
        req.modalidadeEntrega = "EXPRESSA";
        req.formaPagamento = "PIX";
        req.nivelClube = "INVALIDO";
        req.regiao = "SUDESTE";

        String erro = validador.validar(req);
        assertEquals("NIVEL_CLUBE_INVALIDO", erro);
    }

    @Test
    public void testRegiaoInvalida() {
        RequisicaoCheckout req = new RequisicaoCheckout();
        req.itens = new ArrayList<>();
        req.itens.add(new ItemCarrinho("Camiseta", 79.90, 2, 0.30));
        req.modalidadeEntrega = "EXPRESSA";
        req.formaPagamento = "PIX";
        req.nivelClube = "BRONZE";
        req.regiao = "INVALIDA";

        String erro = validador.validar(req);
        assertEquals("REGIAO_INVALIDA", erro);
    }

    @Test
    public void testModalidadeInvalida() {
        RequisicaoCheckout req = new RequisicaoCheckout();
        req.itens = new ArrayList<>();
        req.itens.add(new ItemCarrinho("Camiseta", 79.90, 2, 0.30));
        req.modalidadeEntrega = "INVALIDA";
        req.formaPagamento = "PIX";
        req.nivelClube = "BRONZE";
        req.regiao = "SUDESTE";

        String erro = validador.validar(req);
        assertEquals("MODALIDADE_INVALIDA", erro);
    }

    @Test
    public void testMotoboySobreKg() {
        RequisicaoCheckout req = new RequisicaoCheckout();
        req.itens = new ArrayList<>();
        req.itens.add(new ItemCarrinho("Produto Pesado", 100.0, 1, 6.0));
        req.modalidadeEntrega = "MOTOBOY";
        req.formaPagamento = "PIX";
        req.nivelClube = "BRONZE";
        req.regiao = "SUDESTE";

        String erro = validador.validar(req);
        assertEquals("MODALIDADE_INDISPONIVEL", erro);
    }

    @Test
    public void testCupomInvalido() {
        RequisicaoCheckout req = new RequisicaoCheckout();
        req.itens = new ArrayList<>();
        req.itens.add(new ItemCarrinho("Camiseta", 79.90, 2, 0.30));
        req.modalidadeEntrega = "EXPRESSA";
        req.cupom = "CUPOMINVALIDO";
        req.formaPagamento = "PIX";
        req.nivelClube = "BRONZE";
        req.regiao = "SUDESTE";

        String erro = validador.validar(req);
        assertEquals("CUPOM_INVALIDO", erro);
    }

    @Test
    public void testCupomMenos50Insuficiente() {
        RequisicaoCheckout req = new RequisicaoCheckout();
        req.itens = new ArrayList<>();
        req.itens.add(new ItemCarrinho("Camiseta", 79.90, 2, 0.30));
        req.modalidadeEntrega = "EXPRESSA";
        req.cupom = "MENOS50";
        req.formaPagamento = "PIX";
        req.nivelClube = "BRONZE";
        req.regiao = "SUDESTE";

        String erro = validador.validar(req);
        assertEquals("CUPOM_NAO_APLICAVEL", erro);
    }

    @Test
    public void testFormaPagamentoInvalida() {
        RequisicaoCheckout req = new RequisicaoCheckout();
        req.itens = new ArrayList<>();
        req.itens.add(new ItemCarrinho("Camiseta", 79.90, 2, 0.30));
        req.modalidadeEntrega = "EXPRESSA";
        req.formaPagamento = "INVALIDA";
        req.nivelClube = "BRONZE";
        req.regiao = "SUDESTE";

        String erro = validador.validar(req);
        assertEquals("FORMA_PAGAMENTO_INVALIDA", erro);
    }

    @Test
    public void testPixComMultiplasParcelas() {
        RequisicaoCheckout req = new RequisicaoCheckout();
        req.itens = new ArrayList<>();
        req.itens.add(new ItemCarrinho("Camiseta", 79.90, 2, 0.30));
        req.modalidadeEntrega = "EXPRESSA";
        req.formaPagamento = "PIX";
        req.parcelas = 2;
        req.nivelClube = "BRONZE";
        req.regiao = "SUDESTE";

        String erro = validador.validar(req);
        assertEquals("PARCELAMENTO_INVALIDO", erro);
    }

    @Test
    public void testCartaoComParcelasAltas() {
        RequisicaoCheckout req = new RequisicaoCheckout();
        req.itens = new ArrayList<>();
        req.itens.add(new ItemCarrinho("Camiseta", 79.90, 2, 0.30));
        req.modalidadeEntrega = "EXPRESSA";
        req.formaPagamento = "CARTAO";
        req.parcelas = 13;
        req.nivelClube = "BRONZE";
        req.regiao = "SUDESTE";

        String erro = validador.validar(req);
        assertEquals("PARCELAMENTO_INVALIDO", erro);
    }

    @Test
    public void testBoletoAcima1000() {
        RequisicaoCheckout req = new RequisicaoCheckout();
        req.itens = new ArrayList<>();
        req.itens.add(new ItemCarrinho("Produto Caro", 1500.0, 1, 1.0));
        req.modalidadeEntrega = "RETIRADA_LOJA";
        req.formaPagamento = "BOLETO";
        req.parcelas = 1;
        req.nivelClube = "BRONZE";
        req.regiao = "SUDESTE";

        String erro = validador.validar(req);
        assertEquals("FORMA_PAGAMENTO_INDISPONIVEL", erro);
    }

    @Test
    public void testBoletoOuroComFreteGratis() {
        RequisicaoCheckout req = new RequisicaoCheckout();
        req.itens = new ArrayList<>();
        req.itens.add(new ItemCarrinho("Produto", 950.0, 1, 10.0));
        req.modalidadeEntrega = "EXPRESSA";
        req.formaPagamento = "BOLETO";
        req.parcelas = 1;
        req.nivelClube = "OURO";
        req.regiao = "SUDESTE";

        String erro = validador.validar(req);
        assertNull(erro);
    }

    @Test
    public void testBoletoComCupomFreteGratis() {
        RequisicaoCheckout req = new RequisicaoCheckout();
        req.itens = new ArrayList<>();
        req.itens.add(new ItemCarrinho("Produto", 950.0, 1, 10.0));
        req.modalidadeEntrega = "EXPRESSA";
        req.cupom = "FRETEGRATIS";
        req.formaPagamento = "BOLETO";
        req.parcelas = 1;
        req.nivelClube = "BRONZE";
        req.regiao = "SUDESTE";

        String erro = validador.validar(req);
        assertNull(erro);
    }
}

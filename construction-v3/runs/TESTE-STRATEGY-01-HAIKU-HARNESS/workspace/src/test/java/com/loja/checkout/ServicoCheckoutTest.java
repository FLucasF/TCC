package com.loja.checkout;

import com.loja.checkout.dto.ItemDTO;
import com.loja.checkout.dto.RequisicaoCheckoutDTO;
import com.loja.checkout.dto.RespostaCheckoutDTO;
import com.loja.checkout.servico.ServicoCheckout;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.math.BigDecimal;
import java.util.ArrayList;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
public class ServicoCheckoutTest {

    @Autowired
    private ServicoCheckout servicoCheckout;

    @Test
    public void exemplo1() {
        RequisicaoCheckoutDTO req = new RequisicaoCheckoutDTO();
        req.itens = new ArrayList<>();
        req.itens.add(new ItemDTO("Camiseta", new BigDecimal("79.90"), 2, new BigDecimal("0.30")));
        req.itens.add(new ItemDTO("Tênis", new BigDecimal("249.90"), 1, new BigDecimal("1.20")));
        req.modalidadeEntrega = "EXPRESSA";
        req.cupom = "BEMVINDO10";
        req.formaPagamento = "PIX";
        req.parcelas = 1;
        req.nivelClube = "BRONZE";
        req.regiao = null;

        RespostaCheckoutDTO resp = servicoCheckout.calcularResumo(req);

        assertEquals(new BigDecimal("409.70"), resp.subtotalProdutos);
        assertEquals(new BigDecimal("40.97"), resp.descontoCupom);
        assertEquals(new BigDecimal("33.10"), resp.frete);
        assertEquals(2, resp.prazoEntregaDias);
        assertTrue(resp.imposto.compareTo(BigDecimal.ZERO) == 0);
        assertEquals(new BigDecimal("-20.09"), resp.ajustePagamento);
        assertEquals(new BigDecimal("381.74"), resp.totalFinal);
        assertEquals(1, resp.parcelas);
        assertEquals(new BigDecimal("381.74"), resp.valorParcela);
    }

    @Test
    public void exemplo2() {
        RequisicaoCheckoutDTO req = new RequisicaoCheckoutDTO();
        req.itens = new ArrayList<>();
        req.itens.add(new ItemDTO("Camiseta", new BigDecimal("79.90"), 2, new BigDecimal("0.30")));
        req.itens.add(new ItemDTO("Tênis", new BigDecimal("249.90"), 1, new BigDecimal("1.20")));
        req.modalidadeEntrega = "ECONOMICA";
        req.cupom = null;
        req.formaPagamento = "CARTAO";
        req.parcelas = 6;
        req.nivelClube = "BRONZE";
        req.regiao = null;

        RespostaCheckoutDTO resp = servicoCheckout.calcularResumo(req);

        assertEquals(new BigDecimal("409.70"), resp.subtotalProdutos);
        assertEquals(new BigDecimal("0.00"), resp.descontoCupom);
        assertEquals(new BigDecimal("15.60"), resp.frete);
        assertEquals(7, resp.prazoEntregaDias);
        assertTrue(resp.imposto.compareTo(BigDecimal.ZERO) == 0);
        assertEquals(new BigDecimal("30.10"), resp.ajustePagamento);
        assertEquals(new BigDecimal("455.40"), resp.totalFinal);
        assertEquals(6, resp.parcelas);
        assertEquals(new BigDecimal("75.90"), resp.valorParcela);
    }

    @Test
    public void exemplo3() {
        RequisicaoCheckoutDTO req = new RequisicaoCheckoutDTO();
        req.itens = new ArrayList<>();
        req.itens.add(new ItemDTO("Fone", new BigDecimal("199.90"), 2, new BigDecimal("0.25")));
        req.modalidadeEntrega = "MOTOBOY";
        req.cupom = "MENOS50";
        req.formaPagamento = "BOLETO";
        req.parcelas = 1;
        req.nivelClube = "BRONZE";
        req.regiao = null;

        RespostaCheckoutDTO resp = servicoCheckout.calcularResumo(req);

        assertEquals(new BigDecimal("399.80"), resp.subtotalProdutos);
        assertEquals(new BigDecimal("50.00"), resp.descontoCupom);
        assertEquals(new BigDecimal("18.00"), resp.frete);
        assertEquals(0, resp.prazoEntregaDias);
        assertTrue(resp.imposto.compareTo(BigDecimal.ZERO) == 0);
        assertEquals(new BigDecimal("3.49"), resp.ajustePagamento);
        assertEquals(new BigDecimal("371.29"), resp.totalFinal);
        assertEquals(1, resp.parcelas);
        assertEquals(new BigDecimal("371.29"), resp.valorParcela);
    }

    @Test
    public void exemplo4() {
        RequisicaoCheckoutDTO req = new RequisicaoCheckoutDTO();
        req.itens = new ArrayList<>();
        req.itens.add(new ItemDTO("Meia", new BigDecimal("19.90"), 7, new BigDecimal("0.10")));
        req.itens.add(new ItemDTO("Camiseta", new BigDecimal("79.90"), 2, new BigDecimal("0.30")));
        req.modalidadeEntrega = "RETIRADA_LOJA";
        req.cupom = "LEVE3PAGUE2";
        req.formaPagamento = "CARTAO";
        req.parcelas = 3;
        req.nivelClube = "BRONZE";
        req.regiao = null;

        RespostaCheckoutDTO resp = servicoCheckout.calcularResumo(req);

        assertEquals(new BigDecimal("299.10"), resp.subtotalProdutos);
        assertEquals(new BigDecimal("39.80"), resp.descontoCupom);
        assertEquals(new BigDecimal("0.00"), resp.frete);
        assertEquals(1, resp.prazoEntregaDias);
        assertTrue(resp.imposto.compareTo(BigDecimal.ZERO) == 0);
        assertEquals(new BigDecimal("0.00"), resp.ajustePagamento);
        assertEquals(new BigDecimal("259.30"), resp.totalFinal);
        assertEquals(3, resp.parcelas);
        assertEquals(new BigDecimal("86.43"), resp.valorParcela);
    }

    @Test
    public void exemplo5() {
        RequisicaoCheckoutDTO req = new RequisicaoCheckoutDTO();
        req.itens = new ArrayList<>();
        req.itens.add(new ItemDTO("Camiseta", new BigDecimal("79.90"), 2, new BigDecimal("0.30")));
        req.itens.add(new ItemDTO("Tênis", new BigDecimal("249.90"), 1, new BigDecimal("1.20")));
        req.modalidadeEntrega = "EXPRESSA";
        req.cupom = null;
        req.formaPagamento = "PIX";
        req.parcelas = 1;
        req.nivelClube = "OURO";
        req.regiao = "SUDESTE";

        RespostaCheckoutDTO resp = servicoCheckout.calcularResumo(req);

        assertEquals(new BigDecimal("409.70"), resp.subtotalProdutos);
        assertEquals(new BigDecimal("0.00"), resp.descontoCupom);
        assertEquals(new BigDecimal("0.00"), resp.frete);
        assertEquals(2, resp.prazoEntregaDias);
        assertEquals(new BigDecimal("49.16"), resp.imposto);
        assertEquals(new BigDecimal("-22.94"), resp.ajustePagamento);
        assertEquals(new BigDecimal("435.92"), resp.totalFinal);
        assertEquals(1, resp.parcelas);
        assertEquals(new BigDecimal("435.92"), resp.valorParcela);
        assertEquals(new BigDecimal("20.48"), resp.creditoProximaCompra);
        assertFalse(resp.brinde);
    }

    @Test
    public void validacaoPedidoInvalido() {
        RequisicaoCheckoutDTO req = new RequisicaoCheckoutDTO();
        req.itens = new ArrayList<>();
        req.modalidadeEntrega = "EXPRESSA";
        req.formaPagamento = "PIX";
        req.nivelClube = "BRONZE";
        req.regiao = "SUDESTE";

        assertThrows(IllegalArgumentException.class, () -> servicoCheckout.calcularResumo(req));
    }

    @Test
    public void validacaoCupomInvalido() {
        RequisicaoCheckoutDTO req = new RequisicaoCheckoutDTO();
        req.itens = new ArrayList<>();
        req.itens.add(new ItemDTO("Produto", new BigDecimal("100.00"), 1, new BigDecimal("1.00")));
        req.modalidadeEntrega = "EXPRESSA";
        req.cupom = "CUPOMINVALIDO";
        req.formaPagamento = "PIX";
        req.nivelClube = "BRONZE";
        req.regiao = "SUDESTE";

        assertThrows(IllegalArgumentException.class, () -> servicoCheckout.calcularResumo(req));
    }

    @Test
    public void validacaoMotoboySobrepesoIndisponivel() {
        RequisicaoCheckoutDTO req = new RequisicaoCheckoutDTO();
        req.itens = new ArrayList<>();
        req.itens.add(new ItemDTO("Produto", new BigDecimal("100.00"), 1, new BigDecimal("10.00")));
        req.modalidadeEntrega = "MOTOBOY";
        req.formaPagamento = "PIX";
        req.nivelClube = "BRONZE";
        req.regiao = "SUDESTE";

        assertThrows(IllegalArgumentException.class, () -> servicoCheckout.calcularResumo(req));
    }
}

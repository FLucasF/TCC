package br.tcc.checkout;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;
import br.tcc.checkout.domain.Item;
import br.tcc.checkout.domain.Pedido;
import br.tcc.checkout.dto.ResumoResponse;
import br.tcc.checkout.service.CalculadoraResumo;
import br.tcc.checkout.service.ResumoException;

public class CalculadoraResumoTest {
    @Test
    public void testExemplo1() throws ResumoException {
        List<Item> itens = new ArrayList<>();
        itens.add(new Item("Camiseta", new BigDecimal("79.90"), 2, 0.30));
        itens.add(new Item("Tênis", new BigDecimal("249.90"), 1, 1.20));

        Pedido pedido = new Pedido(itens, "EXPRESSA", "BEMVINDO10", "PIX", 1);
        CalculadoraResumo calculadora = new CalculadoraResumo(pedido);
        ResumoResponse resposta = calculadora.calcular();

        assertEquals(new BigDecimal("409.70"), resposta.subtotalProdutos);
        assertEquals(new BigDecimal("40.97"), resposta.descontoCupom);
        assertEquals(new BigDecimal("33.10"), resposta.frete);
        assertEquals(2, resposta.prazoEntregaDias);
        assertEquals(new BigDecimal("-20.09"), resposta.ajustePagamento);
        assertEquals(new BigDecimal("381.74"), resposta.totalFinal);
        assertEquals(1, resposta.parcelas);
        assertEquals(new BigDecimal("381.74"), resposta.valorParcela);
    }

    @Test
    public void testExemplo2() throws ResumoException {
        List<Item> itens = new ArrayList<>();
        itens.add(new Item("Camiseta", new BigDecimal("79.90"), 2, 0.30));
        itens.add(new Item("Tênis", new BigDecimal("249.90"), 1, 1.20));

        Pedido pedido = new Pedido(itens, "ECONOMICA", null, "CARTAO", 6);
        CalculadoraResumo calculadora = new CalculadoraResumo(pedido);
        ResumoResponse resposta = calculadora.calcular();

        assertEquals(new BigDecimal("409.70"), resposta.subtotalProdutos);
        assertEquals(new BigDecimal("0.00"), resposta.descontoCupom);
        assertEquals(new BigDecimal("15.60"), resposta.frete);
        assertEquals(7, resposta.prazoEntregaDias);
        assertEquals(new BigDecimal("30.10"), resposta.ajustePagamento);
        assertEquals(new BigDecimal("455.40"), resposta.totalFinal);
        assertEquals(6, resposta.parcelas);
        assertEquals(new BigDecimal("75.90"), resposta.valorParcela);
    }

    @Test
    public void testExemplo3() throws ResumoException {
        List<Item> itens = new ArrayList<>();
        itens.add(new Item("Fone", new BigDecimal("199.90"), 2, 0.25));

        Pedido pedido = new Pedido(itens, "MOTOBOY", "MENOS50", "BOLETO", 1);
        CalculadoraResumo calculadora = new CalculadoraResumo(pedido);
        ResumoResponse resposta = calculadora.calcular();

        assertEquals(new BigDecimal("399.80"), resposta.subtotalProdutos);
        assertEquals(new BigDecimal("50.00"), resposta.descontoCupom);
        assertEquals(new BigDecimal("18.00"), resposta.frete);
        assertEquals(0, resposta.prazoEntregaDias);
        assertEquals(new BigDecimal("3.49"), resposta.ajustePagamento);
        assertEquals(new BigDecimal("371.29"), resposta.totalFinal);
        assertEquals(1, resposta.parcelas);
        assertEquals(new BigDecimal("371.29"), resposta.valorParcela);
    }

    @Test
    public void testExemplo4() throws ResumoException {
        List<Item> itens = new ArrayList<>();
        itens.add(new Item("Meia", new BigDecimal("19.90"), 7, 0.10));
        itens.add(new Item("Camiseta", new BigDecimal("79.90"), 2, 0.30));

        Pedido pedido = new Pedido(itens, "RETIRADA_LOJA", "LEVE3PAGUE2", "CARTAO", 3);
        CalculadoraResumo calculadora = new CalculadoraResumo(pedido);
        ResumoResponse resposta = calculadora.calcular();

        assertEquals(new BigDecimal("299.10"), resposta.subtotalProdutos);
        assertEquals(new BigDecimal("39.80"), resposta.descontoCupom);
        assertEquals(new BigDecimal("0.00"), resposta.frete);
        assertEquals(1, resposta.prazoEntregaDias);
        assertEquals(new BigDecimal("0.00"), resposta.ajustePagamento);
        assertEquals(new BigDecimal("259.30"), resposta.totalFinal);
        assertEquals(3, resposta.parcelas);
        assertEquals(new BigDecimal("86.43"), resposta.valorParcela);
    }
}

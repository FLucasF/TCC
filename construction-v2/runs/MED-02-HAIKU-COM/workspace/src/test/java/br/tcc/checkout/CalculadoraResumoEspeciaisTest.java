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

public class CalculadoraResumoEspeciaisTest {
    @Test
    public void testCupomFreteGratis() throws ResumoException {
        List<Item> itens = new ArrayList<>();
        itens.add(new Item("Camiseta", new BigDecimal("79.90"), 2, 0.30));
        itens.add(new Item("Tênis", new BigDecimal("249.90"), 1, 1.20));

        Pedido pedido = new Pedido(itens, "EXPRESSA", "FRETEGRATIS", "CARTAO", 1);
        CalculadoraResumo calculadora = new CalculadoraResumo(pedido);
        ResumoResponse resposta = calculadora.calcular();

        assertEquals(new BigDecimal("409.70"), resposta.subtotalProdutos);
        assertEquals(new BigDecimal("33.10"), resposta.descontoCupom);
        assertEquals(new BigDecimal("33.10"), resposta.frete);
        assertEquals(2, resposta.prazoEntregaDias);
        assertEquals(new BigDecimal("0.00"), resposta.ajustePagamento);
        assertEquals(new BigDecimal("409.70"), resposta.totalFinal);
        assertEquals(1, resposta.parcelas);
        assertEquals(new BigDecimal("409.70"), resposta.valorParcela);
    }

    @Test
    public void testLeve3Pague2ComVariosItens() throws ResumoException {
        List<Item> itens = new ArrayList<>();
        itens.add(new Item("Produto A", new BigDecimal("10.00"), 5, 0.10));
        itens.add(new Item("Produto B", new BigDecimal("20.00"), 4, 0.10));

        Pedido pedido = new Pedido(itens, "RETIRADA_LOJA", "LEVE3PAGUE2", "PIX", 1);
        CalculadoraResumo calculadora = new CalculadoraResumo(pedido);
        ResumoResponse resposta = calculadora.calcular();

        assertEquals(new BigDecimal("130.00"), resposta.subtotalProdutos);

        BigDecimal descontoA = new BigDecimal("10.00")
                .multiply(new BigDecimal("1"));
        BigDecimal descontoB = new BigDecimal("20.00")
                .multiply(new BigDecimal("1"));
        BigDecimal descontoEsperado = descontoA.add(descontoB);

        assertEquals(descontoEsperado, resposta.descontoCupom);
        assertEquals(new BigDecimal("0.00"), resposta.frete);
    }

    @Test
    public void testRetiradaLoja() throws ResumoException {
        List<Item> itens = new ArrayList<>();
        itens.add(new Item("Produto", new BigDecimal("100.00"), 1, 5.0));

        Pedido pedido = new Pedido(itens, "RETIRADA_LOJA", null, "PIX", 1);
        CalculadoraResumo calculadora = new CalculadoraResumo(pedido);
        ResumoResponse resposta = calculadora.calcular();

        assertEquals(new BigDecimal("100.00"), resposta.subtotalProdutos);
        assertEquals(new BigDecimal("0.00"), resposta.descontoCupom);
        assertEquals(new BigDecimal("0.00"), resposta.frete);
        assertEquals(1, resposta.prazoEntregaDias);
    }

    @Test
    public void testEconomicaComPeso() throws ResumoException {
        List<Item> itens = new ArrayList<>();
        itens.add(new Item("Produto", new BigDecimal("100.00"), 5, 1.5));

        Pedido pedido = new Pedido(itens, "ECONOMICA", null, "PIX", 1);
        CalculadoraResumo calculadora = new CalculadoraResumo(pedido);
        ResumoResponse resposta = calculadora.calcular();

        assertEquals(new BigDecimal("27.00"), resposta.frete);
        assertEquals(7, resposta.prazoEntregaDias);
    }

    @Test
    public void testCartaoSemJurosAte3Parcelas() throws ResumoException {
        List<Item> itens = new ArrayList<>();
        itens.add(new Item("Produto", new BigDecimal("300.00"), 1, 1.0));

        Pedido pedido = new Pedido(itens, "RETIRADA_LOJA", null, "CARTAO", 3);
        CalculadoraResumo calculadora = new CalculadoraResumo(pedido);
        ResumoResponse resposta = calculadora.calcular();

        assertEquals(new BigDecimal("300.00"), resposta.totalFinal);
        assertEquals(new BigDecimal("100.00"), resposta.valorParcela);
        assertEquals(3, resposta.parcelas);
    }
}

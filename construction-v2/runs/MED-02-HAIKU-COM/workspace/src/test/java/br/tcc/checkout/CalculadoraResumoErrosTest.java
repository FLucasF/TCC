package br.tcc.checkout;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;
import br.tcc.checkout.domain.Item;
import br.tcc.checkout.domain.Pedido;
import br.tcc.checkout.service.CalculadoraResumo;
import br.tcc.checkout.service.ResumoException;

public class CalculadoraResumoErrosTest {
    @Test
    public void testPedidoVazio() {
        List<Item> itens = new ArrayList<>();
        Pedido pedido = new Pedido(itens, "EXPRESSA", null, "PIX", 1);
        CalculadoraResumo calculadora = new CalculadoraResumo(pedido);

        ResumoException ex = assertThrows(ResumoException.class, () -> calculadora.calcular());
        assertEquals("PEDIDO_INVALIDO", ex.getCodigo());
    }

    @Test
    public void testPrecoCeroOuNegativo() {
        List<Item> itens = new ArrayList<>();
        itens.add(new Item("Camiseta", new BigDecimal("0"), 1, 0.30));
        Pedido pedido = new Pedido(itens, "EXPRESSA", null, "PIX", 1);
        CalculadoraResumo calculadora = new CalculadoraResumo(pedido);

        ResumoException ex = assertThrows(ResumoException.class, () -> calculadora.calcular());
        assertEquals("PEDIDO_INVALIDO", ex.getCodigo());
    }

    @Test
    public void testQuantidadeCeroOuNegativa() {
        List<Item> itens = new ArrayList<>();
        itens.add(new Item("Camiseta", new BigDecimal("79.90"), 0, 0.30));
        Pedido pedido = new Pedido(itens, "EXPRESSA", null, "PIX", 1);
        CalculadoraResumo calculadora = new CalculadoraResumo(pedido);

        ResumoException ex = assertThrows(ResumoException.class, () -> calculadora.calcular());
        assertEquals("PEDIDO_INVALIDO", ex.getCodigo());
    }

    @Test
    public void testPesoNegativo() {
        List<Item> itens = new ArrayList<>();
        itens.add(new Item("Camiseta", new BigDecimal("79.90"), 1, -0.30));
        Pedido pedido = new Pedido(itens, "EXPRESSA", null, "PIX", 1);
        CalculadoraResumo calculadora = new CalculadoraResumo(pedido);

        ResumoException ex = assertThrows(ResumoException.class, () -> calculadora.calcular());
        assertEquals("PEDIDO_INVALIDO", ex.getCodigo());
    }

    @Test
    public void testModalidadeInvalida() {
        List<Item> itens = new ArrayList<>();
        itens.add(new Item("Camiseta", new BigDecimal("79.90"), 1, 0.30));
        Pedido pedido = new Pedido(itens, "INVALIDA", null, "PIX", 1);
        CalculadoraResumo calculadora = new CalculadoraResumo(pedido);

        ResumoException ex = assertThrows(ResumoException.class, () -> calculadora.calcular());
        assertEquals("MODALIDADE_INVALIDA", ex.getCodigo());
    }

    @Test
    public void testModalidadeIndisponivel() {
        List<Item> itens = new ArrayList<>();
        itens.add(new Item("Camiseta", new BigDecimal("79.90"), 100, 0.30));
        Pedido pedido = new Pedido(itens, "MOTOBOY", null, "PIX", 1);
        CalculadoraResumo calculadora = new CalculadoraResumo(pedido);

        ResumoException ex = assertThrows(ResumoException.class, () -> calculadora.calcular());
        assertEquals("MODALIDADE_INDISPONIVEL", ex.getCodigo());
    }

    @Test
    public void testCupomInvalido() {
        List<Item> itens = new ArrayList<>();
        itens.add(new Item("Camiseta", new BigDecimal("79.90"), 1, 0.30));
        Pedido pedido = new Pedido(itens, "EXPRESSA", "INVALIDO", "PIX", 1);
        CalculadoraResumo calculadora = new CalculadoraResumo(pedido);

        ResumoException ex = assertThrows(ResumoException.class, () -> calculadora.calcular());
        assertEquals("CUPOM_INVALIDO", ex.getCodigo());
    }

    @Test
    public void testCupomNaoAplicavel() {
        List<Item> itens = new ArrayList<>();
        itens.add(new Item("Camiseta", new BigDecimal("79.90"), 1, 0.30));
        Pedido pedido = new Pedido(itens, "EXPRESSA", "MENOS50", "PIX", 1);
        CalculadoraResumo calculadora = new CalculadoraResumo(pedido);

        ResumoException ex = assertThrows(ResumoException.class, () -> calculadora.calcular());
        assertEquals("CUPOM_NAO_APLICAVEL", ex.getCodigo());
    }

    @Test
    public void testFormaPagamentoInvalida() {
        List<Item> itens = new ArrayList<>();
        itens.add(new Item("Camiseta", new BigDecimal("79.90"), 1, 0.30));
        Pedido pedido = new Pedido(itens, "EXPRESSA", null, "INVALIDA", 1);
        CalculadoraResumo calculadora = new CalculadoraResumo(pedido);

        ResumoException ex = assertThrows(ResumoException.class, () -> calculadora.calcular());
        assertEquals("FORMA_PAGAMENTO_INVALIDA", ex.getCodigo());
    }

    @Test
    public void testParcelamentoInvalidoPixMaiorQue1() {
        List<Item> itens = new ArrayList<>();
        itens.add(new Item("Camiseta", new BigDecimal("79.90"), 1, 0.30));
        Pedido pedido = new Pedido(itens, "EXPRESSA", null, "PIX", 2);
        CalculadoraResumo calculadora = new CalculadoraResumo(pedido);

        ResumoException ex = assertThrows(ResumoException.class, () -> calculadora.calcular());
        assertEquals("PARCELAMENTO_INVALIDO", ex.getCodigo());
    }

    @Test
    public void testParcelamentoInvalidoCartaoMaiorQue12() {
        List<Item> itens = new ArrayList<>();
        itens.add(new Item("Camiseta", new BigDecimal("79.90"), 1, 0.30));
        Pedido pedido = new Pedido(itens, "EXPRESSA", null, "CARTAO", 13);
        CalculadoraResumo calculadora = new CalculadoraResumo(pedido);

        ResumoException ex = assertThrows(ResumoException.class, () -> calculadora.calcular());
        assertEquals("PARCELAMENTO_INVALIDO", ex.getCodigo());
    }

    @Test
    public void testBoletoAcumaDoLimite() {
        List<Item> itens = new ArrayList<>();
        itens.add(new Item("Item", new BigDecimal("1000.00"), 2, 0.10));
        Pedido pedido = new Pedido(itens, "RETIRADA_LOJA", null, "BOLETO", 1);
        CalculadoraResumo calculadora = new CalculadoraResumo(pedido);

        ResumoException ex = assertThrows(ResumoException.class, () -> calculadora.calcular());
        assertEquals("FORMA_PAGAMENTO_INDISPONIVEL", ex.getCodigo());
    }
}

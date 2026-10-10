package com.loja.checkout.validation;

import com.loja.checkout.dto.PedidoRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class ValidadorPedidoTest {

    private ValidadorPedido validador;

    @BeforeEach
    void setUp() {
        validador = new ValidadorPedido();
    }

    @Test
    void pedidoValido() {
        List<PedidoRequest.Item> itens = Arrays.asList(
            new PedidoRequest.Item("Camiseta", 79.90, 2, 0.30)
        );
        PedidoRequest request = new PedidoRequest(itens, "EXPRESSA", null, "PIX", 1, "BRONZE", "SUDESTE");
        assertNull(validador.validar(request));
    }

    @Test
    void pedidoInvalido_carrinhVazio() {
        PedidoRequest request = new PedidoRequest(Arrays.asList(), "EXPRESSA", null, "PIX", 1, "BRONZE", "SUDESTE");
        assertEquals("PEDIDO_INVALIDO", validador.validar(request));
    }

    @Test
    void pedidoInvalido_precoZero() {
        List<PedidoRequest.Item> itens = Arrays.asList(
            new PedidoRequest.Item("Camiseta", 0, 2, 0.30)
        );
        PedidoRequest request = new PedidoRequest(itens, "EXPRESSA", null, "PIX", 1, "BRONZE", "SUDESTE");
        assertEquals("PEDIDO_INVALIDO", validador.validar(request));
    }

    @Test
    void pedidoInvalido_quantidadeZero() {
        List<PedidoRequest.Item> itens = Arrays.asList(
            new PedidoRequest.Item("Camiseta", 79.90, 0, 0.30)
        );
        PedidoRequest request = new PedidoRequest(itens, "EXPRESSA", null, "PIX", 1, "BRONZE", "SUDESTE");
        assertEquals("PEDIDO_INVALIDO", validador.validar(request));
    }

    @Test
    void nivelClubeInvalido() {
        List<PedidoRequest.Item> itens = Arrays.asList(
            new PedidoRequest.Item("Camiseta", 79.90, 2, 0.30)
        );
        PedidoRequest request = new PedidoRequest(itens, "EXPRESSA", null, "PIX", 1, "INVALIDO", "SUDESTE");
        assertEquals("NIVEL_CLUBE_INVALIDO", validador.validar(request));
    }

    @Test
    void regiaoInvalida() {
        List<PedidoRequest.Item> itens = Arrays.asList(
            new PedidoRequest.Item("Camiseta", 79.90, 2, 0.30)
        );
        PedidoRequest request = new PedidoRequest(itens, "EXPRESSA", null, "PIX", 1, "BRONZE", "INVALIDA");
        assertEquals("REGIAO_INVALIDA", validador.validar(request));
    }

    @Test
    void modalidadeInvalida() {
        List<PedidoRequest.Item> itens = Arrays.asList(
            new PedidoRequest.Item("Camiseta", 79.90, 2, 0.30)
        );
        PedidoRequest request = new PedidoRequest(itens, "INVALIDA", null, "PIX", 1, "BRONZE", "SUDESTE");
        assertEquals("MODALIDADE_INVALIDA", validador.validar(request));
    }

    @Test
    void modalidadeIndisponivel_motoboy_pesoAlto() {
        List<PedidoRequest.Item> itens = Arrays.asList(
            new PedidoRequest.Item("Produto", 100.00, 1, 6.0)
        );
        PedidoRequest request = new PedidoRequest(itens, "MOTOBOY", null, "PIX", 1, "BRONZE", "SUDESTE");
        assertEquals("MODALIDADE_INDISPONIVEL", validador.validar(request));
    }

    @Test
    void cupomInvalido() {
        List<PedidoRequest.Item> itens = Arrays.asList(
            new PedidoRequest.Item("Camiseta", 79.90, 2, 0.30)
        );
        PedidoRequest request = new PedidoRequest(itens, "EXPRESSA", "CUPOMINVALIDO", "PIX", 1, "BRONZE", "SUDESTE");
        assertEquals("CUPOM_INVALIDO", validador.validar(request));
    }

    @Test
    void cupomNaoAplicavel_menos50_abaixoDeTreecentos() {
        List<PedidoRequest.Item> itens = Arrays.asList(
            new PedidoRequest.Item("Camiseta", 79.90, 2, 0.30)
        );
        PedidoRequest request = new PedidoRequest(itens, "EXPRESSA", "MENOS50", "PIX", 1, "BRONZE", "SUDESTE");
        assertEquals("CUPOM_NAO_APLICAVEL", validador.validar(request));
    }

    @Test
    void formaPagamentoInvalida() {
        List<PedidoRequest.Item> itens = Arrays.asList(
            new PedidoRequest.Item("Camiseta", 79.90, 2, 0.30)
        );
        PedidoRequest request = new PedidoRequest(itens, "EXPRESSA", null, "INVALIDA", 1, "BRONZE", "SUDESTE");
        assertEquals("FORMA_PAGAMENTO_INVALIDA", validador.validar(request));
    }

    @Test
    void parcelamentoInvalido_pixAcimaDe1() {
        List<PedidoRequest.Item> itens = Arrays.asList(
            new PedidoRequest.Item("Camiseta", 79.90, 2, 0.30)
        );
        PedidoRequest request = new PedidoRequest(itens, "EXPRESSA", null, "PIX", 2, "BRONZE", "SUDESTE");
        assertEquals("PARCELAMENTO_INVALIDO", validador.validar(request));
    }

    @Test
    void formaPagamentoIndisponivel_boletoAcimaDeMil() {
        List<PedidoRequest.Item> itens = Arrays.asList(
            new PedidoRequest.Item("Produto", 500.50, 3, 1.0)
        );
        PedidoRequest request = new PedidoRequest(itens, "EXPRESSA", null, "BOLETO", 1, "BRONZE", "SUDESTE");
        assertEquals("FORMA_PAGAMENTO_INDISPONIVEL", validador.validar(request));
    }
}

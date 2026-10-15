package com.loja;

import com.loja.dto.ItemRequest;
import com.loja.dto.PedidoRequest;
import com.loja.exception.PedidoException;
import com.loja.service.CalculadoraResumoService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

@SpringBootTest
class ValidacoesTest {

    @Autowired
    private CalculadoraResumoService calculadora;

    @Test
    void deveLancarErroPedidoInvalidoCarrinhoVazio() {
        PedidoRequest request = new PedidoRequest(
            List.of(),
            "ECONOMICA",
            null,
            "PIX",
            null,
            "BRONZE",
            "SUDESTE"
        );

        PedidoException exception = assertThrows(PedidoException.class,
            () -> calculadora.calcular(request));
        assertEquals("PEDIDO_INVALIDO", exception.getCodigo());
    }

    @Test
    void deveLancarErroNivelClubeInvalido() {
        PedidoRequest request = new PedidoRequest(
            List.of(new ItemRequest("Produto", new BigDecimal("100.00"), 1, new BigDecimal("0.5"))),
            "ECONOMICA",
            null,
            "PIX",
            null,
            "DIAMANTE",
            "SUDESTE"
        );

        PedidoException exception = assertThrows(PedidoException.class,
            () -> calculadora.calcular(request));
        assertEquals("NIVEL_CLUBE_INVALIDO", exception.getCodigo());
    }

    @Test
    void deveLancarErroRegiaoInvalida() {
        PedidoRequest request = new PedidoRequest(
            List.of(new ItemRequest("Produto", new BigDecimal("100.00"), 1, new BigDecimal("0.5"))),
            "ECONOMICA",
            null,
            "PIX",
            null,
            "BRONZE",
            "AMAZONIA"
        );

        PedidoException exception = assertThrows(PedidoException.class,
            () -> calculadora.calcular(request));
        assertEquals("REGIAO_INVALIDA", exception.getCodigo());
    }

    @Test
    void deveLancarErroModalidadeInvalida() {
        PedidoRequest request = new PedidoRequest(
            List.of(new ItemRequest("Produto", new BigDecimal("100.00"), 1, new BigDecimal("0.5"))),
            "DRONE",
            null,
            "PIX",
            null,
            "BRONZE",
            "SUDESTE"
        );

        PedidoException exception = assertThrows(PedidoException.class,
            () -> calculadora.calcular(request));
        assertEquals("MODALIDADE_INVALIDA", exception.getCodigo());
    }

    @Test
    void deveLancarErroModalidadeIndisponivelMotoboyAcima5Kg() {
        PedidoRequest request = new PedidoRequest(
            List.of(new ItemRequest("Produto", new BigDecimal("100.00"), 1, new BigDecimal("6.0"))),
            "MOTOBOY",
            null,
            "PIX",
            null,
            "BRONZE",
            "SUDESTE"
        );

        PedidoException exception = assertThrows(PedidoException.class,
            () -> calculadora.calcular(request));
        assertEquals("MODALIDADE_INDISPONIVEL", exception.getCodigo());
    }

    @Test
    void deveLancarErroCupomInvalido() {
        PedidoRequest request = new PedidoRequest(
            List.of(new ItemRequest("Produto", new BigDecimal("100.00"), 1, new BigDecimal("0.5"))),
            "ECONOMICA",
            "CUPOMINEXISTENTE",
            "PIX",
            null,
            "BRONZE",
            "SUDESTE"
        );

        PedidoException exception = assertThrows(PedidoException.class,
            () -> calculadora.calcular(request));
        assertEquals("CUPOM_INVALIDO", exception.getCodigo());
    }

    @Test
    void deveLancarErroCupomNaoAplicavelMenos50Abaixo300() {
        PedidoRequest request = new PedidoRequest(
            List.of(new ItemRequest("Produto", new BigDecimal("100.00"), 1, new BigDecimal("0.5"))),
            "ECONOMICA",
            "MENOS50",
            "PIX",
            null,
            "BRONZE",
            "SUDESTE"
        );

        PedidoException exception = assertThrows(PedidoException.class,
            () -> calculadora.calcular(request));
        assertEquals("CUPOM_NAO_APLICAVEL", exception.getCodigo());
    }

    @Test
    void deveLancarErroFormaPagamentoInvalida() {
        PedidoRequest request = new PedidoRequest(
            List.of(new ItemRequest("Produto", new BigDecimal("100.00"), 1, new BigDecimal("0.5"))),
            "ECONOMICA",
            null,
            "DINHEIRO",
            null,
            "BRONZE",
            "SUDESTE"
        );

        PedidoException exception = assertThrows(PedidoException.class,
            () -> calculadora.calcular(request));
        assertEquals("FORMA_PAGAMENTO_INVALIDA", exception.getCodigo());
    }

    @Test
    void deveLancarErroParcelamentoInvalidoPixMaisDe1Parcela() {
        PedidoRequest request = new PedidoRequest(
            List.of(new ItemRequest("Produto", new BigDecimal("100.00"), 1, new BigDecimal("0.5"))),
            "ECONOMICA",
            null,
            "PIX",
            2,
            "BRONZE",
            "SUDESTE"
        );

        PedidoException exception = assertThrows(PedidoException.class,
            () -> calculadora.calcular(request));
        assertEquals("PARCELAMENTO_INVALIDO", exception.getCodigo());
    }

    @Test
    void deveLancarErroFormaPagamentoIndisponivelBoletoAcima1000() {
        PedidoRequest request = new PedidoRequest(
            List.of(new ItemRequest("Produto", new BigDecimal("1100.00"), 1, new BigDecimal("0.5"))),
            "ECONOMICA",
            null,
            "BOLETO",
            null,
            "BRONZE",
            "SUDESTE"
        );

        PedidoException exception = assertThrows(PedidoException.class,
            () -> calculadora.calcular(request));
        assertEquals("FORMA_PAGAMENTO_INDISPONIVEL", exception.getCodigo());
    }
}

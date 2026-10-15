package br.tcc.checkout.service;

import static org.junit.jupiter.api.Assertions.*;

import br.tcc.checkout.api.dto.ItemDto;
import br.tcc.checkout.api.dto.ResumoCheckoutRequest;
import java.util.List;
import org.junit.jupiter.api.Test;

class ResumoCheckoutServiceErrosTest {
    private final ResumoCheckoutService service = new ResumoCheckoutService();

    @Test
    void pedidoInvalido_CarrinhoVazio() {
        ResumoCheckoutRequest request = new ResumoCheckoutRequest(
            List.of(),
            "EXPRESSA",
            null,
            "PIX",
            1
        );

        ResumoCheckoutService.ErroValidacao erro = assertThrows(
            ResumoCheckoutService.ErroValidacao.class,
            () -> service.calcularResumo(request)
        );
        assertEquals("PEDIDO_INVALIDO", erro.getCodigo());
    }

    @Test
    void pedidoInvalido_ItemComPrecoZero() {
        ResumoCheckoutRequest request = new ResumoCheckoutRequest(
            List.of(new ItemDto("Camiseta", 0, 2, 0.30)),
            "EXPRESSA",
            null,
            "PIX",
            1
        );

        ResumoCheckoutService.ErroValidacao erro = assertThrows(
            ResumoCheckoutService.ErroValidacao.class,
            () -> service.calcularResumo(request)
        );
        assertEquals("PEDIDO_INVALIDO", erro.getCodigo());
    }

    @Test
    void pedidoInvalido_ItemComQuantidadeZero() {
        ResumoCheckoutRequest request = new ResumoCheckoutRequest(
            List.of(new ItemDto("Camiseta", 79.90, 0, 0.30)),
            "EXPRESSA",
            null,
            "PIX",
            1
        );

        ResumoCheckoutService.ErroValidacao erro = assertThrows(
            ResumoCheckoutService.ErroValidacao.class,
            () -> service.calcularResumo(request)
        );
        assertEquals("PEDIDO_INVALIDO", erro.getCodigo());
    }

    @Test
    void pedidoInvalido_ItemComPesoZero() {
        ResumoCheckoutRequest request = new ResumoCheckoutRequest(
            List.of(new ItemDto("Camiseta", 79.90, 2, 0)),
            "EXPRESSA",
            null,
            "PIX",
            1
        );

        ResumoCheckoutService.ErroValidacao erro = assertThrows(
            ResumoCheckoutService.ErroValidacao.class,
            () -> service.calcularResumo(request)
        );
        assertEquals("PEDIDO_INVALIDO", erro.getCodigo());
    }

    @Test
    void modalidadeInvalida_NaoExiste() {
        ResumoCheckoutRequest request = new ResumoCheckoutRequest(
            List.of(new ItemDto("Camiseta", 79.90, 2, 0.30)),
            "SUPER_MEGA_ENTREGA",
            null,
            "PIX",
            1
        );

        ResumoCheckoutService.ErroValidacao erro = assertThrows(
            ResumoCheckoutService.ErroValidacao.class,
            () -> service.calcularResumo(request)
        );
        assertEquals("MODALIDADE_INVALIDA", erro.getCodigo());
    }

    @Test
    void modalidadeInvalida_NaoInformada() {
        ResumoCheckoutRequest request = new ResumoCheckoutRequest(
            List.of(new ItemDto("Camiseta", 79.90, 2, 0.30)),
            null,
            null,
            "PIX",
            1
        );

        ResumoCheckoutService.ErroValidacao erro = assertThrows(
            ResumoCheckoutService.ErroValidacao.class,
            () -> service.calcularResumo(request)
        );
        assertEquals("MODALIDADE_INVALIDA", erro.getCodigo());
    }

    @Test
    void modalidadeIndisponivel_MotoboySobrepeso() {
        ResumoCheckoutRequest request = new ResumoCheckoutRequest(
            List.of(new ItemDto("Fone", 199.90, 2, 3.0)),
            "MOTOBOY",
            null,
            "PIX",
            1
        );

        ResumoCheckoutService.ErroValidacao erro = assertThrows(
            ResumoCheckoutService.ErroValidacao.class,
            () -> service.calcularResumo(request)
        );
        assertEquals("MODALIDADE_INDISPONIVEL", erro.getCodigo());
    }

    @Test
    void cupomInvalido_NaoExiste() {
        ResumoCheckoutRequest request = new ResumoCheckoutRequest(
            List.of(new ItemDto("Camiseta", 79.90, 2, 0.30)),
            "EXPRESSA",
            "CUPOM_FALSO",
            "PIX",
            1
        );

        ResumoCheckoutService.ErroValidacao erro = assertThrows(
            ResumoCheckoutService.ErroValidacao.class,
            () -> service.calcularResumo(request)
        );
        assertEquals("CUPOM_INVALIDO", erro.getCodigo());
    }

    @Test
    void cupomNaoAplicavel_Menos50AbaixoDe300() {
        ResumoCheckoutRequest request = new ResumoCheckoutRequest(
            List.of(new ItemDto("Camiseta", 79.90, 2, 0.30)),
            "EXPRESSA",
            "MENOS50",
            "PIX",
            1
        );

        ResumoCheckoutService.ErroValidacao erro = assertThrows(
            ResumoCheckoutService.ErroValidacao.class,
            () -> service.calcularResumo(request)
        );
        assertEquals("CUPOM_NAO_APLICAVEL", erro.getCodigo());
    }

    @Test
    void formaPagamentoInvalida_NaoExiste() {
        ResumoCheckoutRequest request = new ResumoCheckoutRequest(
            List.of(new ItemDto("Camiseta", 79.90, 2, 0.30)),
            "EXPRESSA",
            null,
            "BITCOIN",
            1
        );

        ResumoCheckoutService.ErroValidacao erro = assertThrows(
            ResumoCheckoutService.ErroValidacao.class,
            () -> service.calcularResumo(request)
        );
        assertEquals("FORMA_PAGAMENTO_INVALIDA", erro.getCodigo());
    }

    @Test
    void formaPagamentoInvalida_NaoInformada() {
        ResumoCheckoutRequest request = new ResumoCheckoutRequest(
            List.of(new ItemDto("Camiseta", 79.90, 2, 0.30)),
            "EXPRESSA",
            null,
            null,
            1
        );

        ResumoCheckoutService.ErroValidacao erro = assertThrows(
            ResumoCheckoutService.ErroValidacao.class,
            () -> service.calcularResumo(request)
        );
        assertEquals("FORMA_PAGAMENTO_INVALIDA", erro.getCodigo());
    }

    @Test
    void parcelamentoInvalido_PixComMultiplasParcelas() {
        ResumoCheckoutRequest request = new ResumoCheckoutRequest(
            List.of(new ItemDto("Camiseta", 79.90, 2, 0.30)),
            "EXPRESSA",
            null,
            "PIX",
            2
        );

        ResumoCheckoutService.ErroValidacao erro = assertThrows(
            ResumoCheckoutService.ErroValidacao.class,
            () -> service.calcularResumo(request)
        );
        assertEquals("PARCELAMENTO_INVALIDO", erro.getCodigo());
    }

    @Test
    void parcelamentoInvalido_BoletoComMultiplasParcelas() {
        ResumoCheckoutRequest request = new ResumoCheckoutRequest(
            List.of(new ItemDto("Camiseta", 79.90, 2, 0.30)),
            "EXPRESSA",
            null,
            "BOLETO",
            2
        );

        ResumoCheckoutService.ErroValidacao erro = assertThrows(
            ResumoCheckoutService.ErroValidacao.class,
            () -> service.calcularResumo(request)
        );
        assertEquals("PARCELAMENTO_INVALIDO", erro.getCodigo());
    }

    @Test
    void parcelamentoInvalido_CartaoMaisDe12x() {
        ResumoCheckoutRequest request = new ResumoCheckoutRequest(
            List.of(new ItemDto("Camiseta", 79.90, 2, 0.30)),
            "EXPRESSA",
            null,
            "CARTAO",
            13
        );

        ResumoCheckoutService.ErroValidacao erro = assertThrows(
            ResumoCheckoutService.ErroValidacao.class,
            () -> service.calcularResumo(request)
        );
        assertEquals("PARCELAMENTO_INVALIDO", erro.getCodigo());
    }

    @Test
    void formaPagamentoIndisponivel_BoletoAcimaDeKMil() {
        ResumoCheckoutRequest request = new ResumoCheckoutRequest(
            List.of(new ItemDto("Monitor", 1500.00, 1, 5.0)),
            "RETIRADA_LOJA",
            null,
            "BOLETO",
            1
        );

        ResumoCheckoutService.ErroValidacao erro = assertThrows(
            ResumoCheckoutService.ErroValidacao.class,
            () -> service.calcularResumo(request)
        );
        assertEquals("FORMA_PAGAMENTO_INDISPONIVEL", erro.getCodigo());
    }
}

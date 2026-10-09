package com.loja.online.checkout.service;

import com.loja.online.checkout.dto.ItemCarrinhoDTO;
import com.loja.online.checkout.dto.ResumoCheckoutRequestDTO;
import com.loja.online.checkout.dto.ResumoCheckoutResponseDTO;
import com.loja.online.checkout.enums.FormaPagamento;
import com.loja.online.checkout.enums.ModalidadeEntrega;
import com.loja.online.checkout.enums.NivelClube;
import com.loja.online.checkout.enums.Regiao;
import com.loja.online.checkout.exception.CheckoutException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class ResumoCheckoutServiceTest {

    private ResumoCheckoutService service;

    @BeforeEach
    void setUp() {
        service = new ResumoCheckoutService();
    }

    @Test
    void exemplo1_CamisetaTenisExpressaPixBronzeNorte() {
        List<ItemCarrinhoDTO> itens = List.of(
            new ItemCarrinhoDTO("Camiseta", new BigDecimal("79.90"), 2, new BigDecimal("0.30")),
            new ItemCarrinhoDTO("Tênis", new BigDecimal("249.90"), 1, new BigDecimal("1.20"))
        );

        ResumoCheckoutRequestDTO request = new ResumoCheckoutRequestDTO(
            itens,
            ModalidadeEntrega.EXPRESSA,
            "BEMVINDO10",
            FormaPagamento.PIX,
            1,
            NivelClube.BRONZE,
            Regiao.NORTE
        );

        ResumoCheckoutResponseDTO resposta = service.calcularResumo(request);

        assertEquals(new BigDecimal("409.70"), resposta.subtotalProdutos());
        assertEquals(new BigDecimal("40.97"), resposta.descontoCupom());
        assertEquals(new BigDecimal("33.10"), resposta.frete());
        assertEquals(2, resposta.prazoEntregaDias());
        assertEquals(new BigDecimal("10.24"), resposta.seguro());
        assertEquals(new BigDecimal("-20.60"), resposta.ajustePagamento());
        assertEquals(new BigDecimal("391.47"), resposta.totalFinal());
        assertEquals(1, resposta.parcelas());
        assertEquals(new BigDecimal("391.47"), resposta.valorParcela());
        assertEquals(new BigDecimal("0.00"), resposta.creditoProximaCompra());
        assertFalse(resposta.brinde());
    }

    @Test
    void exemplo2_CamisetaTenisEconomicaCartao6xPratacentroOeste() {
        List<ItemCarrinhoDTO> itens = List.of(
            new ItemCarrinhoDTO("Camiseta", new BigDecimal("79.90"), 2, new BigDecimal("0.30")),
            new ItemCarrinhoDTO("Tênis", new BigDecimal("249.90"), 1, new BigDecimal("1.20"))
        );

        ResumoCheckoutRequestDTO request = new ResumoCheckoutRequestDTO(
            itens,
            ModalidadeEntrega.ECONOMICA,
            null,
            FormaPagamento.CARTAO,
            6,
            NivelClube.PRATA,
            Regiao.CENTRO_OESTE
        );

        ResumoCheckoutResponseDTO resposta = service.calcularResumo(request);

        assertEquals(new BigDecimal("409.70"), resposta.subtotalProdutos());
        assertEquals(new BigDecimal("0.00"), resposta.descontoCupom());
        assertEquals(new BigDecimal("15.60"), resposta.frete());
        assertEquals(7, resposta.prazoEntregaDias());
        assertEquals(new BigDecimal("6.15"), resposta.seguro());
        assertEquals(new BigDecimal("30.55"), resposta.ajustePagamento());
        assertEquals(new BigDecimal("462.00"), resposta.totalFinal());
        assertEquals(6, resposta.parcelas());
        assertEquals(new BigDecimal("77.00"), resposta.valorParcela());
        assertEquals(new BigDecimal("8.19"), resposta.creditoProximaCompra());
        assertFalse(resposta.brinde());
    }

    @Test
    void exemplo3_FoneMotoboyMenos50BoletoBronzeNordeste() {
        List<ItemCarrinhoDTO> itens = List.of(
            new ItemCarrinhoDTO("Fone", new BigDecimal("199.90"), 2, new BigDecimal("0.25"))
        );

        ResumoCheckoutRequestDTO request = new ResumoCheckoutRequestDTO(
            itens,
            ModalidadeEntrega.MOTOBOY,
            "MENOS50",
            FormaPagamento.BOLETO,
            1,
            NivelClube.BRONZE,
            Regiao.NORDESTE
        );

        ResumoCheckoutResponseDTO resposta = service.calcularResumo(request);

        assertEquals(new BigDecimal("399.80"), resposta.subtotalProdutos());
        assertEquals(new BigDecimal("50.00"), resposta.descontoCupom());
        assertEquals(new BigDecimal("18.00"), resposta.frete());
        assertEquals(0, resposta.prazoEntregaDias());
        assertEquals(new BigDecimal("8.00"), resposta.seguro());
        assertEquals(new BigDecimal("3.49"), resposta.ajustePagamento());
        assertEquals(new BigDecimal("379.29"), resposta.totalFinal());
        assertEquals(1, resposta.parcelas());
        assertEquals(new BigDecimal("379.29"), resposta.valorParcela());
        assertEquals(new BigDecimal("0.00"), resposta.creditoProximaCompra());
        assertFalse(resposta.brinde());
    }

    @Test
    void exemplo4_MeiaCamisetaRetiradaLeve3Pague2Cartao3xPrataSul() {
        List<ItemCarrinhoDTO> itens = List.of(
            new ItemCarrinhoDTO("Meia", new BigDecimal("19.90"), 7, new BigDecimal("0.10")),
            new ItemCarrinhoDTO("Camiseta", new BigDecimal("79.90"), 2, new BigDecimal("0.30"))
        );

        ResumoCheckoutRequestDTO request = new ResumoCheckoutRequestDTO(
            itens,
            ModalidadeEntrega.RETIRADA_LOJA,
            "LEVE3PAGUE2",
            FormaPagamento.CARTAO,
            3,
            NivelClube.PRATA,
            Regiao.SUL
        );

        ResumoCheckoutResponseDTO resposta = service.calcularResumo(request);

        assertEquals(new BigDecimal("299.10"), resposta.subtotalProdutos());
        assertEquals(new BigDecimal("39.80"), resposta.descontoCupom());
        assertEquals(new BigDecimal("0.00"), resposta.frete());
        assertEquals(1, resposta.prazoEntregaDias());
        assertEquals(new BigDecimal("2.99"), resposta.seguro());
        assertEquals(new BigDecimal("0.00"), resposta.ajustePagamento());
        assertEquals(new BigDecimal("262.29"), resposta.totalFinal());
        assertEquals(3, resposta.parcelas());
        assertEquals(new BigDecimal("87.43"), resposta.valorParcela());
        assertEquals(new BigDecimal("5.98"), resposta.creditoProximaCompra());
        assertFalse(resposta.brinde());
    }

    @Test
    void exemplo5_CamisetaTenisExpressaPixOuroSudeste() {
        List<ItemCarrinhoDTO> itens = List.of(
            new ItemCarrinhoDTO("Camiseta", new BigDecimal("79.90"), 2, new BigDecimal("0.30")),
            new ItemCarrinhoDTO("Tênis", new BigDecimal("249.90"), 1, new BigDecimal("1.20"))
        );

        ResumoCheckoutRequestDTO request = new ResumoCheckoutRequestDTO(
            itens,
            ModalidadeEntrega.EXPRESSA,
            null,
            FormaPagamento.PIX,
            1,
            NivelClube.OURO,
            Regiao.SUDESTE
        );

        ResumoCheckoutResponseDTO resposta = service.calcularResumo(request);

        assertEquals(new BigDecimal("409.70"), resposta.subtotalProdutos());
        assertEquals(new BigDecimal("0.00"), resposta.descontoCupom());
        assertEquals(new BigDecimal("0.00"), resposta.frete());
        assertEquals(2, resposta.prazoEntregaDias());
        assertEquals(new BigDecimal("4.10"), resposta.seguro());
        assertEquals(new BigDecimal("-20.69"), resposta.ajustePagamento());
        assertEquals(new BigDecimal("393.11"), resposta.totalFinal());
        assertEquals(1, resposta.parcelas());
        assertEquals(new BigDecimal("393.11"), resposta.valorParcela());
        assertEquals(new BigDecimal("20.48"), resposta.creditoProximaCompra());
        assertFalse(resposta.brinde());
    }

    @Test
    void testCarrinhoVazio() {
        ResumoCheckoutRequestDTO request = new ResumoCheckoutRequestDTO(
            List.of(),
            ModalidadeEntrega.ECONOMICA,
            null,
            FormaPagamento.PIX,
            1,
            NivelClube.BRONZE,
            Regiao.SUDESTE
        );

        CheckoutException exception = assertThrows(CheckoutException.class, () -> {
            service.calcularResumo(request);
        });

        assertEquals("PEDIDO_INVALIDO", exception.getCodigo());
    }

    @Test
    void testItemComPrecoZero() {
        List<ItemCarrinhoDTO> itens = List.of(
            new ItemCarrinhoDTO("Produto", BigDecimal.ZERO, 1, new BigDecimal("0.5"))
        );

        ResumoCheckoutRequestDTO request = new ResumoCheckoutRequestDTO(
            itens,
            ModalidadeEntrega.ECONOMICA,
            null,
            FormaPagamento.PIX,
            1,
            NivelClube.BRONZE,
            Regiao.SUDESTE
        );

        CheckoutException exception = assertThrows(CheckoutException.class, () -> {
            service.calcularResumo(request);
        });

        assertEquals("PEDIDO_INVALIDO", exception.getCodigo());
    }

    @Test
    void testNivelClubeInvalido() {
        List<ItemCarrinhoDTO> itens = List.of(
            new ItemCarrinhoDTO("Produto", new BigDecimal("50"), 1, new BigDecimal("0.5"))
        );

        ResumoCheckoutRequestDTO request = new ResumoCheckoutRequestDTO(
            itens,
            ModalidadeEntrega.ECONOMICA,
            null,
            FormaPagamento.PIX,
            1,
            null,
            Regiao.SUDESTE
        );

        CheckoutException exception = assertThrows(CheckoutException.class, () -> {
            service.calcularResumo(request);
        });

        assertEquals("NIVEL_CLUBE_INVALIDO", exception.getCodigo());
    }

    @Test
    void testRegiaoInvalida() {
        List<ItemCarrinhoDTO> itens = List.of(
            new ItemCarrinhoDTO("Produto", new BigDecimal("50"), 1, new BigDecimal("0.5"))
        );

        ResumoCheckoutRequestDTO request = new ResumoCheckoutRequestDTO(
            itens,
            ModalidadeEntrega.ECONOMICA,
            null,
            FormaPagamento.PIX,
            1,
            NivelClube.BRONZE,
            null
        );

        CheckoutException exception = assertThrows(CheckoutException.class, () -> {
            service.calcularResumo(request);
        });

        assertEquals("REGIAO_INVALIDA", exception.getCodigo());
    }

    @Test
    void testModalidadeInvalida() {
        List<ItemCarrinhoDTO> itens = List.of(
            new ItemCarrinhoDTO("Produto", new BigDecimal("50"), 1, new BigDecimal("0.5"))
        );

        ResumoCheckoutRequestDTO request = new ResumoCheckoutRequestDTO(
            itens,
            null,
            null,
            FormaPagamento.PIX,
            1,
            NivelClube.BRONZE,
            Regiao.SUDESTE
        );

        CheckoutException exception = assertThrows(CheckoutException.class, () -> {
            service.calcularResumo(request);
        });

        assertEquals("MODALIDADE_INVALIDA", exception.getCodigo());
    }

    @Test
    void testMotoboyAcima5Kg() {
        List<ItemCarrinhoDTO> itens = List.of(
            new ItemCarrinhoDTO("Produto", new BigDecimal("50"), 1, new BigDecimal("6"))
        );

        ResumoCheckoutRequestDTO request = new ResumoCheckoutRequestDTO(
            itens,
            ModalidadeEntrega.MOTOBOY,
            null,
            FormaPagamento.PIX,
            1,
            NivelClube.BRONZE,
            Regiao.SUDESTE
        );

        CheckoutException exception = assertThrows(CheckoutException.class, () -> {
            service.calcularResumo(request);
        });

        assertEquals("MODALIDADE_INDISPONIVEL", exception.getCodigo());
    }

    @Test
    void testCupomInvalido() {
        List<ItemCarrinhoDTO> itens = List.of(
            new ItemCarrinhoDTO("Produto", new BigDecimal("50"), 1, new BigDecimal("0.5"))
        );

        ResumoCheckoutRequestDTO request = new ResumoCheckoutRequestDTO(
            itens,
            ModalidadeEntrega.ECONOMICA,
            "CUPOM_INEXISTENTE",
            FormaPagamento.PIX,
            1,
            NivelClube.BRONZE,
            Regiao.SUDESTE
        );

        CheckoutException exception = assertThrows(CheckoutException.class, () -> {
            service.calcularResumo(request);
        });

        assertEquals("CUPOM_INVALIDO", exception.getCodigo());
    }

    @Test
    void testCupomMenos50NaoAplicavel() {
        List<ItemCarrinhoDTO> itens = List.of(
            new ItemCarrinhoDTO("Produto", new BigDecimal("100"), 1, new BigDecimal("0.5"))
        );

        ResumoCheckoutRequestDTO request = new ResumoCheckoutRequestDTO(
            itens,
            ModalidadeEntrega.ECONOMICA,
            "MENOS50",
            FormaPagamento.PIX,
            1,
            NivelClube.BRONZE,
            Regiao.SUDESTE
        );

        CheckoutException exception = assertThrows(CheckoutException.class, () -> {
            service.calcularResumo(request);
        });

        assertEquals("CUPOM_NAO_APLICAVEL", exception.getCodigo());
    }

    @Test
    void testFormaPagamentoInvalida() {
        List<ItemCarrinhoDTO> itens = List.of(
            new ItemCarrinhoDTO("Produto", new BigDecimal("50"), 1, new BigDecimal("0.5"))
        );

        ResumoCheckoutRequestDTO request = new ResumoCheckoutRequestDTO(
            itens,
            ModalidadeEntrega.ECONOMICA,
            null,
            null,
            1,
            NivelClube.BRONZE,
            Regiao.SUDESTE
        );

        CheckoutException exception = assertThrows(CheckoutException.class, () -> {
            service.calcularResumo(request);
        });

        assertEquals("FORMA_PAGAMENTO_INVALIDA", exception.getCodigo());
    }

    @Test
    void testParcelasInvalidasPixMais1() {
        List<ItemCarrinhoDTO> itens = List.of(
            new ItemCarrinhoDTO("Produto", new BigDecimal("50"), 1, new BigDecimal("0.5"))
        );

        ResumoCheckoutRequestDTO request = new ResumoCheckoutRequestDTO(
            itens,
            ModalidadeEntrega.ECONOMICA,
            null,
            FormaPagamento.PIX,
            2,
            NivelClube.BRONZE,
            Regiao.SUDESTE
        );

        CheckoutException exception = assertThrows(CheckoutException.class, () -> {
            service.calcularResumo(request);
        });

        assertEquals("PARCELAMENTO_INVALIDO", exception.getCodigo());
    }

    @Test
    void testBoletoAcima1000() {
        List<ItemCarrinhoDTO> itens = List.of(
            new ItemCarrinhoDTO("Produto", new BigDecimal("1050"), 1, new BigDecimal("0.5"))
        );

        ResumoCheckoutRequestDTO request = new ResumoCheckoutRequestDTO(
            itens,
            ModalidadeEntrega.ECONOMICA,
            null,
            FormaPagamento.BOLETO,
            1,
            NivelClube.BRONZE,
            Regiao.SUDESTE
        );

        CheckoutException exception = assertThrows(CheckoutException.class, () -> {
            service.calcularResumo(request);
        });

        assertEquals("FORMA_PAGAMENTO_INDISPONIVEL", exception.getCodigo());
    }

    @Test
    void testFretegratisAplicaComoCupom() {
        List<ItemCarrinhoDTO> itens = List.of(
            new ItemCarrinhoDTO("Produto", new BigDecimal("100"), 1, new BigDecimal("1"))
        );

        ResumoCheckoutRequestDTO request = new ResumoCheckoutRequestDTO(
            itens,
            ModalidadeEntrega.ECONOMICA,
            "FRETEGRATIS",
            FormaPagamento.PIX,
            1,
            NivelClube.BRONZE,
            Regiao.SUDESTE
        );

        ResumoCheckoutResponseDTO resposta = service.calcularResumo(request);

        assertEquals(new BigDecimal("100.00"), resposta.subtotalProdutos());
        assertEquals(new BigDecimal("14.00"), resposta.descontoCupom());
        assertEquals(new BigDecimal("14.00"), resposta.frete());
        assertEquals(new BigDecimal("1.00"), resposta.seguro());
    }

    @Test
    void testOuroBrindeComAcimaDe500() {
        List<ItemCarrinhoDTO> itens = List.of(
            new ItemCarrinhoDTO("Produto", new BigDecimal("501"), 1, new BigDecimal("0.5"))
        );

        ResumoCheckoutRequestDTO request = new ResumoCheckoutRequestDTO(
            itens,
            ModalidadeEntrega.ECONOMICA,
            null,
            FormaPagamento.PIX,
            1,
            NivelClube.OURO,
            Regiao.SUDESTE
        );

        ResumoCheckoutResponseDTO resposta = service.calcularResumo(request);

        assertTrue(resposta.brinde());
    }

    @Test
    void testOuroSemBrindeSemAcimaDe500() {
        List<ItemCarrinhoDTO> itens = List.of(
            new ItemCarrinhoDTO("Produto", new BigDecimal("500"), 1, new BigDecimal("0.5"))
        );

        ResumoCheckoutRequestDTO request = new ResumoCheckoutRequestDTO(
            itens,
            ModalidadeEntrega.ECONOMICA,
            null,
            FormaPagamento.PIX,
            1,
            NivelClube.OURO,
            Regiao.SUDESTE
        );

        ResumoCheckoutResponseDTO resposta = service.calcularResumo(request);

        assertFalse(resposta.brinde());
    }
}

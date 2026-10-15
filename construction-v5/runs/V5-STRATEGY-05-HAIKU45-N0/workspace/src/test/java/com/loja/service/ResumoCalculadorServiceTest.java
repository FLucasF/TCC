package com.loja.service;

import com.loja.dto.Item;
import com.loja.dto.ResumoRequest;
import com.loja.dto.ResumoResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class ResumoCalculadorServiceTest {

    private ResumoCalculadorService service;

    @BeforeEach
    void setUp() {
        service = new ResumoCalculadorService();
    }

    @Test
    void testExemplo1() throws ResumoCalculadorService.ValidationException {
        List<Item> itens = new ArrayList<>();
        itens.add(new Item("Camiseta", 79.90, 2, 0.30));
        itens.add(new Item("Tênis", 249.90, 1, 1.20));

        ResumoRequest request = new ResumoRequest();
        request.setItens(itens);
        request.setModalidadeEntrega("EXPRESSA");
        request.setCupom("BEMVINDO10");
        request.setFormaPagamento("PIX");
        request.setParcelas(1);
        request.setNivelClube("BRONZE");
        request.setRegiao("NORTE");

        ResumoResponse response = service.calcular(request);

        assertEquals(new BigDecimal("409.70"), response.getSubtotalProdutos());
        assertEquals(new BigDecimal("40.97"), response.getDescontoCupom());
        assertEquals(new BigDecimal("33.10"), response.getFrete());
        assertEquals(2, response.getPrazoEntregaDias());
        assertEquals(new BigDecimal("10.24"), response.getSeguro());
        assertEquals(new BigDecimal("-20.60"), response.getAjustePagamento());
        assertEquals(new BigDecimal("391.47"), response.getTotalFinal());
        assertEquals(1, response.getParcelas());
        assertEquals(new BigDecimal("391.47"), response.getValorParcela());
        assertEquals(new BigDecimal("0.00"), response.getCreditoProximaCompra());
        assertEquals(false, response.getBrinde());
    }

    @Test
    void testExemplo2() throws ResumoCalculadorService.ValidationException {
        List<Item> itens = new ArrayList<>();
        itens.add(new Item("Camiseta", 79.90, 2, 0.30));
        itens.add(new Item("Tênis", 249.90, 1, 1.20));

        ResumoRequest request = new ResumoRequest();
        request.setItens(itens);
        request.setModalidadeEntrega("ECONOMICA");
        request.setCupom(null);
        request.setFormaPagamento("CARTAO");
        request.setParcelas(6);
        request.setNivelClube("PRATA");
        request.setRegiao("CENTRO_OESTE");

        ResumoResponse response = service.calcular(request);

        assertEquals(new BigDecimal("409.70"), response.getSubtotalProdutos());
        assertEquals(new BigDecimal("0.00"), response.getDescontoCupom());
        assertEquals(new BigDecimal("15.60"), response.getFrete());
        assertEquals(7, response.getPrazoEntregaDias());
        assertEquals(new BigDecimal("6.15"), response.getSeguro());
        assertEquals(new BigDecimal("30.55"), response.getAjustePagamento());
        assertEquals(new BigDecimal("462.00"), response.getTotalFinal());
        assertEquals(6, response.getParcelas());
        assertEquals(new BigDecimal("77.00"), response.getValorParcela());
        assertEquals(new BigDecimal("8.19"), response.getCreditoProximaCompra());
        assertEquals(false, response.getBrinde());
    }

    @Test
    void testExemplo3() throws ResumoCalculadorService.ValidationException {
        List<Item> itens = new ArrayList<>();
        itens.add(new Item("Fone", 199.90, 2, 0.25));

        ResumoRequest request = new ResumoRequest();
        request.setItens(itens);
        request.setModalidadeEntrega("MOTOBOY");
        request.setCupom("MENOS50");
        request.setFormaPagamento("BOLETO");
        request.setParcelas(1);
        request.setNivelClube("BRONZE");
        request.setRegiao("NORDESTE");

        ResumoResponse response = service.calcular(request);

        assertEquals(new BigDecimal("399.80"), response.getSubtotalProdutos());
        assertEquals(new BigDecimal("50.00"), response.getDescontoCupom());
        assertEquals(new BigDecimal("18.00"), response.getFrete());
        assertEquals(0, response.getPrazoEntregaDias());
        assertEquals(new BigDecimal("8.00"), response.getSeguro());
        assertEquals(new BigDecimal("3.49"), response.getAjustePagamento());
        assertEquals(new BigDecimal("379.29"), response.getTotalFinal());
        assertEquals(1, response.getParcelas());
        assertEquals(new BigDecimal("379.29"), response.getValorParcela());
        assertEquals(new BigDecimal("0.00"), response.getCreditoProximaCompra());
        assertEquals(false, response.getBrinde());
    }

    @Test
    void testExemplo4() throws ResumoCalculadorService.ValidationException {
        List<Item> itens = new ArrayList<>();
        itens.add(new Item("Meia", 19.90, 7, 0.10));
        itens.add(new Item("Camiseta", 79.90, 2, 0.30));

        ResumoRequest request = new ResumoRequest();
        request.setItens(itens);
        request.setModalidadeEntrega("RETIRADA_LOJA");
        request.setCupom("LEVE3PAGUE2");
        request.setFormaPagamento("CARTAO");
        request.setParcelas(3);
        request.setNivelClube("PRATA");
        request.setRegiao("SUL");

        ResumoResponse response = service.calcular(request);

        assertEquals(new BigDecimal("299.10"), response.getSubtotalProdutos());
        assertEquals(new BigDecimal("39.80"), response.getDescontoCupom());
        assertEquals(new BigDecimal("0.00"), response.getFrete());
        assertEquals(1, response.getPrazoEntregaDias());
        assertEquals(new BigDecimal("2.99"), response.getSeguro());
        assertEquals(new BigDecimal("0.00"), response.getAjustePagamento());
        assertEquals(new BigDecimal("262.29"), response.getTotalFinal());
        assertEquals(3, response.getParcelas());
        assertEquals(new BigDecimal("87.43"), response.getValorParcela());
        assertEquals(new BigDecimal("5.98"), response.getCreditoProximaCompra());
        assertEquals(false, response.getBrinde());
    }

    @Test
    void testExemplo5() throws ResumoCalculadorService.ValidationException {
        List<Item> itens = new ArrayList<>();
        itens.add(new Item("Camiseta", 79.90, 2, 0.30));
        itens.add(new Item("Tênis", 249.90, 1, 1.20));

        ResumoRequest request = new ResumoRequest();
        request.setItens(itens);
        request.setModalidadeEntrega("EXPRESSA");
        request.setCupom(null);
        request.setFormaPagamento("PIX");
        request.setParcelas(1);
        request.setNivelClube("OURO");
        request.setRegiao("SUDESTE");

        ResumoResponse response = service.calcular(request);

        assertEquals(new BigDecimal("409.70"), response.getSubtotalProdutos());
        assertEquals(new BigDecimal("0.00"), response.getDescontoCupom());
        assertEquals(new BigDecimal("0.00"), response.getFrete());
        assertEquals(2, response.getPrazoEntregaDias());
        assertEquals(new BigDecimal("4.10"), response.getSeguro());
        assertEquals(new BigDecimal("-20.69"), response.getAjustePagamento());
        assertEquals(new BigDecimal("393.11"), response.getTotalFinal());
        assertEquals(1, response.getParcelas());
        assertEquals(new BigDecimal("393.11"), response.getValorParcela());
        assertEquals(new BigDecimal("20.48"), response.getCreditoProximaCompra());
        assertEquals(false, response.getBrinde());
    }

    @Test
    void testCarrinhoVazio() {
        ResumoRequest request = new ResumoRequest();
        request.setItens(new ArrayList<>());
        request.setModalidadeEntrega("EXPRESSA");
        request.setFormaPagamento("PIX");
        request.setNivelClube("BRONZE");
        request.setRegiao("SUDESTE");

        assertThrows(ResumoCalculadorService.ValidationException.class, () -> {
            try {
                service.calcular(request);
            } catch (ResumoCalculadorService.ValidationException e) {
                assertEquals("PEDIDO_INVALIDO", e.getCode());
                throw e;
            }
        });
    }

    @Test
    void testItemComPrecoNegativo() {
        List<Item> itens = new ArrayList<>();
        itens.add(new Item("Camiseta", -10.0, 1, 0.30));

        ResumoRequest request = new ResumoRequest();
        request.setItens(itens);
        request.setModalidadeEntrega("EXPRESSA");
        request.setFormaPagamento("PIX");
        request.setNivelClube("BRONZE");
        request.setRegiao("SUDESTE");

        assertThrows(ResumoCalculadorService.ValidationException.class, () -> {
            try {
                service.calcular(request);
            } catch (ResumoCalculadorService.ValidationException e) {
                assertEquals("PEDIDO_INVALIDO", e.getCode());
                throw e;
            }
        });
    }

    @Test
    void testNivelClubeInvalido() {
        List<Item> itens = new ArrayList<>();
        itens.add(new Item("Camiseta", 79.90, 1, 0.30));

        ResumoRequest request = new ResumoRequest();
        request.setItens(itens);
        request.setModalidadeEntrega("EXPRESSA");
        request.setFormaPagamento("PIX");
        request.setNivelClube("DIAMANTE");
        request.setRegiao("SUDESTE");

        assertThrows(ResumoCalculadorService.ValidationException.class, () -> {
            try {
                service.calcular(request);
            } catch (ResumoCalculadorService.ValidationException e) {
                assertEquals("NIVEL_CLUBE_INVALIDO", e.getCode());
                throw e;
            }
        });
    }

    @Test
    void testRegiaoInvalida() {
        List<Item> itens = new ArrayList<>();
        itens.add(new Item("Camiseta", 79.90, 1, 0.30));

        ResumoRequest request = new ResumoRequest();
        request.setItens(itens);
        request.setModalidadeEntrega("EXPRESSA");
        request.setFormaPagamento("PIX");
        request.setNivelClube("BRONZE");
        request.setRegiao("MARTE");

        assertThrows(ResumoCalculadorService.ValidationException.class, () -> {
            try {
                service.calcular(request);
            } catch (ResumoCalculadorService.ValidationException e) {
                assertEquals("REGIAO_INVALIDA", e.getCode());
                throw e;
            }
        });
    }

    @Test
    void testModalidadeInvalida() {
        List<Item> itens = new ArrayList<>();
        itens.add(new Item("Camiseta", 79.90, 1, 0.30));

        ResumoRequest request = new ResumoRequest();
        request.setItens(itens);
        request.setModalidadeEntrega("AVIAO");
        request.setFormaPagamento("PIX");
        request.setNivelClube("BRONZE");
        request.setRegiao("SUDESTE");

        assertThrows(ResumoCalculadorService.ValidationException.class, () -> {
            try {
                service.calcular(request);
            } catch (ResumoCalculadorService.ValidationException e) {
                assertEquals("MODALIDADE_INVALIDA", e.getCode());
                throw e;
            }
        });
    }

    @Test
    void testMotoboySobreLimit() {
        List<Item> itens = new ArrayList<>();
        itens.add(new Item("Camiseta", 79.90, 1, 6.0));

        ResumoRequest request = new ResumoRequest();
        request.setItens(itens);
        request.setModalidadeEntrega("MOTOBOY");
        request.setFormaPagamento("PIX");
        request.setNivelClube("BRONZE");
        request.setRegiao("SUDESTE");

        assertThrows(ResumoCalculadorService.ValidationException.class, () -> {
            try {
                service.calcular(request);
            } catch (ResumoCalculadorService.ValidationException e) {
                assertEquals("MODALIDADE_INDISPONIVEL", e.getCode());
                throw e;
            }
        });
    }

    @Test
    void testCupomInvalido() {
        List<Item> itens = new ArrayList<>();
        itens.add(new Item("Camiseta", 79.90, 1, 0.30));

        ResumoRequest request = new ResumoRequest();
        request.setItens(itens);
        request.setModalidadeEntrega("EXPRESSA");
        request.setCupom("DESCONTO50");
        request.setFormaPagamento("PIX");
        request.setNivelClube("BRONZE");
        request.setRegiao("SUDESTE");

        assertThrows(ResumoCalculadorService.ValidationException.class, () -> {
            try {
                service.calcular(request);
            } catch (ResumoCalculadorService.ValidationException e) {
                assertEquals("CUPOM_INVALIDO", e.getCode());
                throw e;
            }
        });
    }

    @Test
    void testCupomMenos50SemRequisito() {
        List<Item> itens = new ArrayList<>();
        itens.add(new Item("Camiseta", 79.90, 1, 0.30));

        ResumoRequest request = new ResumoRequest();
        request.setItens(itens);
        request.setModalidadeEntrega("EXPRESSA");
        request.setCupom("MENOS50");
        request.setFormaPagamento("PIX");
        request.setNivelClube("BRONZE");
        request.setRegiao("SUDESTE");

        assertThrows(ResumoCalculadorService.ValidationException.class, () -> {
            try {
                service.calcular(request);
            } catch (ResumoCalculadorService.ValidationException e) {
                assertEquals("CUPOM_NAO_APLICAVEL", e.getCode());
                throw e;
            }
        });
    }

    @Test
    void testFormaPagamentoInvalida() {
        List<Item> itens = new ArrayList<>();
        itens.add(new Item("Camiseta", 79.90, 1, 0.30));

        ResumoRequest request = new ResumoRequest();
        request.setItens(itens);
        request.setModalidadeEntrega("EXPRESSA");
        request.setFormaPagamento("DINHEIRO");
        request.setNivelClube("BRONZE");
        request.setRegiao("SUDESTE");

        assertThrows(ResumoCalculadorService.ValidationException.class, () -> {
            try {
                service.calcular(request);
            } catch (ResumoCalculadorService.ValidationException e) {
                assertEquals("FORMA_PAGAMENTO_INVALIDA", e.getCode());
                throw e;
            }
        });
    }

    @Test
    void testParcelamentoBoletoInvalido() {
        List<Item> itens = new ArrayList<>();
        itens.add(new Item("Camiseta", 79.90, 1, 0.30));

        ResumoRequest request = new ResumoRequest();
        request.setItens(itens);
        request.setModalidadeEntrega("EXPRESSA");
        request.setFormaPagamento("BOLETO");
        request.setParcelas(3);
        request.setNivelClube("BRONZE");
        request.setRegiao("SUDESTE");

        assertThrows(ResumoCalculadorService.ValidationException.class, () -> {
            try {
                service.calcular(request);
            } catch (ResumoCalculadorService.ValidationException e) {
                assertEquals("PARCELAMENTO_INVALIDO", e.getCode());
                throw e;
            }
        });
    }

    @Test
    void testParcelamentoCartaoAcima12() {
        List<Item> itens = new ArrayList<>();
        itens.add(new Item("Camiseta", 79.90, 1, 0.30));

        ResumoRequest request = new ResumoRequest();
        request.setItens(itens);
        request.setModalidadeEntrega("EXPRESSA");
        request.setFormaPagamento("CARTAO");
        request.setParcelas(13);
        request.setNivelClube("BRONZE");
        request.setRegiao("SUDESTE");

        assertThrows(ResumoCalculadorService.ValidationException.class, () -> {
            try {
                service.calcular(request);
            } catch (ResumoCalculadorService.ValidationException e) {
                assertEquals("PARCELAMENTO_INVALIDO", e.getCode());
                throw e;
            }
        });
    }

    @Test
    void testBoletoAcima1000() {
        List<Item> itens = new ArrayList<>();
        itens.add(new Item("Relógio", 999.90, 2, 0.30));

        ResumoRequest request = new ResumoRequest();
        request.setItens(itens);
        request.setModalidadeEntrega("EXPRESSA");
        request.setFormaPagamento("BOLETO");
        request.setParcelas(1);
        request.setNivelClube("BRONZE");
        request.setRegiao("SUDESTE");

        assertThrows(ResumoCalculadorService.ValidationException.class, () -> {
            try {
                service.calcular(request);
            } catch (ResumoCalculadorService.ValidationException e) {
                assertEquals("FORMA_PAGAMENTO_INDISPONIVEL", e.getCode());
                throw e;
            }
        });
    }
}

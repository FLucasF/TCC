package com.loja.checkout.service;

import com.loja.checkout.dto.ItemCarrinho;
import com.loja.checkout.dto.PedidoRequest;
import com.loja.checkout.dto.ResumoResponse;
import com.loja.checkout.enums.*;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class ResumoServiceTest {

    private final ResumoService service = new ResumoService();

    @Test
    void exemplo1() {
        PedidoRequest pedido = new PedidoRequest();
        pedido.setItens(List.of(
                criarItem("Camiseta", "79.90", 2, "0.30"),
                criarItem("Tênis", "249.90", 1, "1.20")
        ));
        pedido.setModalidadeEntrega(ModalidadeEntrega.EXPRESSA);
        pedido.setCupom("BEMVINDO10");
        pedido.setFormaPagamento(FormaPagamento.PIX);
        pedido.setNivelClube(NivelClube.BRONZE);
        pedido.setRegiao(Regiao.NORTE);

        ResumoResponse resumo = service.calcular(pedido);

        assertEquals(new BigDecimal("409.70"), resumo.getSubtotalProdutos());
        assertEquals(new BigDecimal("40.97"), resumo.getDescontoCupom());
        assertEquals(new BigDecimal("33.10"), resumo.getFrete());
        assertEquals(2, resumo.getPrazoEntregaDias());
        assertEquals(new BigDecimal("10.24"), resumo.getSeguro());
        assertEquals(new BigDecimal("-20.60"), resumo.getAjustePagamento());
        assertEquals(new BigDecimal("391.47"), resumo.getTotalFinal());
        assertEquals(1, resumo.getParcelas());
        assertEquals(new BigDecimal("391.47"), resumo.getValorParcela());
        assertEquals(new BigDecimal("0.00"), resumo.getCreditoProximaCompra());
        assertFalse(resumo.getBrinde());
    }

    @Test
    void exemplo2() {
        PedidoRequest pedido = new PedidoRequest();
        pedido.setItens(List.of(
                criarItem("Camiseta", "79.90", 2, "0.30"),
                criarItem("Tênis", "249.90", 1, "1.20")
        ));
        pedido.setModalidadeEntrega(ModalidadeEntrega.ECONOMICA);
        pedido.setFormaPagamento(FormaPagamento.CARTAO);
        pedido.setParcelas(6);
        pedido.setNivelClube(NivelClube.PRATA);
        pedido.setRegiao(Regiao.CENTRO_OESTE);

        ResumoResponse resumo = service.calcular(pedido);

        assertEquals(new BigDecimal("409.70"), resumo.getSubtotalProdutos());
        assertEquals(new BigDecimal("0.00"), resumo.getDescontoCupom());
        assertEquals(new BigDecimal("15.60"), resumo.getFrete());
        assertEquals(7, resumo.getPrazoEntregaDias());
        assertEquals(new BigDecimal("6.15"), resumo.getSeguro());
        assertEquals(new BigDecimal("30.55"), resumo.getAjustePagamento());
        assertEquals(new BigDecimal("462.00"), resumo.getTotalFinal());
        assertEquals(6, resumo.getParcelas());
        assertEquals(new BigDecimal("77.00"), resumo.getValorParcela());
        assertEquals(new BigDecimal("8.19"), resumo.getCreditoProximaCompra());
        assertFalse(resumo.getBrinde());
    }

    @Test
    void exemplo3() {
        PedidoRequest pedido = new PedidoRequest();
        pedido.setItens(List.of(
                criarItem("Fone", "199.90", 2, "0.25")
        ));
        pedido.setModalidadeEntrega(ModalidadeEntrega.MOTOBOY);
        pedido.setCupom("MENOS50");
        pedido.setFormaPagamento(FormaPagamento.BOLETO);
        pedido.setNivelClube(NivelClube.BRONZE);
        pedido.setRegiao(Regiao.NORDESTE);

        ResumoResponse resumo = service.calcular(pedido);

        assertEquals(new BigDecimal("399.80"), resumo.getSubtotalProdutos());
        assertEquals(new BigDecimal("50.00"), resumo.getDescontoCupom());
        assertEquals(new BigDecimal("18.00"), resumo.getFrete());
        assertEquals(0, resumo.getPrazoEntregaDias());
        assertEquals(new BigDecimal("8.00"), resumo.getSeguro());
        assertEquals(new BigDecimal("3.49"), resumo.getAjustePagamento());
        assertEquals(new BigDecimal("379.29"), resumo.getTotalFinal());
        assertEquals(1, resumo.getParcelas());
        assertEquals(new BigDecimal("379.29"), resumo.getValorParcela());
        assertEquals(new BigDecimal("0.00"), resumo.getCreditoProximaCompra());
        assertFalse(resumo.getBrinde());
    }

    @Test
    void exemplo4() {
        PedidoRequest pedido = new PedidoRequest();
        pedido.setItens(List.of(
                criarItem("Meia", "19.90", 7, "0.10"),
                criarItem("Camiseta", "79.90", 2, "0.30")
        ));
        pedido.setModalidadeEntrega(ModalidadeEntrega.RETIRADA_LOJA);
        pedido.setCupom("LEVE3PAGUE2");
        pedido.setFormaPagamento(FormaPagamento.CARTAO);
        pedido.setParcelas(3);
        pedido.setNivelClube(NivelClube.PRATA);
        pedido.setRegiao(Regiao.SUL);

        ResumoResponse resumo = service.calcular(pedido);

        assertEquals(new BigDecimal("299.10"), resumo.getSubtotalProdutos());
        assertEquals(new BigDecimal("39.80"), resumo.getDescontoCupom());
        assertEquals(new BigDecimal("0.00"), resumo.getFrete());
        assertEquals(1, resumo.getPrazoEntregaDias());
        assertEquals(new BigDecimal("2.99"), resumo.getSeguro());
        assertEquals(new BigDecimal("0.00"), resumo.getAjustePagamento());
        assertEquals(new BigDecimal("262.29"), resumo.getTotalFinal());
        assertEquals(3, resumo.getParcelas());
        assertEquals(new BigDecimal("87.43"), resumo.getValorParcela());
        assertEquals(new BigDecimal("5.98"), resumo.getCreditoProximaCompra());
        assertFalse(resumo.getBrinde());
    }

    @Test
    void exemplo5() {
        PedidoRequest pedido = new PedidoRequest();
        pedido.setItens(List.of(
                criarItem("Camiseta", "79.90", 2, "0.30"),
                criarItem("Tênis", "249.90", 1, "1.20")
        ));
        pedido.setModalidadeEntrega(ModalidadeEntrega.EXPRESSA);
        pedido.setFormaPagamento(FormaPagamento.PIX);
        pedido.setNivelClube(NivelClube.OURO);
        pedido.setRegiao(Regiao.SUDESTE);

        ResumoResponse resumo = service.calcular(pedido);

        assertEquals(new BigDecimal("409.70"), resumo.getSubtotalProdutos());
        assertEquals(new BigDecimal("0.00"), resumo.getDescontoCupom());
        assertEquals(new BigDecimal("0.00"), resumo.getFrete());
        assertEquals(2, resumo.getPrazoEntregaDias());
        assertEquals(new BigDecimal("4.10"), resumo.getSeguro());
        assertEquals(new BigDecimal("-20.69"), resumo.getAjustePagamento());
        assertEquals(new BigDecimal("393.11"), resumo.getTotalFinal());
        assertEquals(1, resumo.getParcelas());
        assertEquals(new BigDecimal("393.11"), resumo.getValorParcela());
        assertEquals(new BigDecimal("20.48"), resumo.getCreditoProximaCompra());
        assertFalse(resumo.getBrinde());
    }

    @Test
    void cupomFreteGratis() {
        PedidoRequest pedido = new PedidoRequest();
        pedido.setItens(List.of(criarItem("Teste", "100.00", 1, "1.00")));
        pedido.setModalidadeEntrega(ModalidadeEntrega.ECONOMICA);
        pedido.setCupom("FRETEGRATIS");
        pedido.setFormaPagamento(FormaPagamento.PIX);
        pedido.setNivelClube(NivelClube.BRONZE);
        pedido.setRegiao(Regiao.SUDESTE);

        ResumoResponse resumo = service.calcular(pedido);

        assertEquals(new BigDecimal("14.00"), resumo.getFrete());
        assertEquals(new BigDecimal("14.00"), resumo.getDescontoCupom());
    }

    @Test
    void validacao_pedidoVazio() {
        PedidoRequest pedido = new PedidoRequest();
        pedido.setItens(List.of());
        pedido.setNivelClube(NivelClube.BRONZE);
        pedido.setRegiao(Regiao.SUDESTE);
        pedido.setModalidadeEntrega(ModalidadeEntrega.ECONOMICA);
        pedido.setFormaPagamento(FormaPagamento.PIX);

        ValidacaoException ex = assertThrows(ValidacaoException.class, () -> service.calcular(pedido));
        assertEquals(CodigoErro.PEDIDO_INVALIDO, ex.getCodigo());
    }

    @Test
    void validacao_nivelClubeInvalido() {
        PedidoRequest pedido = new PedidoRequest();
        pedido.setItens(List.of(criarItem("Teste", "10.00", 1, "0.10")));
        pedido.setRegiao(Regiao.SUDESTE);
        pedido.setModalidadeEntrega(ModalidadeEntrega.ECONOMICA);
        pedido.setFormaPagamento(FormaPagamento.PIX);

        ValidacaoException ex = assertThrows(ValidacaoException.class, () -> service.calcular(pedido));
        assertEquals(CodigoErro.NIVEL_CLUBE_INVALIDO, ex.getCodigo());
    }

    @Test
    void validacao_regiaoInvalida() {
        PedidoRequest pedido = new PedidoRequest();
        pedido.setItens(List.of(criarItem("Teste", "10.00", 1, "0.10")));
        pedido.setNivelClube(NivelClube.BRONZE);
        pedido.setModalidadeEntrega(ModalidadeEntrega.ECONOMICA);
        pedido.setFormaPagamento(FormaPagamento.PIX);

        ValidacaoException ex = assertThrows(ValidacaoException.class, () -> service.calcular(pedido));
        assertEquals(CodigoErro.REGIAO_INVALIDA, ex.getCodigo());
    }

    @Test
    void validacao_modalidadeInvalida() {
        PedidoRequest pedido = new PedidoRequest();
        pedido.setItens(List.of(criarItem("Teste", "10.00", 1, "0.10")));
        pedido.setNivelClube(NivelClube.BRONZE);
        pedido.setRegiao(Regiao.SUDESTE);
        pedido.setFormaPagamento(FormaPagamento.PIX);

        ValidacaoException ex = assertThrows(ValidacaoException.class, () -> service.calcular(pedido));
        assertEquals(CodigoErro.MODALIDADE_INVALIDA, ex.getCodigo());
    }

    @Test
    void validacao_modalidadeIndisponivel() {
        PedidoRequest pedido = new PedidoRequest();
        pedido.setItens(List.of(criarItem("Teste", "10.00", 1, "10.00")));
        pedido.setNivelClube(NivelClube.BRONZE);
        pedido.setRegiao(Regiao.SUDESTE);
        pedido.setModalidadeEntrega(ModalidadeEntrega.MOTOBOY);
        pedido.setFormaPagamento(FormaPagamento.PIX);

        ValidacaoException ex = assertThrows(ValidacaoException.class, () -> service.calcular(pedido));
        assertEquals(CodigoErro.MODALIDADE_INDISPONIVEL, ex.getCodigo());
    }

    @Test
    void validacao_cupomInvalido() {
        PedidoRequest pedido = new PedidoRequest();
        pedido.setItens(List.of(criarItem("Teste", "100.00", 1, "0.10")));
        pedido.setNivelClube(NivelClube.BRONZE);
        pedido.setRegiao(Regiao.SUDESTE);
        pedido.setModalidadeEntrega(ModalidadeEntrega.ECONOMICA);
        pedido.setCupom("INVALIDO");
        pedido.setFormaPagamento(FormaPagamento.PIX);

        ValidacaoException ex = assertThrows(ValidacaoException.class, () -> service.calcular(pedido));
        assertEquals(CodigoErro.CUPOM_INVALIDO, ex.getCodigo());
    }

    @Test
    void validacao_cupomNaoAplicavel() {
        PedidoRequest pedido = new PedidoRequest();
        pedido.setItens(List.of(criarItem("Teste", "100.00", 1, "0.10")));
        pedido.setNivelClube(NivelClube.BRONZE);
        pedido.setRegiao(Regiao.SUDESTE);
        pedido.setModalidadeEntrega(ModalidadeEntrega.ECONOMICA);
        pedido.setCupom("MENOS50");
        pedido.setFormaPagamento(FormaPagamento.PIX);

        ValidacaoException ex = assertThrows(ValidacaoException.class, () -> service.calcular(pedido));
        assertEquals(CodigoErro.CUPOM_NAO_APLICAVEL, ex.getCodigo());
    }

    @Test
    void validacao_formaPagamentoInvalida() {
        PedidoRequest pedido = new PedidoRequest();
        pedido.setItens(List.of(criarItem("Teste", "100.00", 1, "0.10")));
        pedido.setNivelClube(NivelClube.BRONZE);
        pedido.setRegiao(Regiao.SUDESTE);
        pedido.setModalidadeEntrega(ModalidadeEntrega.ECONOMICA);

        ValidacaoException ex = assertThrows(ValidacaoException.class, () -> service.calcular(pedido));
        assertEquals(CodigoErro.FORMA_PAGAMENTO_INVALIDA, ex.getCodigo());
    }

    @Test
    void validacao_parcelamentoInvalido() {
        PedidoRequest pedido = new PedidoRequest();
        pedido.setItens(List.of(criarItem("Teste", "100.00", 1, "0.10")));
        pedido.setNivelClube(NivelClube.BRONZE);
        pedido.setRegiao(Regiao.SUDESTE);
        pedido.setModalidadeEntrega(ModalidadeEntrega.ECONOMICA);
        pedido.setFormaPagamento(FormaPagamento.PIX);
        pedido.setParcelas(2);

        ValidacaoException ex = assertThrows(ValidacaoException.class, () -> service.calcular(pedido));
        assertEquals(CodigoErro.PARCELAMENTO_INVALIDO, ex.getCodigo());
    }

    @Test
    void validacao_formaPagamentoIndisponivel() {
        PedidoRequest pedido = new PedidoRequest();
        pedido.setItens(List.of(criarItem("Teste", "1100.00", 1, "0.10")));
        pedido.setNivelClube(NivelClube.BRONZE);
        pedido.setRegiao(Regiao.SUDESTE);
        pedido.setModalidadeEntrega(ModalidadeEntrega.ECONOMICA);
        pedido.setFormaPagamento(FormaPagamento.BOLETO);

        ValidacaoException ex = assertThrows(ValidacaoException.class, () -> service.calcular(pedido));
        assertEquals(CodigoErro.FORMA_PAGAMENTO_INDISPONIVEL, ex.getCodigo());
    }

    private ItemCarrinho criarItem(String nome, String preco, int quantidade, String peso) {
        ItemCarrinho item = new ItemCarrinho();
        item.setNome(nome);
        item.setPrecoUnitario(new BigDecimal(preco));
        item.setQuantidade(quantidade);
        item.setPesoKg(new BigDecimal(peso));
        return item;
    }
}

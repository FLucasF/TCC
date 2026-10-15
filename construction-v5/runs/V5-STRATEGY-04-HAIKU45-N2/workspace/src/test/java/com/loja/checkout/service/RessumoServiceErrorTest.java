package com.loja.checkout.service;

import java.util.Arrays;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

import com.loja.checkout.model.*;

public class RessumoServiceErrorTest {
    private RessumoService service;

    @BeforeEach
    public void setup() {
        service = new RessumoService();
    }

    @Test
    public void pedidoInvalido_carrinhVazio() {
        RessumoRequest request = new RessumoRequest();
        request.itens = Arrays.asList();
        request.modalidadeEntrega = ModalidadeEntrega.ECONOMICA;
        request.formaPagamento = FormaPagamento.PIX;
        request.nivelClube = NivelClube.BRONZE;
        request.regiao = Regiao.SUDESTE;

        ErroResponse response = (ErroResponse) service.calcular(request);
        assertEquals("PEDIDO_INVALIDO", response.erro);
    }

    @Test
    public void pedidoInvalido_precoZero() {
        RessumoRequest request = new RessumoRequest();
        request.itens = Arrays.asList(new Item("Produto", 0, 1, 0.5));
        request.modalidadeEntrega = ModalidadeEntrega.ECONOMICA;
        request.formaPagamento = FormaPagamento.PIX;
        request.nivelClube = NivelClube.BRONZE;
        request.regiao = Regiao.SUDESTE;

        ErroResponse response = (ErroResponse) service.calcular(request);
        assertEquals("PEDIDO_INVALIDO", response.erro);
    }

    @Test
    public void nivelClubeInvalido() {
        RessumoRequest request = new RessumoRequest();
        request.itens = Arrays.asList(new Item("Produto", 100, 1, 0.5));
        request.modalidadeEntrega = ModalidadeEntrega.ECONOMICA;
        request.formaPagamento = FormaPagamento.PIX;
        request.nivelClube = null;
        request.regiao = Regiao.SUDESTE;

        ErroResponse response = (ErroResponse) service.calcular(request);
        assertEquals("NIVEL_CLUBE_INVALIDO", response.erro);
    }

    @Test
    public void regiaoInvalida() {
        RessumoRequest request = new RessumoRequest();
        request.itens = Arrays.asList(new Item("Produto", 100, 1, 0.5));
        request.modalidadeEntrega = ModalidadeEntrega.ECONOMICA;
        request.formaPagamento = FormaPagamento.PIX;
        request.nivelClube = NivelClube.BRONZE;
        request.regiao = null;

        ErroResponse response = (ErroResponse) service.calcular(request);
        assertEquals("REGIAO_INVALIDA", response.erro);
    }

    @Test
    public void modalidadeInvalida() {
        RessumoRequest request = new RessumoRequest();
        request.itens = Arrays.asList(new Item("Produto", 100, 1, 0.5));
        request.modalidadeEntrega = null;
        request.formaPagamento = FormaPagamento.PIX;
        request.nivelClube = NivelClube.BRONZE;
        request.regiao = Regiao.SUDESTE;

        ErroResponse response = (ErroResponse) service.calcular(request);
        assertEquals("MODALIDADE_INVALIDA", response.erro);
    }

    @Test
    public void modalidadeIndisponivel_motoboySobrepeso() {
        RessumoRequest request = new RessumoRequest();
        request.itens = Arrays.asList(new Item("Produto pesado", 100, 1, 6.0));
        request.modalidadeEntrega = ModalidadeEntrega.MOTOBOY;
        request.formaPagamento = FormaPagamento.PIX;
        request.nivelClube = NivelClube.BRONZE;
        request.regiao = Regiao.SUDESTE;

        ErroResponse response = (ErroResponse) service.calcular(request);
        assertEquals("MODALIDADE_INDISPONIVEL", response.erro);
    }

    @Test
    public void cupomInvalido() {
        RessumoRequest request = new RessumoRequest();
        request.itens = Arrays.asList(new Item("Produto", 100, 1, 0.5));
        request.modalidadeEntrega = ModalidadeEntrega.ECONOMICA;
        request.cupom = "CUPOMINEXISTENTE";
        request.formaPagamento = FormaPagamento.PIX;
        request.nivelClube = NivelClube.BRONZE;
        request.regiao = Regiao.SUDESTE;

        ErroResponse response = (ErroResponse) service.calcular(request);
        assertEquals("CUPOM_INVALIDO", response.erro);
    }

    @Test
    public void cupomNaoAplicavel_menos50() {
        RessumoRequest request = new RessumoRequest();
        request.itens = Arrays.asList(new Item("Produto", 250, 1, 0.5));
        request.modalidadeEntrega = ModalidadeEntrega.ECONOMICA;
        request.cupom = "MENOS50";
        request.formaPagamento = FormaPagamento.PIX;
        request.nivelClube = NivelClube.BRONZE;
        request.regiao = Regiao.SUDESTE;

        ErroResponse response = (ErroResponse) service.calcular(request);
        assertEquals("CUPOM_NAO_APLICAVEL", response.erro);
    }

    @Test
    public void formaPagamentoInvalida() {
        RessumoRequest request = new RessumoRequest();
        request.itens = Arrays.asList(new Item("Produto", 100, 1, 0.5));
        request.modalidadeEntrega = ModalidadeEntrega.ECONOMICA;
        request.formaPagamento = null;
        request.nivelClube = NivelClube.BRONZE;
        request.regiao = Regiao.SUDESTE;

        ErroResponse response = (ErroResponse) service.calcular(request);
        assertEquals("FORMA_PAGAMENTO_INVALIDA", response.erro);
    }

    @Test
    public void parcelamentoInvalido_pixtres() {
        RessumoRequest request = new RessumoRequest();
        request.itens = Arrays.asList(new Item("Produto", 100, 1, 0.5));
        request.modalidadeEntrega = ModalidadeEntrega.ECONOMICA;
        request.formaPagamento = FormaPagamento.PIX;
        request.parcelas = 3;
        request.nivelClube = NivelClube.BRONZE;
        request.regiao = Regiao.SUDESTE;

        ErroResponse response = (ErroResponse) service.calcular(request);
        assertEquals("PARCELAMENTO_INVALIDO", response.erro);
    }

    @Test
    public void formaPagamentoIndisponivel_boletoacima1000() {
        RessumoRequest request = new RessumoRequest();
        request.itens = Arrays.asList(
            new Item("Produto 1", 500, 1, 0.5),
            new Item("Produto 2", 501, 1, 0.5)
        );
        request.modalidadeEntrega = ModalidadeEntrega.ECONOMICA;
        request.formaPagamento = FormaPagamento.BOLETO;
        request.nivelClube = NivelClube.BRONZE;
        request.regiao = Regiao.SUDESTE;

        ErroResponse response = (ErroResponse) service.calcular(request);
        assertEquals("FORMA_PAGAMENTO_INDISPONIVEL", response.erro);
    }
}

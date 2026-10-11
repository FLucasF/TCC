package com.loja.checkout.service;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

import com.loja.checkout.model.*;

public class RessumoServiceTest {
    private RessumoService service;

    @BeforeEach
    public void setup() {
        service = new RessumoService();
    }

    @Test
    public void exemplo1() {
        RessumoRequest request = new RessumoRequest();
        request.itens = Arrays.asList(
            new Item("Camiseta", 79.90, 2, 0.30),
            new Item("Tênis", 249.90, 1, 1.20)
        );
        request.modalidadeEntrega = ModalidadeEntrega.EXPRESSA;
        request.cupom = "BEMVINDO10";
        request.formaPagamento = FormaPagamento.PIX;
        request.parcelas = 1;
        request.nivelClube = NivelClube.BRONZE;
        request.regiao = Regiao.NORTE;

        RessumoResponse response = (RessumoResponse) service.calcular(request);

        assertEquals("409.70", response.subtotalProdutos.toPlainString());
        assertEquals("40.97", response.descontoCupom.toPlainString());
        assertEquals("33.10", response.frete.toPlainString());
        assertEquals(2, response.prazoEntregaDias);
        assertEquals("10.24", response.seguro.toPlainString());
        assertEquals("-20.60", response.ajustePagamento.toPlainString());
        assertEquals("391.47", response.totalFinal.toPlainString());
        assertEquals(1, response.parcelas);
        assertEquals("391.47", response.valorParcela.toPlainString());
        assertEquals("0.00", response.creditoProximaCompra.toPlainString());
        assertFalse(response.brinde);
    }

    @Test
    public void exemplo2() {
        RessumoRequest request = new RessumoRequest();
        request.itens = Arrays.asList(
            new Item("Camiseta", 79.90, 2, 0.30),
            new Item("Tênis", 249.90, 1, 1.20)
        );
        request.modalidadeEntrega = ModalidadeEntrega.ECONOMICA;
        request.cupom = null;
        request.formaPagamento = FormaPagamento.CARTAO;
        request.parcelas = 6;
        request.nivelClube = NivelClube.PRATA;
        request.regiao = Regiao.CENTRO_OESTE;

        RessumoResponse response = (RessumoResponse) service.calcular(request);

        assertEquals("409.70", response.subtotalProdutos.toPlainString());
        assertEquals("0.00", response.descontoCupom.toPlainString());
        assertEquals("15.60", response.frete.toPlainString());
        assertEquals(7, response.prazoEntregaDias);
        assertEquals("6.15", response.seguro.toPlainString());
        assertEquals(6, response.parcelas);
        assertEquals("8.19", response.creditoProximaCompra.toPlainString());
        assertFalse(response.brinde);
    }

    @Test
    public void exemplo3() {
        RessumoRequest request = new RessumoRequest();
        request.itens = Arrays.asList(
            new Item("Fone", 199.90, 2, 0.25)
        );
        request.modalidadeEntrega = ModalidadeEntrega.MOTOBOY;
        request.cupom = "MENOS50";
        request.formaPagamento = FormaPagamento.BOLETO;
        request.parcelas = 1;
        request.nivelClube = NivelClube.BRONZE;
        request.regiao = Regiao.NORDESTE;

        RessumoResponse response = (RessumoResponse) service.calcular(request);

        assertEquals("399.80", response.subtotalProdutos.toPlainString());
        assertEquals("50.00", response.descontoCupom.toPlainString());
        assertEquals("18.00", response.frete.toPlainString());
        assertEquals(0, response.prazoEntregaDias);
        assertEquals("8.00", response.seguro.toPlainString());
        assertEquals("3.49", response.ajustePagamento.toPlainString());
        assertEquals("379.29", response.totalFinal.toPlainString());
        assertEquals(1, response.parcelas);
        assertEquals("379.29", response.valorParcela.toPlainString());
        assertEquals("0.00", response.creditoProximaCompra.toPlainString());
        assertFalse(response.brinde);
    }

    @Test
    public void exemplo4() {
        RessumoRequest request = new RessumoRequest();
        request.itens = Arrays.asList(
            new Item("Meia", 19.90, 7, 0.10),
            new Item("Camiseta", 79.90, 2, 0.30)
        );
        request.modalidadeEntrega = ModalidadeEntrega.RETIRADA_LOJA;
        request.cupom = "LEVE3PAGUE2";
        request.formaPagamento = FormaPagamento.CARTAO;
        request.parcelas = 3;
        request.nivelClube = NivelClube.PRATA;
        request.regiao = Regiao.SUL;

        RessumoResponse response = (RessumoResponse) service.calcular(request);

        assertEquals("299.10", response.subtotalProdutos.toPlainString());
        assertEquals("39.80", response.descontoCupom.toPlainString());
        assertEquals("0.00", response.frete.toPlainString());
        assertEquals(1, response.prazoEntregaDias);
        assertEquals("2.99", response.seguro.toPlainString());
        assertEquals("0.00", response.ajustePagamento.toPlainString());
        assertEquals("262.29", response.totalFinal.toPlainString());
        assertEquals(3, response.parcelas);
        assertEquals("87.43", response.valorParcela.toPlainString());
        assertEquals("5.98", response.creditoProximaCompra.toPlainString());
        assertFalse(response.brinde);
    }

    @Test
    public void exemplo5() {
        RessumoRequest request = new RessumoRequest();
        request.itens = Arrays.asList(
            new Item("Camiseta", 79.90, 2, 0.30),
            new Item("Tênis", 249.90, 1, 1.20)
        );
        request.modalidadeEntrega = ModalidadeEntrega.EXPRESSA;
        request.cupom = null;
        request.formaPagamento = FormaPagamento.PIX;
        request.parcelas = 1;
        request.nivelClube = NivelClube.OURO;
        request.regiao = Regiao.SUDESTE;

        RessumoResponse response = (RessumoResponse) service.calcular(request);

        assertEquals("409.70", response.subtotalProdutos.toPlainString());
        assertEquals("0.00", response.descontoCupom.toPlainString());
        assertEquals("0.00", response.frete.toPlainString());
        assertEquals(2, response.prazoEntregaDias);
        assertEquals("4.10", response.seguro.toPlainString());
        assertEquals("-20.69", response.ajustePagamento.toPlainString());
        assertEquals("393.11", response.totalFinal.toPlainString());
        assertEquals(1, response.parcelas);
        assertEquals("393.11", response.valorParcela.toPlainString());
        assertEquals("20.48", response.creditoProximaCompra.toPlainString());
        assertFalse(response.brinde);
    }
}

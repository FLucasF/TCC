package com.loja.checkout.servico;

import com.loja.checkout.dto.ItemCarrinho;
import com.loja.checkout.dto.RequisicaoResumo;
import com.loja.checkout.dto.RespostaResumo;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

public class ResumoCheckoutServiceTest {
    private final ResumoCheckoutService service = new ResumoCheckoutService();

    @Test
    public void exemplo1() {
        RequisicaoResumo req = new RequisicaoResumo();
        req.itens = new ArrayList<>();
        req.itens.add(new ItemCarrinho("Camiseta", 79.90, 2, 0.30));
        req.itens.add(new ItemCarrinho("Tênis", 249.90, 1, 1.20));
        req.modalidadeEntrega = "EXPRESSA";
        req.cupom = "BEMVINDO10";
        req.formaPagamento = "PIX";
        req.parcelas = 1;
        req.nivelClube = "BRONZE";
        req.regiao = "NORTE";

        RespostaResumo resposta = service.calcular(req);

        assertEquals(new BigDecimal("409.70"), resposta.subtotalProdutos);
        assertEquals(new BigDecimal("40.97"), resposta.descontoCupom);
        assertEquals(new BigDecimal("33.10"), resposta.frete);
        assertEquals(2, resposta.prazoEntregaDias);
        assertEquals(new BigDecimal("10.24"), resposta.seguro);
        assertEquals(new BigDecimal("-20.60"), resposta.ajustePagamento);
        assertEquals(new BigDecimal("391.47"), resposta.totalFinal);
        assertEquals(1, resposta.parcelas);
        assertEquals(new BigDecimal("391.47"), resposta.valorParcela);
        assertEquals(new BigDecimal("0.00"), resposta.creditoProximaCompra);
        assertEquals(false, resposta.brinde);
    }

    @Test
    public void exemplo2() {
        RequisicaoResumo req = new RequisicaoResumo();
        req.itens = new ArrayList<>();
        req.itens.add(new ItemCarrinho("Camiseta", 79.90, 2, 0.30));
        req.itens.add(new ItemCarrinho("Tênis", 249.90, 1, 1.20));
        req.modalidadeEntrega = "ECONOMICA";
        req.cupom = null;
        req.formaPagamento = "CARTAO";
        req.parcelas = 6;
        req.nivelClube = "PRATA";
        req.regiao = "CENTRO_OESTE";

        RespostaResumo resposta = service.calcular(req);

        assertEquals(new BigDecimal("409.70"), resposta.subtotalProdutos);
        assertEquals(new BigDecimal("0.00"), resposta.descontoCupom);
        assertEquals(new BigDecimal("15.60"), resposta.frete);
        assertEquals(7, resposta.prazoEntregaDias);
        assertEquals(new BigDecimal("6.15"), resposta.seguro);
        assertEquals(new BigDecimal("30.55"), resposta.ajustePagamento);
        assertEquals(new BigDecimal("462.00"), resposta.totalFinal);
        assertEquals(6, resposta.parcelas);
        assertEquals(new BigDecimal("77.00"), resposta.valorParcela);
        assertEquals(new BigDecimal("8.19"), resposta.creditoProximaCompra);
        assertEquals(false, resposta.brinde);
    }

    @Test
    public void exemplo3() {
        RequisicaoResumo req = new RequisicaoResumo();
        req.itens = new ArrayList<>();
        req.itens.add(new ItemCarrinho("Fone", 199.90, 2, 0.25));
        req.modalidadeEntrega = "MOTOBOY";
        req.cupom = "MENOS50";
        req.formaPagamento = "BOLETO";
        req.parcelas = 1;
        req.nivelClube = "BRONZE";
        req.regiao = "NORDESTE";

        RespostaResumo resposta = service.calcular(req);

        assertEquals(new BigDecimal("399.80"), resposta.subtotalProdutos);
        assertEquals(new BigDecimal("50.00"), resposta.descontoCupom);
        assertEquals(new BigDecimal("18.00"), resposta.frete);
        assertEquals(0, resposta.prazoEntregaDias);
        assertEquals(new BigDecimal("8.00"), resposta.seguro);
        assertEquals(new BigDecimal("3.49"), resposta.ajustePagamento);
        assertEquals(new BigDecimal("379.29"), resposta.totalFinal);
        assertEquals(1, resposta.parcelas);
        assertEquals(new BigDecimal("379.29"), resposta.valorParcela);
        assertEquals(new BigDecimal("0.00"), resposta.creditoProximaCompra);
        assertEquals(false, resposta.brinde);
    }

    @Test
    public void exemplo4() {
        RequisicaoResumo req = new RequisicaoResumo();
        req.itens = new ArrayList<>();
        req.itens.add(new ItemCarrinho("Meia", 19.90, 7, 0.10));
        req.itens.add(new ItemCarrinho("Camiseta", 79.90, 2, 0.30));
        req.modalidadeEntrega = "RETIRADA_LOJA";
        req.cupom = "LEVE3PAGUE2";
        req.formaPagamento = "CARTAO";
        req.parcelas = 3;
        req.nivelClube = "PRATA";
        req.regiao = "SUL";

        RespostaResumo resposta = service.calcular(req);

        assertEquals(new BigDecimal("299.10"), resposta.subtotalProdutos);
        assertEquals(new BigDecimal("39.80"), resposta.descontoCupom);
        assertEquals(new BigDecimal("0.00"), resposta.frete);
        assertEquals(1, resposta.prazoEntregaDias);
        assertEquals(new BigDecimal("2.99"), resposta.seguro);
        assertEquals(new BigDecimal("0.00"), resposta.ajustePagamento);
        assertEquals(new BigDecimal("262.29"), resposta.totalFinal);
        assertEquals(3, resposta.parcelas);
        assertEquals(new BigDecimal("87.43"), resposta.valorParcela);
        assertEquals(new BigDecimal("5.98"), resposta.creditoProximaCompra);
        assertEquals(false, resposta.brinde);
    }

    @Test
    public void exemplo5() {
        RequisicaoResumo req = new RequisicaoResumo();
        req.itens = new ArrayList<>();
        req.itens.add(new ItemCarrinho("Camiseta", 79.90, 2, 0.30));
        req.itens.add(new ItemCarrinho("Tênis", 249.90, 1, 1.20));
        req.modalidadeEntrega = "EXPRESSA";
        req.cupom = null;
        req.formaPagamento = "PIX";
        req.parcelas = 1;
        req.nivelClube = "OURO";
        req.regiao = "SUDESTE";

        RespostaResumo resposta = service.calcular(req);

        assertEquals(new BigDecimal("409.70"), resposta.subtotalProdutos);
        assertEquals(new BigDecimal("0.00"), resposta.descontoCupom);
        assertEquals(new BigDecimal("0.00"), resposta.frete);
        assertEquals(2, resposta.prazoEntregaDias);
        assertEquals(new BigDecimal("4.10"), resposta.seguro);
        assertEquals(new BigDecimal("-20.69"), resposta.ajustePagamento);
        assertEquals(new BigDecimal("393.11"), resposta.totalFinal);
        assertEquals(1, resposta.parcelas);
        assertEquals(new BigDecimal("393.11"), resposta.valorParcela);
        assertEquals(new BigDecimal("20.48"), resposta.creditoProximaCompra);
        assertEquals(false, resposta.brinde);
    }
}

package com.loja.checkout.integration;

import com.loja.checkout.api.ItemRequisicao;
import com.loja.checkout.api.RequisicaoCheckout;
import com.loja.checkout.api.RespostaCheckout;
import com.loja.checkout.exceptions.ErroCheckout;
import com.loja.checkout.service.ServicoCheckout;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

public class TestIntegracaoAPI {

    private ServicoCheckout servico = new ServicoCheckout();

    @Test
    public void testIntegracaoCompleta() throws ErroCheckout {
        RequisicaoCheckout requisicao = new RequisicaoCheckout();
        requisicao.itens = new ArrayList<>();

        ItemRequisicao item1 = new ItemRequisicao();
        item1.nome = "Camiseta";
        item1.precoUnitario = new BigDecimal("79.90");
        item1.quantidade = 2;
        item1.pesoKg = new BigDecimal("0.30");
        requisicao.itens.add(item1);

        ItemRequisicao item2 = new ItemRequisicao();
        item2.nome = "Tênis";
        item2.precoUnitario = new BigDecimal("249.90");
        item2.quantidade = 1;
        item2.pesoKg = new BigDecimal("1.20");
        requisicao.itens.add(item2);

        requisicao.modalidadeEntrega = "EXPRESSA";
        requisicao.cupom = "BEMVINDO10";
        requisicao.formaPagamento = "PIX";
        requisicao.parcelas = 1;

        RespostaCheckout resposta = servico.calcularResumo(requisicao);

        assertNotNull(resposta);
        assertEquals(0, new BigDecimal("409.70").compareTo(resposta.subtotalProdutos));
        assertEquals(0, new BigDecimal("40.97").compareTo(resposta.descontoCupom));
        assertEquals(0, new BigDecimal("33.10").compareTo(resposta.frete));
        assertEquals(2, resposta.prazoEntregaDias);
        assertEquals(1, resposta.parcelas);
        assertEquals(0, new BigDecimal("381.74").compareTo(resposta.totalFinal));

        System.out.println("✓ Integração API testada com sucesso");
    }

    @Test
    public void testErroValidacao() {
        RequisicaoCheckout requisicao = new RequisicaoCheckout();
        requisicao.itens = new ArrayList<>();

        ItemRequisicao item = new ItemRequisicao();
        item.nome = "Produto";
        item.precoUnitario = new BigDecimal("10.00");
        item.quantidade = 1;
        item.pesoKg = new BigDecimal("0.10");
        requisicao.itens.add(item);

        requisicao.modalidadeEntrega = "MOTOBOY";
        requisicao.cupom = null;
        requisicao.formaPagamento = "PIX";

        // Este pedido tem 0.1 kg, então é válido para motoboy
        try {
            RespostaCheckout resposta = servico.calcularResumo(requisicao);
            assertNotNull(resposta);
            System.out.println("✓ Validação de pedido válido passada");
        } catch (ErroCheckout e) {
            fail("Não deveria ter lançado erro para um pedido válido");
        }
    }

    @Test
    public void testMotoboy5kg() {
        RequisicaoCheckout requisicao = new RequisicaoCheckout();
        requisicao.itens = new ArrayList<>();

        ItemRequisicao item = new ItemRequisicao();
        item.nome = "Produto pesado";
        item.precoUnitario = new BigDecimal("100.00");
        item.quantidade = 1;
        item.pesoKg = new BigDecimal("6.00");
        requisicao.itens.add(item);

        requisicao.modalidadeEntrega = "MOTOBOY";
        requisicao.formaPagamento = "PIX";

        assertThrows(ErroCheckout.class, () -> servico.calcularResumo(requisicao));
        System.out.println("✓ Validação de peso motoboy passou");
    }

    @Test
    public void testCupomInvalido() {
        RequisicaoCheckout requisicao = new RequisicaoCheckout();
        requisicao.itens = new ArrayList<>();

        ItemRequisicao item = new ItemRequisicao();
        item.nome = "Produto";
        item.precoUnitario = new BigDecimal("100.00");
        item.quantidade = 1;
        item.pesoKg = new BigDecimal("0.10");
        requisicao.itens.add(item);

        requisicao.modalidadeEntrega = "RETIRADA_LOJA";
        requisicao.cupom = "CUPOM_INEXISTENTE";
        requisicao.formaPagamento = "PIX";

        assertThrows(ErroCheckout.class, () -> servico.calcularResumo(requisicao));
        System.out.println("✓ Validação de cupom inválido passou");
    }
}

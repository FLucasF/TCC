package br.tcc.checkout.dominio;

import br.tcc.checkout.dto.ItemPedido;
import br.tcc.checkout.dto.RequisicaoResumo;
import br.tcc.checkout.dto.RespostaResumo;
import br.tcc.checkout.exception.ErroCheckout;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class ServicoCheckoutTest {
    private final ServicoCheckout servico = ServicoCheckout.criar();

    @Test
    void exemplo1_BEMVINDO10_PIX() {
        RequisicaoResumo requisicao = new RequisicaoResumo();
        requisicao.setItens(Arrays.asList(
            new ItemPedido("Camiseta", 79.90, 2, 0.30),
            new ItemPedido("Tênis", 249.90, 1, 1.20)
        ));
        requisicao.setModalidadeEntrega("EXPRESSA");
        requisicao.setCupom("BEMVINDO10");
        requisicao.setFormaPagamento("PIX");
        requisicao.setParcelas(1);

        RespostaResumo resposta = servico.calcularResumo(requisicao);

        assertEquals(new BigDecimal("409.70"), resposta.getSubtotalProdutos());
        assertEquals(new BigDecimal("40.97"), resposta.getDescontoCupom());
        assertEquals(new BigDecimal("33.10"), resposta.getFrete());
        assertEquals(2, resposta.getPrazoEntregaDias());
        assertEquals(new BigDecimal("-20.09"), resposta.getAjustePagamento());
        assertEquals(new BigDecimal("381.74"), resposta.getTotalFinal());
        assertEquals(1, resposta.getParcelas());
        assertEquals(new BigDecimal("381.74"), resposta.getValorParcela());
    }

    @Test
    void exemplo2_ECONOMICA_CARTAO_6x() {
        RequisicaoResumo requisicao = new RequisicaoResumo();
        requisicao.setItens(Arrays.asList(
            new ItemPedido("Camiseta", 79.90, 2, 0.30),
            new ItemPedido("Tênis", 249.90, 1, 1.20)
        ));
        requisicao.setModalidadeEntrega("ECONOMICA");
        requisicao.setCupom(null);
        requisicao.setFormaPagamento("CARTAO");
        requisicao.setParcelas(6);

        RespostaResumo resposta = servico.calcularResumo(requisicao);

        assertEquals(new BigDecimal("409.70"), resposta.getSubtotalProdutos());
        assertEquals(0, resposta.getDescontoCupom().compareTo(BigDecimal.ZERO));
        assertEquals(new BigDecimal("15.60"), resposta.getFrete());
        assertEquals(7, resposta.getPrazoEntregaDias());
        assertEquals(new BigDecimal("30.10"), resposta.getAjustePagamento());
        assertEquals(new BigDecimal("455.40"), resposta.getTotalFinal());
        assertEquals(6, resposta.getParcelas());
        assertEquals(new BigDecimal("75.90"), resposta.getValorParcela());
    }

    @Test
    void exemplo3_MENOS50_MOTOBOY_BOLETO() {
        RequisicaoResumo requisicao = new RequisicaoResumo();
        requisicao.setItens(Arrays.asList(
            new ItemPedido("Fone", 199.90, 2, 0.25)
        ));
        requisicao.setModalidadeEntrega("MOTOBOY");
        requisicao.setCupom("MENOS50");
        requisicao.setFormaPagamento("BOLETO");
        requisicao.setParcelas(1);

        RespostaResumo resposta = servico.calcularResumo(requisicao);

        assertEquals(new BigDecimal("399.80"), resposta.getSubtotalProdutos());
        assertEquals(new BigDecimal("50.00"), resposta.getDescontoCupom());
        assertEquals(new BigDecimal("18.00"), resposta.getFrete());
        assertEquals(0, resposta.getPrazoEntregaDias());
        assertEquals(new BigDecimal("3.49"), resposta.getAjustePagamento());
        assertEquals(new BigDecimal("371.29"), resposta.getTotalFinal());
        assertEquals(1, resposta.getParcelas());
        assertEquals(new BigDecimal("371.29"), resposta.getValorParcela());
    }

    @Test
    void exemplo4_LEVE3PAGUE2_RETIRADA_CARTAO_3x() {
        RequisicaoResumo requisicao = new RequisicaoResumo();
        requisicao.setItens(Arrays.asList(
            new ItemPedido("Meia", 19.90, 7, 0.10),
            new ItemPedido("Camiseta", 79.90, 2, 0.30)
        ));
        requisicao.setModalidadeEntrega("RETIRADA_LOJA");
        requisicao.setCupom("LEVE3PAGUE2");
        requisicao.setFormaPagamento("CARTAO");
        requisicao.setParcelas(3);

        RespostaResumo resposta = servico.calcularResumo(requisicao);

        assertEquals(new BigDecimal("299.10"), resposta.getSubtotalProdutos());
        assertEquals(new BigDecimal("39.80"), resposta.getDescontoCupom());
        assertEquals(0, resposta.getFrete().compareTo(BigDecimal.ZERO));
        assertEquals(1, resposta.getPrazoEntregaDias());
        assertEquals(0, resposta.getAjustePagamento().compareTo(BigDecimal.ZERO));
        assertEquals(new BigDecimal("259.30"), resposta.getTotalFinal());
        assertEquals(3, resposta.getParcelas());
        assertEquals(new BigDecimal("86.43"), resposta.getValorParcela());
    }

    @Test
    void deveLancarErroParceladoInvalidoNoCartao() {
        RequisicaoResumo requisicao = new RequisicaoResumo();
        requisicao.setItens(Arrays.asList(
            new ItemPedido("Item", 100.00, 1, 0.10)
        ));
        requisicao.setModalidadeEntrega("RETIRADA_LOJA");
        requisicao.setFormaPagamento("CARTAO");
        requisicao.setParcelas(13);

        ErroCheckout erro = assertThrows(ErroCheckout.class, () -> servico.calcularResumo(requisicao));
        assertEquals("PARCELAMENTO_INVALIDO", erro.getCodigo());
    }
}

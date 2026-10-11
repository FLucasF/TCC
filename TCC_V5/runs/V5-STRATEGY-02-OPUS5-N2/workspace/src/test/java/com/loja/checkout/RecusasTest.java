package com.loja.checkout;

import com.loja.checkout.aplicacao.CalculadoraResumo;
import com.loja.checkout.aplicacao.ItemRecebido;
import com.loja.checkout.aplicacao.PedidoRecebido;
import com.loja.checkout.dominio.CodigoErro;
import com.loja.checkout.dominio.PedidoRecusado;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static com.loja.checkout.PedidoDeTeste.item;
import static com.loja.checkout.PedidoDeTeste.um;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

class RecusasTest {

    private final CalculadoraResumo calculadora = new CalculadoraResumo();

    @Test
    void carrinho_vazio() {
        recusa(um().semItens(), CodigoErro.PEDIDO_INVALIDO);
    }

    @Test
    void itens_ausentes() {
        PedidoRecusado recusa = assertThrows(PedidoRecusado.class, () -> calculadora.calcular(
                new PedidoRecebido(null, "ECONOMICA", null, "PIX", null, "BRONZE", "SUDESTE")));

        assertThat(recusa.codigo()).isEqualTo(CodigoErro.PEDIDO_INVALIDO);
    }

    @Test
    void item_com_preco_ausente() {
        recusa(um().com(new ItemRecebido("Camiseta", null, 2, new BigDecimal("0.30"))),
                CodigoErro.PEDIDO_INVALIDO);
    }

    @Test
    void item_com_quantidade_zerada() {
        recusa(um().com(item("Camiseta", "79.90", 0, "0.30")), CodigoErro.PEDIDO_INVALIDO);
    }

    @Test
    void item_com_peso_negativo() {
        recusa(um().com(item("Camiseta", "79.90", 2, "-0.30")), CodigoErro.PEDIDO_INVALIDO);
    }

    @Test
    void nivel_do_clube_que_nao_existe() {
        recusa(um().clube("DIAMANTE"), CodigoErro.NIVEL_CLUBE_INVALIDO);
    }

    @Test
    void nivel_do_clube_ausente() {
        recusa(um().clube(null), CodigoErro.NIVEL_CLUBE_INVALIDO);
    }

    @Test
    void regiao_que_nao_existe() {
        recusa(um().regiao("SUDESTE_LITORAL"), CodigoErro.REGIAO_INVALIDA);
    }

    @Test
    void regiao_ausente() {
        recusa(um().regiao(null), CodigoErro.REGIAO_INVALIDA);
    }

    @Test
    void modalidade_que_nao_existe() {
        recusa(um().entrega("DRONE"), CodigoErro.MODALIDADE_INVALIDA);
    }

    @Test
    void modalidade_ausente() {
        recusa(um().entrega(null), CodigoErro.MODALIDADE_INVALIDA);
    }

    @Test
    void motoboy_acima_de_cinco_quilos() {
        recusa(um().com(item("Mochila", "100.00", 3, "2.00")).entrega("MOTOBOY"),
                CodigoErro.MODALIDADE_INDISPONIVEL);
    }

    @Test
    void cupom_que_nao_existe() {
        recusa(um().cupom("BEMVINDO99"), CodigoErro.CUPOM_INVALIDO);
    }

    @Test
    void menos50_abaixo_de_trezentos() {
        recusa(um().com(item("Casaco", "299.99", 1, "0.80")).cupom("MENOS50"),
                CodigoErro.CUPOM_NAO_APLICAVEL);
    }

    @Test
    void forma_de_pagamento_que_nao_existe() {
        recusa(um().pagamento("CRIPTO"), CodigoErro.FORMA_PAGAMENTO_INVALIDA);
    }

    @Test
    void forma_de_pagamento_ausente() {
        recusa(um().pagamento(null), CodigoErro.FORMA_PAGAMENTO_INVALIDA);
    }

    @Test
    void pix_parcelado() {
        recusa(um().pagamento("PIX").parcelas(2), CodigoErro.PARCELAMENTO_INVALIDO);
    }

    @Test
    void boleto_parcelado() {
        recusa(um().pagamento("BOLETO").parcelas(2), CodigoErro.PARCELAMENTO_INVALIDO);
    }

    @Test
    void cartao_acima_de_doze_vezes() {
        recusa(um().pagamento("CARTAO").parcelas(13), CodigoErro.PARCELAMENTO_INVALIDO);
    }

    @Test
    void cartao_em_zero_vezes() {
        recusa(um().pagamento("CARTAO").parcelas(0), CodigoErro.PARCELAMENTO_INVALIDO);
    }

    @Test
    void boleto_acima_de_mil_reais() {
        recusa(um().com(item("Notebook", "1200.00", 1, "2.00")).entrega("RETIRADA_LOJA").pagamento("BOLETO"),
                CodigoErro.FORMA_PAGAMENTO_INDISPONIVEL);
    }

    @Test
    void confere_na_ordem_do_enunciado() {
        PedidoDeTeste tudoErrado = um()
                .com(item("Mochila", "90.00", 3, "2.00"))
                .entrega("MOTOBOY").cupom("BEMVINDO99").pagamento("CRIPTO").clube("DIAMANTE").regiao("MARTE");

        recusa(tudoErrado, CodigoErro.NIVEL_CLUBE_INVALIDO);
        recusa(tudoErrado.clube("BRONZE"), CodigoErro.REGIAO_INVALIDA);
        recusa(tudoErrado.regiao("SUDESTE"), CodigoErro.MODALIDADE_INDISPONIVEL);
        recusa(tudoErrado.entrega("ECONOMICA"), CodigoErro.CUPOM_INVALIDO);
        recusa(tudoErrado.cupom("MENOS50"), CodigoErro.CUPOM_NAO_APLICAVEL);
        recusa(tudoErrado.com(item("Notebook", "1200.00", 1, "2.00")), CodigoErro.FORMA_PAGAMENTO_INVALIDA);
        recusa(tudoErrado.pagamento("BOLETO").parcelas(2), CodigoErro.PARCELAMENTO_INVALIDO);
        recusa(tudoErrado.parcelas(1), CodigoErro.FORMA_PAGAMENTO_INDISPONIVEL);
    }

    @Test
    void itens_invalidos_vem_antes_de_tudo() {
        recusa(um().semItens().clube("DIAMANTE").regiao("MARTE").entrega("DRONE").pagamento("CRIPTO"),
                CodigoErro.PEDIDO_INVALIDO);
    }

    private void recusa(PedidoDeTeste pedido, CodigoErro esperado) {
        PedidoRecusado recusa = assertThrows(PedidoRecusado.class, () -> calculadora.calcular(pedido.pedido()));

        assertThat(recusa.codigo()).isEqualTo(esperado);
    }
}

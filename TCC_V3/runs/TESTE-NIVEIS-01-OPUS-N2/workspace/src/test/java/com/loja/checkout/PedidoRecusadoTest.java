package com.loja.checkout;

import static com.loja.checkout.ResumoDaCompraTest.item;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.loja.checkout.api.ItemRequest;
import com.loja.checkout.api.PedidoRecusadoException;
import com.loja.checkout.api.ResumoRequest;
import com.loja.checkout.aplicacao.CalculadoraDoResumo;
import com.loja.checkout.dominio.CodigoErro;
import java.math.BigDecimal;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

/** Os pedidos que a loja recusa, e a ordem em que os problemas sao conferidos. */
@SpringBootTest
class PedidoRecusadoTest {

    private static final ItemRequest CAMISETA = item("Camiseta", "79.90", 2, "0.30");

    @Autowired
    private CalculadoraDoResumo calculadora;

    @Test
    void carrinho_vazio() {
        recusa(pedido(List.of(), "EXPRESSA", null, "PIX", 1, "BRONZE", "SUDESTE"),
                CodigoErro.PEDIDO_INVALIDO);
    }

    @Test
    void carrinho_ausente() {
        recusa(pedido(null, "EXPRESSA", null, "PIX", 1, "BRONZE", "SUDESTE"),
                CodigoErro.PEDIDO_INVALIDO);
    }

    @Test
    void item_sem_peso() {
        ItemRequest semPeso = new ItemRequest("Camiseta", new BigDecimal("79.90"), 2, null);
        recusa(pedido(List.of(semPeso), "EXPRESSA", null, "PIX", 1, "BRONZE", "SUDESTE"),
                CodigoErro.PEDIDO_INVALIDO);
    }

    @Test
    void item_com_quantidade_zerada() {
        recusa(pedido(List.of(item("Camiseta", "79.90", 0, "0.30")), "EXPRESSA", null, "PIX", 1,
                "BRONZE", "SUDESTE"), CodigoErro.PEDIDO_INVALIDO);
    }

    @Test
    void item_com_preco_negativo() {
        recusa(pedido(List.of(item("Camiseta", "-1.00", 2, "0.30")), "EXPRESSA", null, "PIX", 1,
                "BRONZE", "SUDESTE"), CodigoErro.PEDIDO_INVALIDO);
    }

    @Test
    void nivel_de_clube_que_nao_existe() {
        recusa(pedido(List.of(CAMISETA), "EXPRESSA", null, "PIX", 1, "DIAMANTE", "SUDESTE"),
                CodigoErro.NIVEL_CLUBE_INVALIDO);
    }

    @Test
    void nivel_de_clube_ausente() {
        recusa(pedido(List.of(CAMISETA), "EXPRESSA", null, "PIX", 1, null, "SUDESTE"),
                CodigoErro.NIVEL_CLUBE_INVALIDO);
    }

    @Test
    void regiao_que_nao_existe() {
        recusa(pedido(List.of(CAMISETA), "EXPRESSA", null, "PIX", 1, "BRONZE", "SUDESTE_LITORAL"),
                CodigoErro.REGIAO_INVALIDA);
    }

    @Test
    void modalidade_de_entrega_que_nao_existe() {
        recusa(pedido(List.of(CAMISETA), "DRONE", null, "PIX", 1, "BRONZE", "SUDESTE"),
                CodigoErro.MODALIDADE_INVALIDA);
    }

    @Test
    void motoboy_acima_de_cinco_quilos() {
        recusa(pedido(List.of(item("Mala", "250.00", 1, "5.01")), "MOTOBOY", null, "PIX", 1,
                "BRONZE", "SUDESTE"), CodigoErro.MODALIDADE_INDISPONIVEL);
    }

    @Test
    void motoboy_com_exatamente_cinco_quilos_e_aceito() {
        assertThat(calculadora.calcular(pedido(List.of(item("Mala", "250.00", 1, "5.00")),
                "MOTOBOY", null, "PIX", 1, "BRONZE", "SUDESTE")).frete())
                .isEqualByComparingTo("18.00");
    }

    @Test
    void cupom_que_nao_existe() {
        recusa(pedido(List.of(CAMISETA), "EXPRESSA", "PROMOCAOXYZ", "PIX", 1, "BRONZE", "SUDESTE"),
                CodigoErro.CUPOM_INVALIDO);
    }

    @Test
    void cupom_em_letras_minusculas_nao_existe() {
        recusa(pedido(List.of(CAMISETA), "EXPRESSA", "bemvindo10", "PIX", 1, "BRONZE", "SUDESTE"),
                CodigoErro.CUPOM_INVALIDO);
    }

    @Test
    void menos50_abaixo_do_minimo() {
        recusa(pedido(List.of(CAMISETA), "EXPRESSA", "MENOS50", "PIX", 1, "BRONZE", "SUDESTE"),
                CodigoErro.CUPOM_NAO_APLICAVEL);
    }

    @Test
    void forma_de_pagamento_que_nao_existe() {
        recusa(pedido(List.of(CAMISETA), "EXPRESSA", null, "CRIPTO", 1, "BRONZE", "SUDESTE"),
                CodigoErro.FORMA_PAGAMENTO_INVALIDA);
    }

    @Test
    void pix_parcelado() {
        recusa(pedido(List.of(CAMISETA), "EXPRESSA", null, "PIX", 2, "BRONZE", "SUDESTE"),
                CodigoErro.PARCELAMENTO_INVALIDO);
    }

    @Test
    void boleto_parcelado() {
        recusa(pedido(List.of(CAMISETA), "EXPRESSA", null, "BOLETO", 3, "BRONZE", "SUDESTE"),
                CodigoErro.PARCELAMENTO_INVALIDO);
    }

    @Test
    void cartao_acima_de_doze_vezes() {
        recusa(pedido(List.of(CAMISETA), "EXPRESSA", null, "CARTAO", 13, "BRONZE", "SUDESTE"),
                CodigoErro.PARCELAMENTO_INVALIDO);
    }

    @Test
    void boleto_acima_de_mil_reais() {
        recusa(pedido(List.of(item("Jaqueta", "999.00", 1, "1.00")), "EXPRESSA", null, "BOLETO", 1,
                "BRONZE", "SUDESTE"), CodigoErro.FORMA_PAGAMENTO_INDISPONIVEL);
    }

    @Test
    void confere_o_nivel_do_clube_antes_da_regiao() {
        recusa(pedido(List.of(CAMISETA), "DRONE", "PROMOCAOXYZ", "CRIPTO", 9, "DIAMANTE", "MARTE"),
                CodigoErro.NIVEL_CLUBE_INVALIDO);
    }

    @Test
    void confere_os_itens_antes_de_tudo() {
        recusa(pedido(List.of(), "DRONE", "PROMOCAOXYZ", "CRIPTO", 9, "DIAMANTE", "MARTE"),
                CodigoErro.PEDIDO_INVALIDO);
    }

    @Test
    void confere_a_entrega_antes_do_cupom() {
        recusa(pedido(List.of(item("Mala", "250.00", 1, "9.00")), "MOTOBOY", "PROMOCAOXYZ", "PIX", 1,
                "BRONZE", "SUDESTE"), CodigoErro.MODALIDADE_INDISPONIVEL);
    }

    private void recusa(ResumoRequest pedido, CodigoErro esperado) {
        assertThatThrownBy(() -> calculadora.calcular(pedido))
                .isInstanceOf(PedidoRecusadoException.class)
                .extracting(erro -> ((PedidoRecusadoException) erro).codigo())
                .isEqualTo(esperado);
    }

    private static ResumoRequest pedido(List<ItemRequest> itens, String entrega, String cupom,
            String pagamento, Integer parcelas, String nivel, String regiao) {
        return new ResumoRequest(itens, entrega, cupom, pagamento, parcelas, nivel, regiao);
    }
}

package com.loja.checkout;

import static org.assertj.core.api.Assertions.assertThat;

import com.loja.checkout.dominio.ErroCheckout;
import com.loja.checkout.dominio.ErroCheckoutException;
import com.loja.checkout.dominio.ResumoCompra;
import com.loja.checkout.web.ItemRequest;
import com.loja.checkout.web.ResumoRequest;
import java.math.BigDecimal;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
class CheckoutServiceTest {

    private static final ItemRequest CAMISETA = item("Camiseta", "79.90", 2, "0.30");
    private static final ItemRequest TENIS = item("Tenis", "249.90", 1, "1.20");

    @Autowired
    private CheckoutService servico;

    @Test
    void exemplo1_expressa_bemvindo10_pix() {
        ResumoCompra resumo = servico.calcular(new ResumoRequest(
                List.of(CAMISETA, TENIS), "EXPRESSA", "BEMVINDO10", "PIX", 1));

        assertThat(resumo).isEqualTo(resumoEsperado("409.70", "40.97", "33.10", 2,
                "-20.09", "381.74", 1, "381.74"));
    }

    @Test
    void exemplo2_economica_sem_cupom_cartao_6x() {
        ResumoCompra resumo = servico.calcular(new ResumoRequest(
                List.of(CAMISETA, TENIS), "ECONOMICA", null, "CARTAO", 6));

        assertThat(resumo).isEqualTo(resumoEsperado("409.70", "0.00", "15.60", 7,
                "30.10", "455.40", 6, "75.90"));
    }

    @Test
    void exemplo3_motoboy_menos50_boleto() {
        ResumoCompra resumo = servico.calcular(new ResumoRequest(
                List.of(item("Fone", "199.90", 2, "0.25")), "MOTOBOY", "MENOS50", "BOLETO", null));

        assertThat(resumo).isEqualTo(resumoEsperado("399.80", "50.00", "18.00", 0,
                "3.49", "371.29", 1, "371.29"));
    }

    @Test
    void exemplo4_retirada_leve3pague2_cartao_3x() {
        ResumoCompra resumo = servico.calcular(new ResumoRequest(
                List.of(item("Meia", "19.90", 7, "0.10"), CAMISETA), "RETIRADA_LOJA",
                "LEVE3PAGUE2", "CARTAO", 3));

        assertThat(resumo).isEqualTo(resumoEsperado("299.10", "39.80", "0.00", 1,
                "0.00", "259.30", 3, "86.43"));
    }

    @Test
    void fretegratis_desconta_exatamente_o_frete() {
        ResumoCompra resumo = servico.calcular(new ResumoRequest(
                List.of(CAMISETA, TENIS), "EXPRESSA", "FRETEGRATIS", "CARTAO", 1));

        assertThat(resumo.frete()).isEqualByComparingTo("33.10");
        assertThat(resumo.descontoCupom()).isEqualByComparingTo("33.10");
        assertThat(resumo.totalFinal()).isEqualByComparingTo("409.70");
        assertThat(resumo.ajustePagamento()).isEqualByComparingTo("0.00");
    }

    @Test
    void arredonda_meio_para_o_par() {
        // 2 x 14,975 = 29,95 nos produtos; 5% no Pix = 1,4975 -> 1,50 (par) no ajuste.
        ResumoCompra resumo = servico.calcular(new ResumoRequest(
                List.of(item("Brinco", "14.975", 2, "0.05")), "RETIRADA_LOJA", null, "PIX", null));

        assertThat(resumo.subtotalProdutos()).isEqualByComparingTo("29.95");
        assertThat(resumo.ajustePagamento()).isEqualByComparingTo("-1.50");
        assertThat(resumo.totalFinal()).isEqualByComparingTo("28.45");
    }

    @Test
    void erros_sao_verificados_na_ordem_combinada() {
        assertThat(erroDe(new ResumoRequest(List.of(), "NAO_EXISTE", "NAO_EXISTE", "NAO_EXISTE", 99)))
                .isEqualTo(ErroCheckout.PEDIDO_INVALIDO);
        assertThat(erroDe(new ResumoRequest(List.of(item("Meia", "19.90", 0, "0.10")),
                "ECONOMICA", null, "PIX", 1))).isEqualTo(ErroCheckout.PEDIDO_INVALIDO);
        assertThat(erroDe(new ResumoRequest(List.of(CAMISETA), null, null, "PIX", 1)))
                .isEqualTo(ErroCheckout.MODALIDADE_INVALIDA);
        assertThat(erroDe(new ResumoRequest(List.of(item("Halter", "300.00", 3, "3.00")),
                "MOTOBOY", null, "PIX", 1))).isEqualTo(ErroCheckout.MODALIDADE_INDISPONIVEL);
        assertThat(erroDe(new ResumoRequest(List.of(CAMISETA), "ECONOMICA", "bemvindo10", "PIX", 1)))
                .isEqualTo(ErroCheckout.CUPOM_INVALIDO);
        assertThat(erroDe(new ResumoRequest(List.of(CAMISETA), "ECONOMICA", "MENOS50", "PIX", 1)))
                .isEqualTo(ErroCheckout.CUPOM_NAO_APLICAVEL);
        assertThat(erroDe(new ResumoRequest(List.of(CAMISETA), "ECONOMICA", null, "DINHEIRO", 1)))
                .isEqualTo(ErroCheckout.FORMA_PAGAMENTO_INVALIDA);
        assertThat(erroDe(new ResumoRequest(List.of(CAMISETA), "ECONOMICA", null, "PIX", 2)))
                .isEqualTo(ErroCheckout.PARCELAMENTO_INVALIDO);
        assertThat(erroDe(new ResumoRequest(List.of(CAMISETA), "ECONOMICA", null, "CARTAO", 13)))
                .isEqualTo(ErroCheckout.PARCELAMENTO_INVALIDO);
        assertThat(erroDe(new ResumoRequest(List.of(item("Sofa", "600.00", 2, "1.00")),
                "RETIRADA_LOJA", null, "BOLETO", 1))).isEqualTo(ErroCheckout.FORMA_PAGAMENTO_INDISPONIVEL);
    }

    @Test
    void boleto_no_limite_de_mil_reais_e_aceito() {
        ResumoCompra resumo = servico.calcular(new ResumoRequest(
                List.of(item("Jaqueta", "500.00", 2, "1.00")), "RETIRADA_LOJA", null, "BOLETO", 1));

        assertThat(resumo.totalFinal()).isEqualByComparingTo("1003.49");
    }

    @Test
    void motoboy_no_limite_de_cinco_quilos_e_aceito() {
        ResumoCompra resumo = servico.calcular(new ResumoRequest(
                List.of(item("Halter", "100.00", 2, "2.50")), "MOTOBOY", null, "PIX", null));

        assertThat(resumo.frete()).isEqualByComparingTo("18.00");
        assertThat(resumo.prazoEntregaDias()).isZero();
    }

    private ErroCheckout erroDe(ResumoRequest requisicao) {
        try {
            servico.calcular(requisicao);
        } catch (ErroCheckoutException excecao) {
            return excecao.erro();
        }
        throw new AssertionError("esperava um erro de checkout");
    }

    private static ItemRequest item(String nome, String preco, int quantidade, String peso) {
        return new ItemRequest(nome, new BigDecimal(preco), quantidade, new BigDecimal(peso));
    }

    private static ResumoCompra resumoEsperado(String subtotal, String cupom, String frete, int prazo,
                                               String ajuste, String total, int parcelas, String parcela) {
        return new ResumoCompra(new BigDecimal(subtotal), new BigDecimal(cupom), new BigDecimal(frete),
                prazo, new BigDecimal(ajuste), new BigDecimal(total), parcelas, new BigDecimal(parcela));
    }
}

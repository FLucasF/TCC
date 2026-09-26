package com.loja.checkout.servico;

import com.loja.checkout.api.ErroNegocioException;
import com.loja.checkout.api.dto.ItemPedidoDto;
import com.loja.checkout.api.dto.ResumoRequest;
import com.loja.checkout.api.dto.ResumoResponse;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class CalculoResumoServiceTest {

    private final CalculoResumoService servico = new CalculoResumoService();

    private static ItemPedidoDto item(double preco, int quantidade, double pesoKg) {
        return new ItemPedidoDto("item", BigDecimal.valueOf(preco), quantidade, BigDecimal.valueOf(pesoKg));
    }

    @Test
    void exemplo5_completo_com_clube_ouro_e_regiao() {
        ResumoRequest request = new ResumoRequest(
                List.of(item(79.90, 2, 0.30), item(249.90, 1, 1.20)),
                "EXPRESSA",
                null,
                "PIX",
                1,
                "OURO",
                "SUDESTE"
        );

        ResumoResponse resposta = servico.calcular(request);

        assertThat(resposta.subtotalProdutos()).isEqualByComparingTo("409.70");
        assertThat(resposta.descontoCupom()).isEqualByComparingTo("0.00");
        assertThat(resposta.frete()).isEqualByComparingTo("0.00");
        assertThat(resposta.prazoEntregaDias()).isEqualTo(2);
        assertThat(resposta.imposto()).isEqualByComparingTo("49.16");
        assertThat(resposta.ajustePagamento()).isEqualByComparingTo("-22.94");
        assertThat(resposta.totalFinal()).isEqualByComparingTo("435.92");
        assertThat(resposta.parcelas()).isEqualTo(1);
        assertThat(resposta.valorParcela()).isEqualByComparingTo("435.92");
        assertThat(resposta.creditoProximaCompra()).isEqualByComparingTo("20.48");
        assertThat(resposta.brinde()).isFalse();
    }

    @Test
    void exemplo1_subtotal_cupom_frete_e_prazo() {
        ResumoRequest request = new ResumoRequest(
                List.of(item(79.90, 2, 0.30), item(249.90, 1, 1.20)),
                "EXPRESSA",
                "BEMVINDO10",
                "PIX",
                1,
                "BRONZE",
                "SUDESTE"
        );

        ResumoResponse resposta = servico.calcular(request);

        assertThat(resposta.subtotalProdutos()).isEqualByComparingTo("409.70");
        assertThat(resposta.descontoCupom()).isEqualByComparingTo("40.97");
        assertThat(resposta.frete()).isEqualByComparingTo("33.10");
        assertThat(resposta.prazoEntregaDias()).isEqualTo(2);
    }

    @Test
    void exemplo3_cupom_menos50_e_motoboy() {
        ResumoRequest request = new ResumoRequest(
                List.of(item(199.90, 2, 0.25)),
                "MOTOBOY",
                "MENOS50",
                "BOLETO",
                1,
                "BRONZE",
                "SUDESTE"
        );

        ResumoResponse resposta = servico.calcular(request);

        assertThat(resposta.subtotalProdutos()).isEqualByComparingTo("399.80");
        assertThat(resposta.descontoCupom()).isEqualByComparingTo("50.00");
        assertThat(resposta.frete()).isEqualByComparingTo("18.00");
        assertThat(resposta.prazoEntregaDias()).isEqualTo(0);
    }

    @Test
    void exemplo4_cupom_leve3pague2_e_retirada_loja() {
        ResumoRequest request = new ResumoRequest(
                List.of(item(19.90, 7, 0.10), item(79.90, 2, 0.30)),
                "RETIRADA_LOJA",
                "LEVE3PAGUE2",
                "CARTAO",
                3,
                "BRONZE",
                "SUDESTE"
        );

        ResumoResponse resposta = servico.calcular(request);

        assertThat(resposta.subtotalProdutos()).isEqualByComparingTo("299.10");
        assertThat(resposta.descontoCupom()).isEqualByComparingTo("39.80");
        assertThat(resposta.frete()).isEqualByComparingTo("0.00");
        assertThat(resposta.prazoEntregaDias()).isEqualTo(1);
    }

    @Test
    void motoboy_indisponivel_acima_de_5kg() {
        ResumoRequest request = new ResumoRequest(
                List.of(item(100.0, 1, 6.0)),
                "MOTOBOY",
                null,
                "PIX",
                1,
                "BRONZE",
                "SUDESTE"
        );

        assertThatThrownBy(() -> servico.calcular(request))
                .isInstanceOf(ErroNegocioException.class)
                .satisfies(excecao -> assertThat(((ErroNegocioException) excecao).getCodigo())
                        .isEqualTo("MODALIDADE_INDISPONIVEL"));
    }

    @Test
    void carrinho_vazio_retorna_pedido_invalido() {
        ResumoRequest request = new ResumoRequest(
                List.of(),
                "ECONOMICA",
                null,
                "PIX",
                1,
                "BRONZE",
                "SUDESTE"
        );

        assertThatThrownBy(() -> servico.calcular(request))
                .isInstanceOf(ErroNegocioException.class)
                .satisfies(excecao -> assertThat(((ErroNegocioException) excecao).getCodigo())
                        .isEqualTo("PEDIDO_INVALIDO"));
    }

    @Test
    void cupom_menos50_abaixo_do_minimo_e_nao_aplicavel() {
        ResumoRequest request = new ResumoRequest(
                List.of(item(50.0, 1, 1.0)),
                "ECONOMICA",
                "MENOS50",
                "PIX",
                1,
                "BRONZE",
                "SUDESTE"
        );

        assertThatThrownBy(() -> servico.calcular(request))
                .isInstanceOf(ErroNegocioException.class)
                .satisfies(excecao -> assertThat(((ErroNegocioException) excecao).getCodigo())
                        .isEqualTo("CUPOM_NAO_APLICAVEL"));
    }

    @Test
    void boleto_acima_de_mil_e_indisponivel() {
        ResumoRequest request = new ResumoRequest(
                List.of(item(2000.0, 1, 1.0)),
                "RETIRADA_LOJA",
                null,
                "BOLETO",
                1,
                "BRONZE",
                "SUDESTE"
        );

        assertThatThrownBy(() -> servico.calcular(request))
                .isInstanceOf(ErroNegocioException.class)
                .satisfies(excecao -> assertThat(((ErroNegocioException) excecao).getCodigo())
                        .isEqualTo("FORMA_PAGAMENTO_INDISPONIVEL"));
    }

    @Test
    void parcelamento_invalido_para_pix_com_mais_de_1x() {
        ResumoRequest request = new ResumoRequest(
                List.of(item(100.0, 1, 1.0)),
                "RETIRADA_LOJA",
                null,
                "PIX",
                2,
                "BRONZE",
                "SUDESTE"
        );

        assertThatThrownBy(() -> servico.calcular(request))
                .isInstanceOf(ErroNegocioException.class)
                .satisfies(excecao -> assertThat(((ErroNegocioException) excecao).getCodigo())
                        .isEqualTo("PARCELAMENTO_INVALIDO"));
    }
}

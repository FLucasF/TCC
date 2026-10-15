package com.loja.checkout.servico;

import com.loja.checkout.dominio.clube.Bronze;
import com.loja.checkout.dominio.clube.NivelClubeRegistry;
import com.loja.checkout.dominio.clube.Ouro;
import com.loja.checkout.dominio.clube.Prata;
import com.loja.checkout.dominio.cupom.Bemvindo10;
import com.loja.checkout.dominio.cupom.CupomRegistry;
import com.loja.checkout.dominio.cupom.FreteGratis;
import com.loja.checkout.dominio.cupom.Leve3Pague2;
import com.loja.checkout.dominio.cupom.Menos50;
import com.loja.checkout.dominio.entrega.Economica;
import com.loja.checkout.dominio.entrega.Expressa;
import com.loja.checkout.dominio.entrega.ModalidadeEntregaRegistry;
import com.loja.checkout.dominio.entrega.Motoboy;
import com.loja.checkout.dominio.entrega.RetiradaLoja;
import com.loja.checkout.dominio.pagamento.Boleto;
import com.loja.checkout.dominio.pagamento.Cartao;
import com.loja.checkout.dominio.pagamento.FormaPagamentoRegistry;
import com.loja.checkout.dominio.pagamento.Pix;
import com.loja.checkout.erro.PedidoException;
import com.loja.checkout.web.dto.ItemRequest;
import com.loja.checkout.web.dto.ResumoRequest;
import com.loja.checkout.web.dto.ResumoResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ResumoCompraServiceTest {

    private ResumoCompraService service;

    @BeforeEach
    void setUp() {
        service = new ResumoCompraService(
                new ModalidadeEntregaRegistry(List.of(new Economica(), new Expressa(), new RetiradaLoja(), new Motoboy())),
                new CupomRegistry(List.of(new Bemvindo10(), new Menos50(), new FreteGratis(), new Leve3Pague2())),
                new NivelClubeRegistry(List.of(new Bronze(), new Prata(), new Ouro())),
                new FormaPagamentoRegistry(List.of(new Pix(), new Cartao(), new Boleto()))
        );
    }

    private static ItemRequest item(String nome, double preco, int quantidade, double pesoKg) {
        return new ItemRequest(nome, BigDecimal.valueOf(preco), quantidade, pesoKg);
    }

    @Test
    void exemplo1_expressaComBemvindo10Pix() {
        ResumoRequest request = new ResumoRequest(
                List.of(item("Camiseta", 79.90, 2, 0.30), item("Tenis", 249.90, 1, 1.20)),
                "EXPRESSA", "BEMVINDO10", "PIX", null, "BRONZE", "NORTE"
        );

        ResumoResponse resposta = service.calcular(request);

        assertThat(resposta.subtotalProdutos()).isEqualByComparingTo("409.70");
        assertThat(resposta.descontoCupom()).isEqualByComparingTo("40.97");
        assertThat(resposta.frete()).isEqualByComparingTo("33.10");
        assertThat(resposta.prazoEntregaDias()).isEqualTo(2);
        assertThat(resposta.seguro()).isEqualByComparingTo("10.24");
        assertThat(resposta.ajustePagamento()).isEqualByComparingTo("-20.60");
        assertThat(resposta.totalFinal()).isEqualByComparingTo("391.47");
        assertThat(resposta.parcelas()).isEqualTo(1);
        assertThat(resposta.valorParcela()).isEqualByComparingTo("391.47");
        assertThat(resposta.creditoProximaCompra()).isEqualByComparingTo("0.00");
        assertThat(resposta.brinde()).isFalse();
    }

    @Test
    void exemplo2_economicaSemCupomCartao6xPrata() {
        ResumoRequest request = new ResumoRequest(
                List.of(item("Camiseta", 79.90, 2, 0.30), item("Tenis", 249.90, 1, 1.20)),
                "ECONOMICA", null, "CARTAO", 6, "PRATA", "CENTRO_OESTE"
        );

        ResumoResponse resposta = service.calcular(request);

        assertThat(resposta.subtotalProdutos()).isEqualByComparingTo("409.70");
        assertThat(resposta.descontoCupom()).isEqualByComparingTo("0.00");
        assertThat(resposta.frete()).isEqualByComparingTo("15.60");
        assertThat(resposta.prazoEntregaDias()).isEqualTo(7);
        assertThat(resposta.seguro()).isEqualByComparingTo("6.15");
        assertThat(resposta.ajustePagamento()).isEqualByComparingTo("30.55");
        assertThat(resposta.totalFinal()).isEqualByComparingTo("462.00");
        assertThat(resposta.parcelas()).isEqualTo(6);
        assertThat(resposta.valorParcela()).isEqualByComparingTo("77.00");
        assertThat(resposta.creditoProximaCompra()).isEqualByComparingTo("8.19");
        assertThat(resposta.brinde()).isFalse();
    }

    @Test
    void exemplo3_motoboyComMenos50Boleto() {
        ResumoRequest request = new ResumoRequest(
                List.of(item("Fone", 199.90, 2, 0.25)),
                "MOTOBOY", "MENOS50", "BOLETO", null, "BRONZE", "NORDESTE"
        );

        ResumoResponse resposta = service.calcular(request);

        assertThat(resposta.subtotalProdutos()).isEqualByComparingTo("399.80");
        assertThat(resposta.descontoCupom()).isEqualByComparingTo("50.00");
        assertThat(resposta.frete()).isEqualByComparingTo("18.00");
        assertThat(resposta.prazoEntregaDias()).isEqualTo(0);
        assertThat(resposta.seguro()).isEqualByComparingTo("8.00");
        assertThat(resposta.ajustePagamento()).isEqualByComparingTo("3.49");
        assertThat(resposta.totalFinal()).isEqualByComparingTo("379.29");
        assertThat(resposta.parcelas()).isEqualTo(1);
        assertThat(resposta.valorParcela()).isEqualByComparingTo("379.29");
        assertThat(resposta.creditoProximaCompra()).isEqualByComparingTo("0.00");
        assertThat(resposta.brinde()).isFalse();
    }

    @Test
    void exemplo4_retiradaLojaComLeve3Pague2Cartao3xPrata() {
        ResumoRequest request = new ResumoRequest(
                List.of(item("Meia", 19.90, 7, 0.10), item("Camiseta", 79.90, 2, 0.30)),
                "RETIRADA_LOJA", "LEVE3PAGUE2", "CARTAO", 3, "PRATA", "SUL"
        );

        ResumoResponse resposta = service.calcular(request);

        assertThat(resposta.subtotalProdutos()).isEqualByComparingTo("299.10");
        assertThat(resposta.descontoCupom()).isEqualByComparingTo("39.80");
        assertThat(resposta.frete()).isEqualByComparingTo("0.00");
        assertThat(resposta.prazoEntregaDias()).isEqualTo(1);
        assertThat(resposta.seguro()).isEqualByComparingTo("2.99");
        assertThat(resposta.ajustePagamento()).isEqualByComparingTo("0.00");
        assertThat(resposta.totalFinal()).isEqualByComparingTo("262.29");
        assertThat(resposta.parcelas()).isEqualTo(3);
        assertThat(resposta.valorParcela()).isEqualByComparingTo("87.43");
        assertThat(resposta.creditoProximaCompra()).isEqualByComparingTo("5.98");
        assertThat(resposta.brinde()).isFalse();
    }

    @Test
    void exemplo5_expressaSemCupomPixOuro() {
        ResumoRequest request = new ResumoRequest(
                List.of(item("Camiseta", 79.90, 2, 0.30), item("Tenis", 249.90, 1, 1.20)),
                "EXPRESSA", null, "PIX", null, "OURO", "SUDESTE"
        );

        ResumoResponse resposta = service.calcular(request);

        assertThat(resposta.subtotalProdutos()).isEqualByComparingTo("409.70");
        assertThat(resposta.descontoCupom()).isEqualByComparingTo("0.00");
        assertThat(resposta.frete()).isEqualByComparingTo("0.00");
        assertThat(resposta.prazoEntregaDias()).isEqualTo(2);
        assertThat(resposta.seguro()).isEqualByComparingTo("4.10");
        assertThat(resposta.ajustePagamento()).isEqualByComparingTo("-20.69");
        assertThat(resposta.totalFinal()).isEqualByComparingTo("393.11");
        assertThat(resposta.parcelas()).isEqualTo(1);
        assertThat(resposta.valorParcela()).isEqualByComparingTo("393.11");
        assertThat(resposta.creditoProximaCompra()).isEqualByComparingTo("20.48");
        assertThat(resposta.brinde()).isFalse();
    }

    @Test
    void carrinhoVazioRetornaPedidoInvalido() {
        ResumoRequest request = new ResumoRequest(List.of(), "EXPRESSA", null, "PIX", null, "BRONZE", "SUDESTE");

        assertThatThrownBy(() -> service.calcular(request))
                .isInstanceOf(PedidoException.class)
                .hasMessage("PEDIDO_INVALIDO");
    }

    @Test
    void itemComQuantidadeZeroRetornaPedidoInvalido() {
        ResumoRequest request = new ResumoRequest(
                List.of(item("Camiseta", 79.90, 0, 0.30)),
                "EXPRESSA", null, "PIX", null, "BRONZE", "SUDESTE"
        );

        assertThatThrownBy(() -> service.calcular(request))
                .isInstanceOf(PedidoException.class)
                .hasMessage("PEDIDO_INVALIDO");
    }

    @Test
    void nivelClubeInvalidoRetornaErro() {
        ResumoRequest request = new ResumoRequest(
                List.of(item("Camiseta", 79.90, 1, 0.30)),
                "EXPRESSA", null, "PIX", null, "DIAMANTE", "SUDESTE"
        );

        assertThatThrownBy(() -> service.calcular(request))
                .isInstanceOf(PedidoException.class)
                .hasMessage("NIVEL_CLUBE_INVALIDO");
    }

    @Test
    void regiaoInvalidaRetornaErro() {
        ResumoRequest request = new ResumoRequest(
                List.of(item("Camiseta", 79.90, 1, 0.30)),
                "EXPRESSA", null, "PIX", null, "BRONZE", "LUA"
        );

        assertThatThrownBy(() -> service.calcular(request))
                .isInstanceOf(PedidoException.class)
                .hasMessage("REGIAO_INVALIDA");
    }

    @Test
    void modalidadeInvalidaRetornaErro() {
        ResumoRequest request = new ResumoRequest(
                List.of(item("Camiseta", 79.90, 1, 0.30)),
                "TELEPORTE", null, "PIX", null, "BRONZE", "SUDESTE"
        );

        assertThatThrownBy(() -> service.calcular(request))
                .isInstanceOf(PedidoException.class)
                .hasMessage("MODALIDADE_INVALIDA");
    }

    @Test
    void motoboyAcimaDoLimiteDePesoRetornaIndisponivel() {
        ResumoRequest request = new ResumoRequest(
                List.of(item("Caixa", 100.00, 1, 6.0)),
                "MOTOBOY", null, "PIX", null, "BRONZE", "SUDESTE"
        );

        assertThatThrownBy(() -> service.calcular(request))
                .isInstanceOf(PedidoException.class)
                .hasMessage("MODALIDADE_INDISPONIVEL");
    }

    @Test
    void cupomInexistenteRetornaErro() {
        ResumoRequest request = new ResumoRequest(
                List.of(item("Camiseta", 79.90, 1, 0.30)),
                "EXPRESSA", "NAOEXISTE", "PIX", null, "BRONZE", "SUDESTE"
        );

        assertThatThrownBy(() -> service.calcular(request))
                .isInstanceOf(PedidoException.class)
                .hasMessage("CUPOM_INVALIDO");
    }

    @Test
    void menos50AbaixoDoMinimoRetornaCupomNaoAplicavel() {
        ResumoRequest request = new ResumoRequest(
                List.of(item("Camiseta", 79.90, 1, 0.30)),
                "EXPRESSA", "MENOS50", "PIX", null, "BRONZE", "SUDESTE"
        );

        assertThatThrownBy(() -> service.calcular(request))
                .isInstanceOf(PedidoException.class)
                .hasMessage("CUPOM_NAO_APLICAVEL");
    }

    @Test
    void formaPagamentoInvalidaRetornaErro() {
        ResumoRequest request = new ResumoRequest(
                List.of(item("Camiseta", 79.90, 1, 0.30)),
                "EXPRESSA", null, "CRIPTO", null, "BRONZE", "SUDESTE"
        );

        assertThatThrownBy(() -> service.calcular(request))
                .isInstanceOf(PedidoException.class)
                .hasMessage("FORMA_PAGAMENTO_INVALIDA");
    }

    @Test
    void parcelamentoInvalidoParaPixRetornaErro() {
        ResumoRequest request = new ResumoRequest(
                List.of(item("Camiseta", 79.90, 1, 0.30)),
                "EXPRESSA", null, "PIX", 2, "BRONZE", "SUDESTE"
        );

        assertThatThrownBy(() -> service.calcular(request))
                .isInstanceOf(PedidoException.class)
                .hasMessage("PARCELAMENTO_INVALIDO");
    }

    @Test
    void boletoAcimaDoLimiteRetornaFormaPagamentoIndisponivel() {
        ResumoRequest request = new ResumoRequest(
                List.of(item("Notebook", 1200.00, 1, 2.0)),
                "RETIRADA_LOJA", null, "BOLETO", null, "BRONZE", "SUDESTE"
        );

        assertThatThrownBy(() -> service.calcular(request))
                .isInstanceOf(PedidoException.class)
                .hasMessage("FORMA_PAGAMENTO_INDISPONIVEL");
    }
}

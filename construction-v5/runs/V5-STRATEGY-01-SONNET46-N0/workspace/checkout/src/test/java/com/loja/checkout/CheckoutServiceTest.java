package com.loja.checkout;

import com.loja.checkout.model.*;
import com.loja.checkout.service.CheckoutService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
class CheckoutServiceTest {

    @Autowired
    CheckoutService service;

    // Camiseta 79,90 x2 (0,30kg) + Tênis 249,90 x1 (1,20kg)
    private List<ItemCarrinho> itensPadrao() {
        return List.of(
                new ItemCarrinho("Camiseta", 79.90, 2, 0.30),
                new ItemCarrinho("Tênis", 249.90, 1, 1.20)
        );
    }

    @Test
    void exemplo1_expressa_bemvindo10_pix_bronze_norte() {
        var req = new PedidoRequest(itensPadrao(), "EXPRESSA", "BEMVINDO10", "PIX", 1, "BRONZE", "NORTE");
        var resp = (ResumoResponse) service.calcularResumo(req);

        assertThat(resp.subtotalProdutos()).isEqualTo(409.70);
        assertThat(resp.descontoCupom()).isEqualTo(40.97);
        assertThat(resp.frete()).isEqualTo(33.10);
        assertThat(resp.prazoEntregaDias()).isEqualTo(2);
        assertThat(resp.seguro()).isEqualTo(10.24);
        assertThat(resp.ajustePagamento()).isEqualTo(-20.60);
        assertThat(resp.totalFinal()).isEqualTo(391.47);
        assertThat(resp.parcelas()).isEqualTo(1);
        assertThat(resp.valorParcela()).isEqualTo(391.47);
        assertThat(resp.creditoProximaCompra()).isEqualTo(0.00);
        assertThat(resp.brinde()).isFalse();
    }

    @Test
    void exemplo2_economica_semCupom_cartao6x_prata_centroOeste() {
        var req = new PedidoRequest(itensPadrao(), "ECONOMICA", null, "CARTAO", 6, "PRATA", "CENTRO_OESTE");
        var resp = (ResumoResponse) service.calcularResumo(req);

        assertThat(resp.subtotalProdutos()).isEqualTo(409.70);
        assertThat(resp.descontoCupom()).isEqualTo(0.00);
        assertThat(resp.frete()).isEqualTo(15.60);
        assertThat(resp.prazoEntregaDias()).isEqualTo(7);
        assertThat(resp.seguro()).isEqualTo(6.15);
        assertThat(resp.ajustePagamento()).isEqualTo(30.55);
        assertThat(resp.totalFinal()).isEqualTo(462.00);
        assertThat(resp.parcelas()).isEqualTo(6);
        assertThat(resp.valorParcela()).isEqualTo(77.00);
        assertThat(resp.creditoProximaCompra()).isEqualTo(8.19);
        assertThat(resp.brinde()).isFalse();
    }

    @Test
    void exemplo3_fone_motoboy_menos50_boleto_bronze_nordeste() {
        var itens = List.of(new ItemCarrinho("Fone", 199.90, 2, 0.25));
        var req = new PedidoRequest(itens, "MOTOBOY", "MENOS50", "BOLETO", 1, "BRONZE", "NORDESTE");
        var resp = (ResumoResponse) service.calcularResumo(req);

        assertThat(resp.subtotalProdutos()).isEqualTo(399.80);
        assertThat(resp.descontoCupom()).isEqualTo(50.00);
        assertThat(resp.frete()).isEqualTo(18.00);
        assertThat(resp.prazoEntregaDias()).isEqualTo(0);
        assertThat(resp.seguro()).isEqualTo(8.00);
        assertThat(resp.ajustePagamento()).isEqualTo(3.49);
        assertThat(resp.totalFinal()).isEqualTo(379.29);
        assertThat(resp.parcelas()).isEqualTo(1);
        assertThat(resp.valorParcela()).isEqualTo(379.29);
        assertThat(resp.creditoProximaCompra()).isEqualTo(0.00);
        assertThat(resp.brinde()).isFalse();
    }

    @Test
    void exemplo4_meia7_camiseta2_retirada_leve3pague2_cartao3x_prata_sul() {
        var itens = List.of(
                new ItemCarrinho("Meia", 19.90, 7, 0.10),
                new ItemCarrinho("Camiseta", 79.90, 2, 0.30)
        );
        var req = new PedidoRequest(itens, "RETIRADA_LOJA", "LEVE3PAGUE2", "CARTAO", 3, "PRATA", "SUL");
        var resp = (ResumoResponse) service.calcularResumo(req);

        assertThat(resp.subtotalProdutos()).isEqualTo(299.10);
        assertThat(resp.descontoCupom()).isEqualTo(39.80);
        assertThat(resp.frete()).isEqualTo(0.00);
        assertThat(resp.prazoEntregaDias()).isEqualTo(1);
        assertThat(resp.seguro()).isEqualTo(2.99);
        assertThat(resp.ajustePagamento()).isEqualTo(0.00);
        assertThat(resp.totalFinal()).isEqualTo(262.29);
        assertThat(resp.parcelas()).isEqualTo(3);
        assertThat(resp.valorParcela()).isEqualTo(87.43);
        assertThat(resp.creditoProximaCompra()).isEqualTo(5.98);
        assertThat(resp.brinde()).isFalse();
    }

    @Test
    void exemplo5_expressa_semCupom_pix_ouro_sudeste() {
        var req = new PedidoRequest(itensPadrao(), "EXPRESSA", null, "PIX", 1, "OURO", "SUDESTE");
        var resp = (ResumoResponse) service.calcularResumo(req);

        assertThat(resp.subtotalProdutos()).isEqualTo(409.70);
        assertThat(resp.descontoCupom()).isEqualTo(0.00);
        assertThat(resp.frete()).isEqualTo(0.00);
        assertThat(resp.prazoEntregaDias()).isEqualTo(2);
        assertThat(resp.seguro()).isEqualTo(4.10);
        assertThat(resp.ajustePagamento()).isEqualTo(-20.69);
        assertThat(resp.totalFinal()).isEqualTo(393.11);
        assertThat(resp.parcelas()).isEqualTo(1);
        assertThat(resp.valorParcela()).isEqualTo(393.11);
        assertThat(resp.creditoProximaCompra()).isEqualTo(20.48);
        assertThat(resp.brinde()).isFalse();
    }

    @Test
    void erroCarrinhoVazio() {
        var req = new PedidoRequest(List.of(), "EXPRESSA", null, "PIX", 1, "BRONZE", "SUL");
        var resp = (ErroResponse) service.calcularResumo(req);
        assertThat(resp.erro()).isEqualTo("PEDIDO_INVALIDO");
    }

    @Test
    void erroItemPrecoZero() {
        var itens = List.of(new ItemCarrinho("X", 0.0, 1, 0.5));
        var req = new PedidoRequest(itens, "EXPRESSA", null, "PIX", 1, "BRONZE", "SUL");
        var resp = (ErroResponse) service.calcularResumo(req);
        assertThat(resp.erro()).isEqualTo("PEDIDO_INVALIDO");
    }

    @Test
    void erroNivelClubeInvalido() {
        var req = new PedidoRequest(itensPadrao(), "EXPRESSA", null, "PIX", 1, "DIAMANTE", "SUL");
        var resp = (ErroResponse) service.calcularResumo(req);
        assertThat(resp.erro()).isEqualTo("NIVEL_CLUBE_INVALIDO");
    }

    @Test
    void erroRegiaoInvalida() {
        var req = new PedidoRequest(itensPadrao(), "EXPRESSA", null, "PIX", 1, "BRONZE", "LESTE");
        var resp = (ErroResponse) service.calcularResumo(req);
        assertThat(resp.erro()).isEqualTo("REGIAO_INVALIDA");
    }

    @Test
    void erroModalidadeInvalida() {
        var req = new PedidoRequest(itensPadrao(), "DRONE", null, "PIX", 1, "BRONZE", "SUL");
        var resp = (ErroResponse) service.calcularResumo(req);
        assertThat(resp.erro()).isEqualTo("MODALIDADE_INVALIDA");
    }

    @Test
    void erroMotoboyAcima5kg() {
        var itens = List.of(new ItemCarrinho("Caixa", 100.0, 1, 6.0));
        var req = new PedidoRequest(itens, "MOTOBOY", null, "PIX", 1, "BRONZE", "SUL");
        var resp = (ErroResponse) service.calcularResumo(req);
        assertThat(resp.erro()).isEqualTo("MODALIDADE_INDISPONIVEL");
    }

    @Test
    void erroCupomInvalido() {
        var req = new PedidoRequest(itensPadrao(), "EXPRESSA", "CUPOMINEXISTENTE", "PIX", 1, "BRONZE", "SUL");
        var resp = (ErroResponse) service.calcularResumo(req);
        assertThat(resp.erro()).isEqualTo("CUPOM_INVALIDO");
    }

    @Test
    void erroCupomMenos50NaoAplicavel() {
        var itens = List.of(new ItemCarrinho("Camiseta", 79.90, 1, 0.30));
        var req = new PedidoRequest(itens, "EXPRESSA", "MENOS50", "PIX", 1, "BRONZE", "SUL");
        var resp = (ErroResponse) service.calcularResumo(req);
        assertThat(resp.erro()).isEqualTo("CUPOM_NAO_APLICAVEL");
    }

    @Test
    void erroFormaPagamentoInvalida() {
        var req = new PedidoRequest(itensPadrao(), "EXPRESSA", null, "CRIPTOMOEDA", 1, "BRONZE", "SUL");
        var resp = (ErroResponse) service.calcularResumo(req);
        assertThat(resp.erro()).isEqualTo("FORMA_PAGAMENTO_INVALIDA");
    }

    @Test
    void erroParcelamentoBoleto() {
        var req = new PedidoRequest(itensPadrao(), "EXPRESSA", null, "BOLETO", 2, "BRONZE", "SUL");
        var resp = (ErroResponse) service.calcularResumo(req);
        assertThat(resp.erro()).isEqualTo("PARCELAMENTO_INVALIDO");
    }

    @Test
    void erroParcelamentoPix() {
        var req = new PedidoRequest(itensPadrao(), "EXPRESSA", null, "PIX", 3, "BRONZE", "SUL");
        var resp = (ErroResponse) service.calcularResumo(req);
        assertThat(resp.erro()).isEqualTo("PARCELAMENTO_INVALIDO");
    }

    @Test
    void erroCartaoParcelasAcima12() {
        var req = new PedidoRequest(itensPadrao(), "EXPRESSA", null, "CARTAO", 13, "BRONZE", "SUL");
        var resp = (ErroResponse) service.calcularResumo(req);
        assertThat(resp.erro()).isEqualTo("PARCELAMENTO_INVALIDO");
    }

    @Test
    void erroBoletoAcima1000() {
        var itens = List.of(new ItemCarrinho("Produto caro", 600.0, 2, 1.0));
        var req = new PedidoRequest(itens, "EXPRESSA", null, "BOLETO", 1, "BRONZE", "SUL");
        var resp = (ErroResponse) service.calcularResumo(req);
        assertThat(resp.erro()).isEqualTo("FORMA_PAGAMENTO_INDISPONIVEL");
    }

    @Test
    void ourofSemFrete_brinde_acima500() {
        var itens = List.of(new ItemCarrinho("Produto", 300.0, 2, 1.0));
        var req = new PedidoRequest(itens, "EXPRESSA", null, "PIX", 1, "OURO", "SUDESTE");
        var resp = (ResumoResponse) service.calcularResumo(req);
        assertThat(resp.frete()).isEqualTo(0.00);
        assertThat(resp.brinde()).isTrue();
    }

    @Test
    void fretegratisCupom() {
        var req = new PedidoRequest(itensPadrao(), "EXPRESSA", "FRETEGRATIS", "CARTAO", 1, "BRONZE", "SUL");
        var resp = (ResumoResponse) service.calcularResumo(req);
        // frete aparece normalmente, desconto = frete
        assertThat(resp.frete()).isEqualTo(resp.descontoCupom());
        assertThat(resp.frete()).isGreaterThan(0);
    }

    @Test
    void parcelasNulasTratadaComoUm() {
        var req = new PedidoRequest(itensPadrao(), "EXPRESSA", null, "PIX", null, "BRONZE", "SUL");
        var resp = (ResumoResponse) service.calcularResumo(req);
        assertThat(resp.parcelas()).isEqualTo(1);
    }
}

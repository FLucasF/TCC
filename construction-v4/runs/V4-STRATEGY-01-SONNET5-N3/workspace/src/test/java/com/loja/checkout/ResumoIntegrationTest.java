package com.loja.checkout;

import static org.assertj.core.api.Assertions.assertThat;

import com.loja.checkout.web.ErroResponse;
import com.loja.checkout.web.ItemRequest;
import com.loja.checkout.web.ResumoRequest;
import com.loja.checkout.web.ResumoResponse;
import java.math.BigDecimal;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import tools.jackson.databind.ObjectMapper;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class ResumoIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    private static List<ItemRequest> itensCamisetaTenis() {
        return List.of(
                new ItemRequest("Camiseta", new BigDecimal("79.90"), 2, new BigDecimal("0.30")),
                new ItemRequest("Tenis", new BigDecimal("249.90"), 1, new BigDecimal("1.20")));
    }

    private ResumoResponse executar(ResumoRequest request) throws Exception {
        String json = mockMvc.perform(post("/checkout/resumo")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();
        return objectMapper.readValue(json, ResumoResponse.class);
    }

    private String executarComErro(ResumoRequest request) throws Exception {
        String json = mockMvc.perform(post("/checkout/resumo")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andReturn()
                .getResponse()
                .getContentAsString();
        return objectMapper.readValue(json, ErroResponse.class).erro();
    }

    @Test
    void exemplo1() throws Exception {
        ResumoResponse r = executar(new ResumoRequest(
                itensCamisetaTenis(), "EXPRESSA", "BEMVINDO10", "PIX", null, "BRONZE", "NORTE"));

        assertThat(r.subtotalProdutos()).isEqualTo("409.70");
        assertThat(r.descontoCupom()).isEqualTo("40.97");
        assertThat(r.frete()).isEqualTo("33.10");
        assertThat(r.prazoEntregaDias()).isEqualTo(2);
        assertThat(r.seguro()).isEqualTo("10.24");
        assertThat(r.ajustePagamento()).isEqualTo("-20.60");
        assertThat(r.totalFinal()).isEqualTo("391.47");
        assertThat(r.parcelas()).isEqualTo(1);
        assertThat(r.valorParcela()).isEqualTo("391.47");
        assertThat(r.creditoProximaCompra()).isEqualTo("0.00");
        assertThat(r.brinde()).isFalse();
    }

    @Test
    void exemplo2() throws Exception {
        ResumoResponse r = executar(new ResumoRequest(
                itensCamisetaTenis(), "ECONOMICA", null, "CARTAO", 6, "PRATA", "CENTRO_OESTE"));

        assertThat(r.subtotalProdutos()).isEqualTo("409.70");
        assertThat(r.descontoCupom()).isEqualTo("0.00");
        assertThat(r.frete()).isEqualTo("15.60");
        assertThat(r.prazoEntregaDias()).isEqualTo(7);
        assertThat(r.seguro()).isEqualTo("6.15");
        assertThat(r.ajustePagamento()).isEqualTo("30.55");
        assertThat(r.totalFinal()).isEqualTo("462.00");
        assertThat(r.parcelas()).isEqualTo(6);
        assertThat(r.valorParcela()).isEqualTo("77.00");
        assertThat(r.creditoProximaCompra()).isEqualTo("8.19");
        assertThat(r.brinde()).isFalse();
    }

    @Test
    void exemplo3() throws Exception {
        List<ItemRequest> itens = List.of(
                new ItemRequest("Fone", new BigDecimal("199.90"), 2, new BigDecimal("0.25")));
        ResumoResponse r = executar(new ResumoRequest(
                itens, "MOTOBOY", "MENOS50", "BOLETO", null, "BRONZE", "NORDESTE"));

        assertThat(r.subtotalProdutos()).isEqualTo("399.80");
        assertThat(r.descontoCupom()).isEqualTo("50.00");
        assertThat(r.frete()).isEqualTo("18.00");
        assertThat(r.prazoEntregaDias()).isEqualTo(0);
        assertThat(r.seguro()).isEqualTo("8.00");
        assertThat(r.ajustePagamento()).isEqualTo("3.49");
        assertThat(r.totalFinal()).isEqualTo("379.29");
        assertThat(r.parcelas()).isEqualTo(1);
        assertThat(r.valorParcela()).isEqualTo("379.29");
        assertThat(r.creditoProximaCompra()).isEqualTo("0.00");
        assertThat(r.brinde()).isFalse();
    }

    @Test
    void exemplo4() throws Exception {
        List<ItemRequest> itens = List.of(
                new ItemRequest("Meia", new BigDecimal("19.90"), 7, new BigDecimal("0.10")),
                new ItemRequest("Camiseta", new BigDecimal("79.90"), 2, new BigDecimal("0.30")));
        ResumoResponse r = executar(new ResumoRequest(
                itens, "RETIRADA_LOJA", "LEVE3PAGUE2", "CARTAO", 3, "PRATA", "SUL"));

        assertThat(r.subtotalProdutos()).isEqualTo("299.10");
        assertThat(r.descontoCupom()).isEqualTo("39.80");
        assertThat(r.frete()).isEqualTo("0.00");
        assertThat(r.prazoEntregaDias()).isEqualTo(1);
        assertThat(r.seguro()).isEqualTo("2.99");
        assertThat(r.ajustePagamento()).isEqualTo("0.00");
        assertThat(r.totalFinal()).isEqualTo("262.29");
        assertThat(r.parcelas()).isEqualTo(3);
        assertThat(r.valorParcela()).isEqualTo("87.43");
        assertThat(r.creditoProximaCompra()).isEqualTo("5.98");
        assertThat(r.brinde()).isFalse();
    }

    @Test
    void exemplo5() throws Exception {
        ResumoResponse r = executar(new ResumoRequest(
                itensCamisetaTenis(), "EXPRESSA", null, "PIX", null, "OURO", "SUDESTE"));

        assertThat(r.subtotalProdutos()).isEqualTo("409.70");
        assertThat(r.descontoCupom()).isEqualTo("0.00");
        assertThat(r.frete()).isEqualTo("0.00");
        assertThat(r.prazoEntregaDias()).isEqualTo(2);
        assertThat(r.seguro()).isEqualTo("4.10");
        assertThat(r.ajustePagamento()).isEqualTo("-20.69");
        assertThat(r.totalFinal()).isEqualTo("393.11");
        assertThat(r.parcelas()).isEqualTo(1);
        assertThat(r.valorParcela()).isEqualTo("393.11");
        assertThat(r.creditoProximaCompra()).isEqualTo("20.48");
        assertThat(r.brinde()).isFalse();
    }

    @Test
    void carrinhoVazioDaErro() throws Exception {
        String erro = executarComErro(new ResumoRequest(
                List.of(), "EXPRESSA", null, "PIX", null, "BRONZE", "SUDESTE"));
        assertThat(erro).isEqualTo("PEDIDO_INVALIDO");
    }

    @Test
    void itemComPrecoZeroDaErro() throws Exception {
        List<ItemRequest> itens = List.of(
                new ItemRequest("Camiseta", new BigDecimal("0"), 2, new BigDecimal("0.30")));
        String erro = executarComErro(new ResumoRequest(
                itens, "EXPRESSA", null, "PIX", null, "BRONZE", "SUDESTE"));
        assertThat(erro).isEqualTo("PEDIDO_INVALIDO");
    }

    @Test
    void nivelClubeInvalidoDaErro() throws Exception {
        String erro = executarComErro(new ResumoRequest(
                itensCamisetaTenis(), "EXPRESSA", null, "PIX", null, "DIAMANTE", "SUDESTE"));
        assertThat(erro).isEqualTo("NIVEL_CLUBE_INVALIDO");
    }

    @Test
    void regiaoInvalidaDaErro() throws Exception {
        String erro = executarComErro(new ResumoRequest(
                itensCamisetaTenis(), "EXPRESSA", null, "PIX", null, "BRONZE", "MARTE"));
        assertThat(erro).isEqualTo("REGIAO_INVALIDA");
    }

    @Test
    void modalidadeInvalidaDaErro() throws Exception {
        String erro = executarComErro(new ResumoRequest(
                itensCamisetaTenis(), "TELEPORTE", null, "PIX", null, "BRONZE", "SUDESTE"));
        assertThat(erro).isEqualTo("MODALIDADE_INVALIDA");
    }

    @Test
    void motoboyAcimaDoPesoDaErro() throws Exception {
        List<ItemRequest> itens = List.of(
                new ItemRequest("Sofa", new BigDecimal("999.00"), 1, new BigDecimal("6.00")));
        String erro = executarComErro(new ResumoRequest(
                itens, "MOTOBOY", null, "PIX", null, "BRONZE", "SUDESTE"));
        assertThat(erro).isEqualTo("MODALIDADE_INDISPONIVEL");
    }

    @Test
    void cupomInvalidoDaErro() throws Exception {
        String erro = executarComErro(new ResumoRequest(
                itensCamisetaTenis(), "EXPRESSA", "NAOEXISTE", "PIX", null, "BRONZE", "SUDESTE"));
        assertThat(erro).isEqualTo("CUPOM_INVALIDO");
    }

    @Test
    void cupomNaoAplicavelDaErro() throws Exception {
        List<ItemRequest> itens = List.of(
                new ItemRequest("Meia", new BigDecimal("19.90"), 1, new BigDecimal("0.10")));
        String erro = executarComErro(new ResumoRequest(
                itens, "EXPRESSA", "MENOS50", "PIX", null, "BRONZE", "SUDESTE"));
        assertThat(erro).isEqualTo("CUPOM_NAO_APLICAVEL");
    }

    @Test
    void formaPagamentoInvalidaDaErro() throws Exception {
        String erro = executarComErro(new ResumoRequest(
                itensCamisetaTenis(), "EXPRESSA", null, "CRIPTO", null, "BRONZE", "SUDESTE"));
        assertThat(erro).isEqualTo("FORMA_PAGAMENTO_INVALIDA");
    }

    @Test
    void parcelamentoInvalidoNoPixDaErro() throws Exception {
        String erro = executarComErro(new ResumoRequest(
                itensCamisetaTenis(), "EXPRESSA", null, "PIX", 2, "BRONZE", "SUDESTE"));
        assertThat(erro).isEqualTo("PARCELAMENTO_INVALIDO");
    }

    @Test
    void parcelamentoInvalidoNoCartaoAcimaDe12DaErro() throws Exception {
        String erro = executarComErro(new ResumoRequest(
                itensCamisetaTenis(), "EXPRESSA", null, "CARTAO", 13, "BRONZE", "SUDESTE"));
        assertThat(erro).isEqualTo("PARCELAMENTO_INVALIDO");
    }

    @Test
    void boletoAcimaDeMilDaErro() throws Exception {
        List<ItemRequest> itens = List.of(
                new ItemRequest("Notebook", new BigDecimal("1500.00"), 1, new BigDecimal("2.00")));
        String erro = executarComErro(new ResumoRequest(
                itens, "RETIRADA_LOJA", null, "BOLETO", null, "BRONZE", "SUDESTE"));
        assertThat(erro).isEqualTo("FORMA_PAGAMENTO_INDISPONIVEL");
    }

    @Test
    void motoboyNoLimiteDeCincoQuilosFuncionaNormalmente() throws Exception {
        List<ItemRequest> itens = List.of(
                new ItemRequest("Caixa", new BigDecimal("100.00"), 1, new BigDecimal("5.00")));
        ResumoResponse r = executar(new ResumoRequest(
                itens, "MOTOBOY", null, "PIX", null, "BRONZE", "SUDESTE"));

        assertThat(r.frete()).isEqualTo("18.00");
        assertThat(r.prazoEntregaDias()).isEqualTo(0);
        assertThat(r.totalFinal()).isEqualTo("113.05");
    }

    @Test
    void boletoNoLimiteExatoDeMilFuncionaNormalmente() throws Exception {
        List<ItemRequest> itens = List.of(
                new ItemRequest("Produto", new BigDecimal("990.10"), 1, new BigDecimal("1.00")));
        ResumoResponse r = executar(new ResumoRequest(
                itens, "RETIRADA_LOJA", null, "BOLETO", null, "BRONZE", "SUDESTE"));

        assertThat(r.seguro()).isEqualTo("9.90");
        assertThat(r.ajustePagamento()).isEqualTo("3.49");
        assertThat(r.totalFinal()).isEqualTo("1003.49");
    }

    @Test
    void cartaoEmQuatroVezesJaCobraJuros() throws Exception {
        ResumoResponse r = executar(new ResumoRequest(
                itensCamisetaTenis(), "ECONOMICA", null, "CARTAO", 4, "BRONZE", "SUDESTE"));

        assertThat(r.valorParcela()).isEqualTo("112.74");
        assertThat(r.totalFinal()).isEqualTo("450.96");
        assertThat(r.ajustePagamento()).isEqualTo("21.56");
    }

    @Test
    void leve3Pague2DescontaCadaItemElegivelSeparadamente() throws Exception {
        List<ItemRequest> itens = List.of(
                new ItemRequest("Meia", new BigDecimal("19.90"), 7, new BigDecimal("0.10")),
                new ItemRequest("Camiseta", new BigDecimal("79.90"), 6, new BigDecimal("0.30")));
        ResumoResponse r = executar(new ResumoRequest(
                itens, "RETIRADA_LOJA", "LEVE3PAGUE2", "PIX", null, "BRONZE", "SUDESTE"));

        assertThat(r.subtotalProdutos()).isEqualTo("618.70");
        assertThat(r.descontoCupom()).isEqualTo("199.60");
        assertThat(r.totalFinal()).isEqualTo("404.03");
    }

    @Test
    void ouroComFreteGratisNaoDescontaNadaPorqueFreteJaEraZero() throws Exception {
        ResumoResponse r = executar(new ResumoRequest(
                itensCamisetaTenis(), "EXPRESSA", "FRETEGRATIS", "PIX", null, "OURO", "SUDESTE"));

        assertThat(r.frete()).isEqualTo("0.00");
        assertThat(r.descontoCupom()).isEqualTo("0.00");
        assertThat(r.totalFinal()).isEqualTo("393.11");
        assertThat(r.creditoProximaCompra()).isEqualTo("20.48");
    }
}

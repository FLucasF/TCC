package com.loja.checkout;

import com.loja.checkout.dto.ItemRequest;
import com.loja.checkout.dto.ResumoRequest;
import com.loja.checkout.dto.ResumoResponse;
import com.loja.checkout.exception.CheckoutException;
import com.loja.checkout.service.CheckoutService;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class CheckoutServiceTest {

    private final CheckoutService service = new CheckoutService();

    private ItemRequest item(String nome, String preco, int quantidade, String peso) {
        ItemRequest item = new ItemRequest();
        item.setNome(nome);
        item.setPrecoUnitario(new BigDecimal(preco));
        item.setQuantidade(quantidade);
        item.setPesoKg(new BigDecimal(peso));
        return item;
    }

    private ResumoRequest baseRequest(List<ItemRequest> itens, String modalidade, String cupom,
                                       String forma, Integer parcelas, String nivel, String regiao) {
        ResumoRequest request = new ResumoRequest();
        request.setItens(itens);
        request.setModalidadeEntrega(modalidade);
        request.setCupom(cupom);
        request.setFormaPagamento(forma);
        request.setParcelas(parcelas);
        request.setNivelClube(nivel);
        request.setRegiao(regiao);
        return request;
    }

    @Test
    void exemploDoEnunciado_ouroSemPagarFreteComCreditoEBrinde() {
        List<ItemRequest> itens = List.of(
                item("Camiseta", "79.90", 2, "0.30"),
                item("Tênis", "249.90", 1, "1.20")
        );
        ResumoRequest request = baseRequest(itens, "EXPRESSA", null, "PIX", null, "OURO", "SUDESTE");

        ResumoResponse resumo = service.calcularResumo(request);

        assertThat(resumo.getSubtotalProdutos()).isEqualByComparingTo("409.70");
        assertThat(resumo.getDescontoCupom()).isEqualByComparingTo("0.00");
        assertThat(resumo.getFrete()).isEqualByComparingTo("0.00");
        assertThat(resumo.getPrazoEntregaDias()).isEqualTo(2);
        assertThat(resumo.getImposto()).isEqualByComparingTo("49.16");
        assertThat(resumo.getAjustePagamento()).isEqualByComparingTo("-22.94");
        assertThat(resumo.getTotalFinal()).isEqualByComparingTo("435.92");
        assertThat(resumo.getParcelas()).isEqualTo(1);
        assertThat(resumo.getValorParcela()).isEqualByComparingTo("435.92");
        assertThat(resumo.getCreditoProximaCompra()).isEqualByComparingTo("20.48");
        assertThat(resumo.isBrinde()).isFalse();
    }

    @Test
    void ouroComBrindeQuandoProdutosPassamDe500() {
        List<ItemRequest> itens = List.of(item("Sofá", "600.00", 1, "1.00"));
        ResumoRequest request = baseRequest(itens, "RETIRADA_LOJA", null, "PIX", null, "OURO", "SUL");

        ResumoResponse resumo = service.calcularResumo(request);

        assertThat(resumo.getFrete()).isEqualByComparingTo("0.00");
        assertThat(resumo.getImposto()).isEqualByComparingTo("66.00");
        assertThat(resumo.getTotalFinal()).isEqualByComparingTo("632.70");
        assertThat(resumo.getCreditoProximaCompra()).isEqualByComparingTo("30.00");
        assertThat(resumo.isBrinde()).isTrue();
    }

    @Test
    void cupomBemvindo10ComPixEJurosNegativos() {
        List<ItemRequest> itens = List.of(
                item("Camiseta", "79.90", 2, "0.30"),
                item("Tênis", "249.90", 1, "1.20")
        );
        ResumoRequest request = baseRequest(itens, "EXPRESSA", "BEMVINDO10", "PIX", null, "BRONZE", "SUL");

        ResumoResponse resumo = service.calcularResumo(request);

        assertThat(resumo.getDescontoCupom()).isEqualByComparingTo("40.97");
        assertThat(resumo.getFrete()).isEqualByComparingTo("33.10");
        assertThat(resumo.getImposto()).isEqualByComparingTo("40.56");
        assertThat(resumo.getAjustePagamento()).isEqualByComparingTo("-22.12");
        assertThat(resumo.getTotalFinal()).isEqualByComparingTo("420.27");
    }

    @Test
    void cartaoSeisParcelasComJuros() {
        List<ItemRequest> itens = List.of(
                item("Camiseta", "79.90", 2, "0.30"),
                item("Tênis", "249.90", 1, "1.20")
        );
        ResumoRequest request = baseRequest(itens, "ECONOMICA", null, "CARTAO", 6, "BRONZE", "CENTRO_OESTE");

        ResumoResponse resumo = service.calcularResumo(request);

        assertThat(resumo.getFrete()).isEqualByComparingTo("15.60");
        assertThat(resumo.getPrazoEntregaDias()).isEqualTo(7);
        assertThat(resumo.getImposto()).isEqualByComparingTo("36.87");
        assertThat(resumo.getAjustePagamento()).isEqualByComparingTo("32.71");
        assertThat(resumo.getTotalFinal()).isEqualByComparingTo("494.88");
        assertThat(resumo.getValorParcela()).isEqualByComparingTo("82.48");
    }

    @Test
    void motoboyComCupomMenos50EBoleto() {
        List<ItemRequest> itens = List.of(item("Fone", "199.90", 2, "0.25"));
        ResumoRequest request = baseRequest(itens, "MOTOBOY", "MENOS50", "BOLETO", null, "BRONZE", "NORTE");

        ResumoResponse resumo = service.calcularResumo(request);

        assertThat(resumo.getDescontoCupom()).isEqualByComparingTo("50.00");
        assertThat(resumo.getFrete()).isEqualByComparingTo("18.00");
        assertThat(resumo.getPrazoEntregaDias()).isEqualTo(0);
        assertThat(resumo.getAjustePagamento()).isEqualByComparingTo("3.49");
        assertThat(resumo.getTotalFinal()).isEqualByComparingTo("395.78");
    }

    @Test
    void leve3Pague2ComRetiradaLojaECartaoTresVezes() {
        List<ItemRequest> itens = List.of(
                item("Meia", "19.90", 7, "0.10"),
                item("Camiseta", "79.90", 2, "0.30")
        );
        ResumoRequest request = baseRequest(itens, "RETIRADA_LOJA", "LEVE3PAGUE2", "CARTAO", 3, "PRATA", "NORDESTE");

        ResumoResponse resumo = service.calcularResumo(request);

        assertThat(resumo.getSubtotalProdutos()).isEqualByComparingTo("299.10");
        assertThat(resumo.getDescontoCupom()).isEqualByComparingTo("39.80");
        assertThat(resumo.getFrete()).isEqualByComparingTo("0.00");
        assertThat(resumo.getAjustePagamento()).isEqualByComparingTo("0.00");
        assertThat(resumo.getTotalFinal()).isEqualByComparingTo("277.45");
        assertThat(resumo.getValorParcela()).isEqualByComparingTo("92.48");
        assertThat(resumo.getCreditoProximaCompra()).isEqualByComparingTo("5.98");
    }

    @Test
    void carrinhoVazioGeraPedidoInvalido() {
        ResumoRequest request = baseRequest(List.of(), "EXPRESSA", null, "PIX", null, "BRONZE", "SUL");

        assertThatThrownBy(() -> service.calcularResumo(request))
                .isInstanceOf(CheckoutException.class)
                .hasMessage("PEDIDO_INVALIDO");
    }

    @Test
    void itemComQuantidadeZeradaGeraPedidoInvalido() {
        List<ItemRequest> itens = List.of(item("Camiseta", "79.90", 0, "0.30"));
        ResumoRequest request = baseRequest(itens, "EXPRESSA", null, "PIX", null, "BRONZE", "SUL");

        assertThatThrownBy(() -> service.calcularResumo(request))
                .isInstanceOf(CheckoutException.class)
                .hasMessage("PEDIDO_INVALIDO");
    }

    @Test
    void nivelClubeInvalido() {
        List<ItemRequest> itens = List.of(item("Camiseta", "79.90", 1, "0.30"));
        ResumoRequest request = baseRequest(itens, "EXPRESSA", null, "PIX", null, "DIAMANTE", "SUL");

        assertThatThrownBy(() -> service.calcularResumo(request))
                .isInstanceOf(CheckoutException.class)
                .hasMessage("NIVEL_CLUBE_INVALIDO");
    }

    @Test
    void regiaoInvalida() {
        List<ItemRequest> itens = List.of(item("Camiseta", "79.90", 1, "0.30"));
        ResumoRequest request = baseRequest(itens, "EXPRESSA", null, "PIX", null, "BRONZE", "OESTE");

        assertThatThrownBy(() -> service.calcularResumo(request))
                .isInstanceOf(CheckoutException.class)
                .hasMessage("REGIAO_INVALIDA");
    }

    @Test
    void modalidadeInvalida() {
        List<ItemRequest> itens = List.of(item("Camiseta", "79.90", 1, "0.30"));
        ResumoRequest request = baseRequest(itens, "TELEPORTE", null, "PIX", null, "BRONZE", "SUL");

        assertThatThrownBy(() -> service.calcularResumo(request))
                .isInstanceOf(CheckoutException.class)
                .hasMessage("MODALIDADE_INVALIDA");
    }

    @Test
    void motoboyAcimaDoLimiteDePesoGeraModalidadeIndisponivel() {
        List<ItemRequest> itens = List.of(item("Peso extra", "50.00", 1, "6"));
        ResumoRequest request = baseRequest(itens, "MOTOBOY", null, "PIX", null, "BRONZE", "SUL");

        assertThatThrownBy(() -> service.calcularResumo(request))
                .isInstanceOf(CheckoutException.class)
                .hasMessage("MODALIDADE_INDISPONIVEL");
    }

    @Test
    void cupomInvalido() {
        List<ItemRequest> itens = List.of(item("Camiseta", "79.90", 1, "0.30"));
        ResumoRequest request = baseRequest(itens, "EXPRESSA", "NAOEXISTE", "PIX", null, "BRONZE", "SUL");

        assertThatThrownBy(() -> service.calcularResumo(request))
                .isInstanceOf(CheckoutException.class)
                .hasMessage("CUPOM_INVALIDO");
    }

    @Test
    void cupomMenos50AbaixoDoMinimoNaoAplicavel() {
        List<ItemRequest> itens = List.of(item("Camiseta", "79.90", 1, "0.30"));
        ResumoRequest request = baseRequest(itens, "EXPRESSA", "MENOS50", "PIX", null, "BRONZE", "SUL");

        assertThatThrownBy(() -> service.calcularResumo(request))
                .isInstanceOf(CheckoutException.class)
                .hasMessage("CUPOM_NAO_APLICAVEL");
    }

    @Test
    void formaPagamentoInvalida() {
        List<ItemRequest> itens = List.of(item("Camiseta", "79.90", 1, "0.30"));
        ResumoRequest request = baseRequest(itens, "EXPRESSA", null, "CRIPTOMOEDA", null, "BRONZE", "SUL");

        assertThatThrownBy(() -> service.calcularResumo(request))
                .isInstanceOf(CheckoutException.class)
                .hasMessage("FORMA_PAGAMENTO_INVALIDA");
    }

    @Test
    void parcelamentoInvalidoParaPix() {
        List<ItemRequest> itens = List.of(item("Camiseta", "79.90", 1, "0.30"));
        ResumoRequest request = baseRequest(itens, "EXPRESSA", null, "PIX", 2, "BRONZE", "SUL");

        assertThatThrownBy(() -> service.calcularResumo(request))
                .isInstanceOf(CheckoutException.class)
                .hasMessage("PARCELAMENTO_INVALIDO");
    }

    @Test
    void parcelamentoInvalidoParaCartaoAcimaDeDoze() {
        List<ItemRequest> itens = List.of(item("Camiseta", "79.90", 1, "0.30"));
        ResumoRequest request = baseRequest(itens, "EXPRESSA", null, "CARTAO", 13, "BRONZE", "SUL");

        assertThatThrownBy(() -> service.calcularResumo(request))
                .isInstanceOf(CheckoutException.class)
                .hasMessage("PARCELAMENTO_INVALIDO");
    }

    @Test
    void boletoAcimaDeMilNaoDisponivel() {
        List<ItemRequest> itens = List.of(item("Notebook", "1200.00", 1, "2.00"));
        ResumoRequest request = baseRequest(itens, "RETIRADA_LOJA", null, "BOLETO", null, "BRONZE", "SUL");

        assertThatThrownBy(() -> service.calcularResumo(request))
                .isInstanceOf(CheckoutException.class)
                .hasMessage("FORMA_PAGAMENTO_INDISPONIVEL");
    }
}

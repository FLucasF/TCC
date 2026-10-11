package br.com.loja.checkout;

import static org.assertj.core.api.Assertions.assertThat;

import br.com.loja.checkout.api.ItemRequest;
import br.com.loja.checkout.api.ResumoCheckoutService;
import br.com.loja.checkout.api.ResumoRequest;
import br.com.loja.checkout.api.ResumoResponse;
import java.math.BigDecimal;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
class RegrasDeCalculoTest {

    @Autowired
    private ResumoCheckoutService servico;

    private static ItemRequest item(String preco, int quantidade, String peso) {
        return new ItemRequest("Produto", new BigDecimal(preco), quantidade, new BigDecimal(peso));
    }

    private ResumoResponse resumo(List<ItemRequest> itens, String modalidade, String cupom,
            String pagamento, Integer parcelas, String nivel, String regiao) {
        return servico.calcular(new ResumoRequest(itens, modalidade, cupom, pagamento, parcelas, nivel, regiao));
    }

    @Test
    void comFreteGratisOFreteApareceEODescontoFicaIgualAoFrete() {
        ResumoResponse resposta = resumo(List.of(item("79.90", 2, "0.30")),
                "ECONOMICA", "FRETEGRATIS", "PIX", null, "BRONZE", "SUDESTE");

        assertThat(resposta.frete()).isEqualByComparingTo("13.20");
        assertThat(resposta.descontoCupom()).isEqualByComparingTo("13.20");
        assertThat(resposta.totalFinal()).isEqualByComparingTo("153.33");
    }

    @Test
    void ouroNaoPagaFreteEntaoFreteGratisNaoDescontaNada() {
        ResumoResponse resposta = resumo(List.of(item("79.90", 2, "0.30")),
                "EXPRESSA", "FRETEGRATIS", "PIX", null, "OURO", "SUDESTE");

        assertThat(resposta.frete()).isEqualByComparingTo("0.00");
        assertThat(resposta.descontoCupom()).isEqualByComparingTo("0.00");
    }

    @Test
    void ouroGanhaBrindeQuandoOsProdutosPassamDeQuinhentosReais() {
        ResumoResponse acima = resumo(List.of(item("249.90", 3, "1.20")),
                "RETIRADA_LOJA", null, "PIX", null, "OURO", "SUDESTE");
        ResumoResponse exatamenteQuinhentos = resumo(List.of(item("250.00", 2, "1.20")),
                "RETIRADA_LOJA", null, "PIX", null, "OURO", "SUDESTE");

        assertThat(acima.brinde()).isTrue();
        assertThat(acima.creditoProximaCompra()).isEqualByComparingTo("37.48");
        assertThat(exatamenteQuinhentos.brinde()).isFalse();
    }

    @Test
    void bronzeNuncaGanhaCreditoNemBrinde() {
        ResumoResponse resposta = resumo(List.of(item("249.90", 3, "1.20")),
                "RETIRADA_LOJA", null, "PIX", null, "BRONZE", "SUDESTE");

        assertThat(resposta.creditoProximaCompra()).isEqualByComparingTo("0.00");
        assertThat(resposta.brinde()).isFalse();
    }

    @Test
    void cartaoAcimaDeTresParcelasCobraJurosPelaTabelaPrice() {
        ResumoResponse resposta = resumo(List.of(item("1000.00", 1, "1.00")),
                "RETIRADA_LOJA", null, "CARTAO", 12, "BRONZE", "SUDESTE");

        assertThat(resposta.valorParcela()).isEqualByComparingTo("95.45");
        assertThat(resposta.totalFinal()).isEqualByComparingTo("1145.40");
        assertThat(resposta.ajustePagamento()).isEqualByComparingTo("135.40");
    }

    @Test
    void motoboyAtendePedidoDeExatamenteCincoQuilos() {
        ResumoResponse resposta = resumo(List.of(item("100.00", 2, "2.50")),
                "MOTOBOY", null, "PIX", null, "BRONZE", "SUDESTE");

        assertThat(resposta.frete()).isEqualByComparingTo("18.00");
        assertThat(resposta.prazoEntregaDias()).isZero();
    }

    @Test
    void leve3pague2SoDescontaAPartirDaTerceiraUnidadeDoMesmoItem() {
        ResumoResponse semTrio = resumo(List.of(item("50.00", 2, "0.20")),
                "RETIRADA_LOJA", "LEVE3PAGUE2", "PIX", null, "BRONZE", "SUDESTE");
        ResumoResponse comSeisUnidades = resumo(List.of(item("50.00", 6, "0.20")),
                "RETIRADA_LOJA", "LEVE3PAGUE2", "PIX", null, "BRONZE", "SUDESTE");

        assertThat(semTrio.descontoCupom()).isEqualByComparingTo("0.00");
        assertThat(comSeisUnidades.descontoCupom()).isEqualByComparingTo("100.00");
    }
}

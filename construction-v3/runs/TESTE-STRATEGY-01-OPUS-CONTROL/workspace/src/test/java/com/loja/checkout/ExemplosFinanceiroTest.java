package com.loja.checkout;

import static com.loja.checkout.ResumoTestData.CAMISETA;
import static com.loja.checkout.ResumoTestData.FONE;
import static com.loja.checkout.ResumoTestData.MEIA;
import static com.loja.checkout.ResumoTestData.TENIS;
import static com.loja.checkout.ResumoTestData.pedido;
import static org.assertj.core.api.Assertions.assertThat;

import com.loja.checkout.api.ResumoResponse;
import com.loja.checkout.comum.Dinheiro;
import com.loja.checkout.dominio.pagamento.ContextoPagamento;
import com.loja.checkout.dominio.pagamento.PagamentoBoleto;
import com.loja.checkout.dominio.pagamento.PagamentoCartao;
import com.loja.checkout.dominio.pagamento.PagamentoPix;
import com.loja.checkout.dominio.pagamento.ResultadoPagamento;
import com.loja.checkout.servico.CalculadoraResumoService;
import java.math.BigDecimal;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

/**
 * Exemplos 1 a 4 conferidos pelo financeiro. Esses quatro exemplos nao informam
 * regiao e os totais que eles mostram nao incluem o imposto da regiao; por isso
 * conferimos aqui as partes que eles definem (produtos, cupom, frete e prazo) e,
 * separadamente, o efeito da forma de pagamento sobre o total que eles usaram.
 * O exemplo 5, que informa regiao, e conferido ponta a ponta em CheckoutApiTest.
 */
@SpringBootTest
class ExemplosFinanceiroTest {

    @Autowired
    private CalculadoraResumoService calculadora;

    private final PagamentoPix pix = new PagamentoPix();
    private final PagamentoCartao cartao = new PagamentoCartao();
    private final PagamentoBoleto boleto = new PagamentoBoleto();

    private static BigDecimal reais(String valor) {
        return new BigDecimal(valor);
    }

    private static ContextoPagamento total(String valor) {
        return new ContextoPagamento(reais(valor), reais(valor));
    }

    @Test
    void exemplo1Partes() {
        ResumoResponse resumo = calculadora.calcular(pedido(
                List.of(CAMISETA, TENIS), "EXPRESSA", "BEMVINDO10", "PIX", 1, "BRONZE", "SUDESTE"));

        assertThat(resumo.subtotalProdutos()).isEqualByComparingTo(reais("409.70"));
        assertThat(resumo.descontoCupom()).isEqualByComparingTo(reais("40.97"));
        assertThat(resumo.frete()).isEqualByComparingTo(reais("33.10"));
        assertThat(resumo.prazoEntregaDias()).isEqualTo(2);

        ResultadoPagamento pagamento = pix.calcular(total("401.83"), 1);
        assertThat(pagamento.totalFinal()).isEqualByComparingTo(reais("381.74"));
        assertThat(pagamento.valorParcela()).isEqualByComparingTo(reais("381.74"));
    }

    @Test
    void exemplo2Partes() {
        ResumoResponse resumo = calculadora.calcular(pedido(
                List.of(CAMISETA, TENIS), "ECONOMICA", null, "CARTAO", 6, "BRONZE", "SUDESTE"));

        assertThat(resumo.subtotalProdutos()).isEqualByComparingTo(reais("409.70"));
        assertThat(resumo.descontoCupom()).isEqualByComparingTo(reais("0.00"));
        assertThat(resumo.frete()).isEqualByComparingTo(reais("15.60"));
        assertThat(resumo.prazoEntregaDias()).isEqualTo(7);

        ResultadoPagamento pagamento = cartao.calcular(total("425.30"), 6);
        assertThat(pagamento.valorParcela()).isEqualByComparingTo(reais("75.90"));
        assertThat(pagamento.totalFinal()).isEqualByComparingTo(reais("455.40"));
    }

    @Test
    void exemplo3Partes() {
        ResumoResponse resumo = calculadora.calcular(pedido(
                List.of(FONE), "MOTOBOY", "MENOS50", "BOLETO", 1, "BRONZE", "SUDESTE"));

        assertThat(resumo.subtotalProdutos()).isEqualByComparingTo(reais("399.80"));
        assertThat(resumo.descontoCupom()).isEqualByComparingTo(reais("50.00"));
        assertThat(resumo.frete()).isEqualByComparingTo(reais("18.00"));
        assertThat(resumo.prazoEntregaDias()).isZero();

        ResultadoPagamento pagamento = boleto.calcular(total("367.80"), 1);
        assertThat(pagamento.totalFinal()).isEqualByComparingTo(reais("371.29"));
    }

    @Test
    void exemplo4Partes() {
        ResumoResponse resumo = calculadora.calcular(pedido(
                List.of(MEIA, CAMISETA), "RETIRADA_LOJA", "LEVE3PAGUE2", "CARTAO", 3, "BRONZE", "SUDESTE"));

        assertThat(resumo.subtotalProdutos()).isEqualByComparingTo(reais("299.10"));
        assertThat(resumo.descontoCupom()).isEqualByComparingTo(reais("39.80"));
        assertThat(resumo.frete()).isEqualByComparingTo(reais("0.00"));
        assertThat(resumo.prazoEntregaDias()).isEqualTo(1);

        ResultadoPagamento pagamento = cartao.calcular(total("259.30"), 3);
        assertThat(pagamento.totalFinal()).isEqualByComparingTo(reais("259.30"));
        assertThat(pagamento.valorParcela()).isEqualByComparingTo(reais("86.43"));
    }

    @Test
    void arredondamentoMeioParaOPar() {
        assertThat(Dinheiro.centavos(reais("2.995"))).isEqualByComparingTo(reais("3.00"));
        assertThat(Dinheiro.centavos(reais("2.985"))).isEqualByComparingTo(reais("2.98"));
    }
}

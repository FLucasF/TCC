package com.loja.checkout;

import static com.loja.checkout.Pedidos.CAMISETA;
import static com.loja.checkout.Pedidos.FONE;
import static com.loja.checkout.Pedidos.MEIA;
import static com.loja.checkout.Pedidos.TENIS;
import static com.loja.checkout.Pedidos.pedido;
import static org.assertj.core.api.Assertions.assertThat;

import com.loja.checkout.aplicacao.ResumoCheckoutService;
import com.loja.checkout.api.ResumoResponse;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

/**
 * Exemplos conferidos pelo financeiro. Os exemplos 1 a 4 nao informam a regiao,
 * entao aqui eles rodam no Sudeste e o imposto entra normalmente; as partes que
 * o financeiro listou (produtos, cupom, frete e prazo) sao conferidas uma a uma,
 * e os valores de total final deles estao fixados em PagamentoTest.
 */
@SpringBootTest
class ExemplosFinanceiroTest {

    @Autowired
    private ResumoCheckoutService service;

    @Test
    void exemplo1_expressaComBemvindo10NoPix() {
        ResumoResponse resumo = service.calcular(pedido(CAMISETA, TENIS)
                .entrega("EXPRESSA").cupom("BEMVINDO10").pagamento("PIX").montar());

        assertThat(resumo.subtotalProdutos()).isEqualByComparingTo("409.70");
        assertThat(resumo.descontoCupom()).isEqualByComparingTo("40.97");
        assertThat(resumo.frete()).isEqualByComparingTo("33.10");
        assertThat(resumo.prazoEntregaDias()).isEqualTo(2);
        assertThat(resumo.imposto()).isEqualByComparingTo("44.25");
        assertThat(resumo.ajustePagamento()).isEqualByComparingTo("-22.30");
        assertThat(resumo.totalFinal()).isEqualByComparingTo("423.78");
        assertThat(resumo.parcelas()).isEqualTo(1);
        assertThat(resumo.valorParcela()).isEqualByComparingTo("423.78");
        assertThat(resumo.creditoProximaCompra()).isEqualByComparingTo("0.00");
        assertThat(resumo.brinde()).isFalse();
    }

    @Test
    void exemplo2_economicaSemCupomNoCartaoEmSeisVezes() {
        ResumoResponse resumo = service.calcular(pedido(CAMISETA, TENIS)
                .entrega("ECONOMICA").pagamento("CARTAO").parcelas(6).montar());

        assertThat(resumo.subtotalProdutos()).isEqualByComparingTo("409.70");
        assertThat(resumo.descontoCupom()).isEqualByComparingTo("0.00");
        assertThat(resumo.frete()).isEqualByComparingTo("15.60");
        assertThat(resumo.prazoEntregaDias()).isEqualTo(7);
        assertThat(resumo.imposto()).isEqualByComparingTo("49.16");
        assertThat(resumo.parcelas()).isEqualTo(6);
        assertThat(resumo.totalFinal())
                .isEqualByComparingTo(resumo.valorParcela().multiply(java.math.BigDecimal.valueOf(6)));
    }

    @Test
    void exemplo3_motoboyComMenos50NoBoleto() {
        ResumoResponse resumo = service.calcular(pedido(FONE)
                .entrega("MOTOBOY").cupom("MENOS50").pagamento("BOLETO").montar());

        assertThat(resumo.subtotalProdutos()).isEqualByComparingTo("399.80");
        assertThat(resumo.descontoCupom()).isEqualByComparingTo("50.00");
        assertThat(resumo.frete()).isEqualByComparingTo("18.00");
        assertThat(resumo.prazoEntregaDias()).isZero();
        assertThat(resumo.imposto()).isEqualByComparingTo("41.98");
        assertThat(resumo.ajustePagamento()).isEqualByComparingTo("3.49");
        assertThat(resumo.totalFinal()).isEqualByComparingTo("413.27");
    }

    @Test
    void exemplo4_retiradaComLeve3Pague2NoCartaoEmTresVezes() {
        ResumoResponse resumo = service.calcular(pedido(MEIA, CAMISETA)
                .entrega("RETIRADA_LOJA").cupom("LEVE3PAGUE2").pagamento("CARTAO").parcelas(3).montar());

        assertThat(resumo.subtotalProdutos()).isEqualByComparingTo("299.10");
        assertThat(resumo.descontoCupom()).isEqualByComparingTo("39.80");
        assertThat(resumo.frete()).isEqualByComparingTo("0.00");
        assertThat(resumo.prazoEntregaDias()).isEqualTo(1);
        assertThat(resumo.imposto()).isEqualByComparingTo("31.12");
        assertThat(resumo.ajustePagamento()).isEqualByComparingTo("0.00");
        assertThat(resumo.totalFinal()).isEqualByComparingTo("290.42");
        assertThat(resumo.valorParcela()).isEqualByComparingTo("96.81");
    }

    @Test
    void exemplo5_clienteOuroNoSudestePagandoComPix() {
        ResumoResponse resumo = service.calcular(pedido(CAMISETA, TENIS)
                .entrega("EXPRESSA").pagamento("PIX").clube("OURO").regiao("SUDESTE").montar());

        assertThat(resumo.subtotalProdutos()).isEqualByComparingTo("409.70");
        assertThat(resumo.descontoCupom()).isEqualByComparingTo("0.00");
        assertThat(resumo.frete()).isEqualByComparingTo("0.00");
        assertThat(resumo.prazoEntregaDias()).isEqualTo(2);
        assertThat(resumo.imposto()).isEqualByComparingTo("49.16");
        assertThat(resumo.ajustePagamento()).isEqualByComparingTo("-22.94");
        assertThat(resumo.totalFinal()).isEqualByComparingTo("435.92");
        assertThat(resumo.parcelas()).isEqualTo(1);
        assertThat(resumo.valorParcela()).isEqualByComparingTo("435.92");
        assertThat(resumo.creditoProximaCompra()).isEqualByComparingTo("20.48");
        assertThat(resumo.brinde()).isFalse();
    }
}

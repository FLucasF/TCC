package com.loja.checkout;

import com.loja.checkout.aplicacao.CalculadoraResumo;
import com.loja.checkout.aplicacao.ResumoCompra;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static com.loja.checkout.PedidoDeTeste.item;
import static com.loja.checkout.PedidoDeTeste.um;
import static org.assertj.core.api.Assertions.assertThat;

class ExemplosDoFinanceiroTest {

    private final CalculadoraResumo calculadora = new CalculadoraResumo();

    @Test
    void exemplo1_expressa_bemvindo10_pix_bronze_norte() {
        ResumoCompra resumo = calculadora.calcular(um()
                .com(item("Camiseta", "79.90", 2, "0.30"), item("Tenis", "249.90", 1, "1.20"))
                .entrega("EXPRESSA").cupom("BEMVINDO10").pagamento("PIX").clube("BRONZE").regiao("NORTE")
                .pedido());

        confere(resumo, "409.70", "40.97", "33.10", 2, "10.24", "-20.60", "391.47", 1, "391.47", "0.00", false);
    }

    @Test
    void exemplo2_economica_sem_cupom_cartao_6x_prata_centro_oeste() {
        ResumoCompra resumo = calculadora.calcular(um()
                .com(item("Camiseta", "79.90", 2, "0.30"), item("Tenis", "249.90", 1, "1.20"))
                .entrega("ECONOMICA").pagamento("CARTAO").parcelas(6).clube("PRATA").regiao("CENTRO_OESTE")
                .pedido());

        confere(resumo, "409.70", "0.00", "15.60", 7, "6.15", "30.55", "462.00", 6, "77.00", "8.19", false);
    }

    @Test
    void exemplo3_motoboy_menos50_boleto_bronze_nordeste() {
        ResumoCompra resumo = calculadora.calcular(um()
                .com(item("Fone", "199.90", 2, "0.25"))
                .entrega("MOTOBOY").cupom("MENOS50").pagamento("BOLETO").clube("BRONZE").regiao("NORDESTE")
                .pedido());

        confere(resumo, "399.80", "50.00", "18.00", 0, "8.00", "3.49", "379.29", 1, "379.29", "0.00", false);
    }

    @Test
    void exemplo4_retirada_leve3pague2_cartao_3x_prata_sul() {
        ResumoCompra resumo = calculadora.calcular(um()
                .com(item("Meia", "19.90", 7, "0.10"), item("Camiseta", "79.90", 2, "0.30"))
                .entrega("RETIRADA_LOJA").cupom("LEVE3PAGUE2").pagamento("CARTAO").parcelas(3)
                .clube("PRATA").regiao("SUL")
                .pedido());

        confere(resumo, "299.10", "39.80", "0.00", 1, "2.99", "0.00", "262.29", 3, "87.43", "5.98", false);
    }

    @Test
    void exemplo5_expressa_sem_cupom_pix_ouro_sudeste() {
        ResumoCompra resumo = calculadora.calcular(um()
                .com(item("Camiseta", "79.90", 2, "0.30"), item("Tenis", "249.90", 1, "1.20"))
                .entrega("EXPRESSA").pagamento("PIX").clube("OURO").regiao("SUDESTE")
                .pedido());

        confere(resumo, "409.70", "0.00", "0.00", 2, "4.10", "-20.69", "393.11", 1, "393.11", "20.48", false);
    }

    private void confere(ResumoCompra resumo, String subtotal, String desconto, String frete, int prazo,
                         String seguro, String ajuste, String totalFinal, int parcelas, String valorParcela,
                         String credito, boolean brinde) {
        assertThat(resumo.subtotalProdutos()).isEqualByComparingTo(subtotal);
        assertThat(resumo.descontoCupom()).isEqualByComparingTo(desconto);
        assertThat(resumo.frete()).isEqualByComparingTo(frete);
        assertThat(resumo.prazoEntregaDias()).isEqualTo(prazo);
        assertThat(resumo.seguro()).isEqualByComparingTo(seguro);
        assertThat(resumo.ajustePagamento()).isEqualByComparingTo(ajuste);
        assertThat(resumo.totalFinal()).isEqualByComparingTo(totalFinal);
        assertThat(resumo.parcelas()).isEqualTo(parcelas);
        assertThat(resumo.valorParcela()).isEqualByComparingTo(valorParcela);
        assertThat(resumo.creditoProximaCompra()).isEqualByComparingTo(credito);
        assertThat(resumo.brinde()).isEqualTo(brinde);
        assertThat(todosOsValores(resumo)).allSatisfy(valor -> assertThat(valor.scale()).isEqualTo(2));
    }

    private java.util.List<BigDecimal> todosOsValores(ResumoCompra resumo) {
        return java.util.List.of(resumo.subtotalProdutos(), resumo.descontoCupom(), resumo.frete(),
                resumo.seguro(), resumo.ajustePagamento(), resumo.totalFinal(), resumo.valorParcela(),
                resumo.creditoProximaCompra());
    }
}

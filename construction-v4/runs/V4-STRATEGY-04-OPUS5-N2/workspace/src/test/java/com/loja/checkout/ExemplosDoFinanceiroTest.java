package com.loja.checkout;

import static com.loja.checkout.Pedidos.CAMISETA;
import static com.loja.checkout.Pedidos.FONE;
import static com.loja.checkout.Pedidos.MEIA;
import static com.loja.checkout.Pedidos.TENIS;
import static org.assertj.core.api.Assertions.assertThat;

import com.loja.checkout.dominio.CalculadoraResumo;
import com.loja.checkout.dominio.PedidoRecebido;
import com.loja.checkout.dominio.ResumoCompra;
import java.math.BigDecimal;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

/** Os exemplos conferidos pelo financeiro, campo por campo. */
@SpringBootTest
class ExemplosDoFinanceiroTest {

    @Autowired
    private CalculadoraResumo calculadora;

    @Test
    @DisplayName("exemplo 1: EXPRESSA, BEMVINDO10, PIX, BRONZE, NORTE")
    void exemplo1() {
        ResumoCompra resumo = calculadora.calcular(new PedidoRecebido(
                List.of(CAMISETA, TENIS), "EXPRESSA", "BEMVINDO10", "PIX", 1, "BRONZE", "NORTE"));

        assertThat(resumo).isEqualTo(resumo("409.70", "40.97", "33.10", 2, "10.24",
                "-20.60", "391.47", 1, "391.47", "0.00", false));
    }

    @Test
    @DisplayName("exemplo 2: ECONOMICA, sem cupom, CARTAO 6x, PRATA, CENTRO_OESTE")
    void exemplo2() {
        ResumoCompra resumo = calculadora.calcular(new PedidoRecebido(
                List.of(CAMISETA, TENIS), "ECONOMICA", null, "CARTAO", 6, "PRATA", "CENTRO_OESTE"));

        assertThat(resumo).isEqualTo(resumo("409.70", "0.00", "15.60", 7, "6.15",
                "30.55", "462.00", 6, "77.00", "8.19", false));
    }

    @Test
    @DisplayName("exemplo 3: MOTOBOY, MENOS50, BOLETO, BRONZE, NORDESTE")
    void exemplo3() {
        ResumoCompra resumo = calculadora.calcular(new PedidoRecebido(
                List.of(FONE), "MOTOBOY", "MENOS50", "BOLETO", 1, "BRONZE", "NORDESTE"));

        assertThat(resumo).isEqualTo(resumo("399.80", "50.00", "18.00", 0, "8.00",
                "3.49", "379.29", 1, "379.29", "0.00", false));
    }

    @Test
    @DisplayName("exemplo 4: RETIRADA_LOJA, LEVE3PAGUE2, CARTAO 3x, PRATA, SUL")
    void exemplo4() {
        ResumoCompra resumo = calculadora.calcular(new PedidoRecebido(
                List.of(MEIA, CAMISETA), "RETIRADA_LOJA", "LEVE3PAGUE2", "CARTAO", 3, "PRATA", "SUL"));

        assertThat(resumo).isEqualTo(resumo("299.10", "39.80", "0.00", 1, "2.99",
                "0.00", "262.29", 3, "87.43", "5.98", false));
    }

    @Test
    @DisplayName("exemplo 5: EXPRESSA, sem cupom, PIX, OURO (nao paga frete), SUDESTE")
    void exemplo5() {
        ResumoCompra resumo = calculadora.calcular(new PedidoRecebido(
                List.of(CAMISETA, TENIS), "EXPRESSA", null, "PIX", 1, "OURO", "SUDESTE"));

        assertThat(resumo).isEqualTo(resumo("409.70", "0.00", "0.00", 2, "4.10",
                "-20.69", "393.11", 1, "393.11", "20.48", false));
    }

    @Test
    @DisplayName("exemplo do anexo: EXPRESSA, BEMVINDO10, PIX, OURO, SUDESTE")
    void exemploDoAnexo() {
        ResumoCompra resumo = calculadora.calcular(new PedidoRecebido(
                List.of(CAMISETA, TENIS), "EXPRESSA", "BEMVINDO10", "PIX", 1, "OURO", "SUDESTE"));

        assertThat(resumo).isEqualTo(resumo("409.70", "40.97", "0.00", 2, "4.10",
                "-18.64", "354.19", 1, "354.19", "20.48", false));
    }

    private static ResumoCompra resumo(String subtotalProdutos, String descontoCupom, String frete,
                                       int prazoEntregaDias, String seguro, String ajustePagamento,
                                       String totalFinal, int parcelas, String valorParcela,
                                       String creditoProximaCompra, boolean brinde) {
        return new ResumoCompra(new BigDecimal(subtotalProdutos), new BigDecimal(descontoCupom),
                new BigDecimal(frete), prazoEntregaDias, new BigDecimal(seguro),
                new BigDecimal(ajustePagamento), new BigDecimal(totalFinal), parcelas,
                new BigDecimal(valorParcela), new BigDecimal(creditoProximaCompra), brinde);
    }
}

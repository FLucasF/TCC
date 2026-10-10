package com.loja.checkout;

import com.loja.checkout.dominio.CalculadoraResumo;
import com.loja.checkout.dominio.Resumo;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static com.loja.checkout.PedidoBuilder.camiseta;
import static com.loja.checkout.PedidoBuilder.fone;
import static com.loja.checkout.PedidoBuilder.item;
import static com.loja.checkout.PedidoBuilder.pedido;
import static com.loja.checkout.PedidoBuilder.tenis;
import static org.assertj.core.api.Assertions.assertThat;

/** Os exemplos que o financeiro conferiu. */
class ExemplosDoFinanceiroTest {

    private final CalculadoraResumo calculadora = new CalculadoraResumo();

    @Test
    @DisplayName("exemplo do anexo: EXPRESSA, BEMVINDO10, PIX, OURO, SUDESTE")
    void exemploDoAnexo() {
        Resumo resumo = calculadora.calcular(pedido()
                .itens(camiseta(2), tenis(1))
                .entrega("EXPRESSA")
                .cupom("BEMVINDO10")
                .pagamento("PIX")
                .clube("OURO")
                .regiao("SUDESTE")
                .construir());

        assertThat(resumo.subtotalProdutos()).isEqualByComparingTo("409.70");
        assertThat(resumo.descontoCupom()).isEqualByComparingTo("40.97");
        assertThat(resumo.frete()).isEqualByComparingTo("0.00");
        assertThat(resumo.prazoEntregaDias()).isEqualTo(2);
        assertThat(resumo.seguro()).isEqualByComparingTo("4.10");
        assertThat(resumo.ajustePagamento()).isEqualByComparingTo("-18.64");
        assertThat(resumo.totalFinal()).isEqualByComparingTo("354.19");
        assertThat(resumo.parcelas()).isEqualTo(1);
        assertThat(resumo.valorParcela()).isEqualByComparingTo("354.19");
        assertThat(resumo.creditoProximaCompra()).isEqualByComparingTo("20.48");
        assertThat(resumo.brinde()).isFalse();
    }

    @Test
    @DisplayName("exemplo 1: EXPRESSA, BEMVINDO10, PIX, BRONZE, NORTE")
    void exemplo1() {
        Resumo resumo = calculadora.calcular(pedido()
                .itens(camiseta(2), tenis(1))
                .entrega("EXPRESSA")
                .cupom("BEMVINDO10")
                .pagamento("PIX")
                .clube("BRONZE")
                .regiao("NORTE")
                .construir());

        assertThat(resumo.subtotalProdutos()).isEqualByComparingTo("409.70");
        assertThat(resumo.descontoCupom()).isEqualByComparingTo("40.97");
        assertThat(resumo.frete()).isEqualByComparingTo("33.10");
        assertThat(resumo.prazoEntregaDias()).isEqualTo(2);
        assertThat(resumo.seguro()).isEqualByComparingTo("10.24");
        assertThat(resumo.ajustePagamento()).isEqualByComparingTo("-20.60");
        assertThat(resumo.totalFinal()).isEqualByComparingTo("391.47");
        assertThat(resumo.parcelas()).isEqualTo(1);
        assertThat(resumo.valorParcela()).isEqualByComparingTo("391.47");
        assertThat(resumo.creditoProximaCompra()).isEqualByComparingTo("0.00");
        assertThat(resumo.brinde()).isFalse();
    }

    @Test
    @DisplayName("exemplo 2: ECONOMICA, sem cupom, CARTAO 6x, PRATA, CENTRO_OESTE")
    void exemplo2() {
        Resumo resumo = calculadora.calcular(pedido()
                .itens(camiseta(2), tenis(1))
                .entrega("ECONOMICA")
                .pagamento("CARTAO")
                .parcelas(6)
                .clube("PRATA")
                .regiao("CENTRO_OESTE")
                .construir());

        assertThat(resumo.subtotalProdutos()).isEqualByComparingTo("409.70");
        assertThat(resumo.descontoCupom()).isEqualByComparingTo("0.00");
        assertThat(resumo.frete()).isEqualByComparingTo("15.60");
        assertThat(resumo.prazoEntregaDias()).isEqualTo(7);
        assertThat(resumo.seguro()).isEqualByComparingTo("6.15");
        assertThat(resumo.ajustePagamento()).isEqualByComparingTo("30.55");
        assertThat(resumo.totalFinal()).isEqualByComparingTo("462.00");
        assertThat(resumo.parcelas()).isEqualTo(6);
        assertThat(resumo.valorParcela()).isEqualByComparingTo("77.00");
        assertThat(resumo.creditoProximaCompra()).isEqualByComparingTo("8.19");
        assertThat(resumo.brinde()).isFalse();
    }

    @Test
    @DisplayName("exemplo 3: MOTOBOY, MENOS50, BOLETO, BRONZE, NORDESTE")
    void exemplo3() {
        Resumo resumo = calculadora.calcular(pedido()
                .itens(fone(2))
                .entrega("MOTOBOY")
                .cupom("MENOS50")
                .pagamento("BOLETO")
                .clube("BRONZE")
                .regiao("NORDESTE")
                .construir());

        assertThat(resumo.subtotalProdutos()).isEqualByComparingTo("399.80");
        assertThat(resumo.descontoCupom()).isEqualByComparingTo("50.00");
        assertThat(resumo.frete()).isEqualByComparingTo("18.00");
        assertThat(resumo.prazoEntregaDias()).isZero();
        assertThat(resumo.seguro()).isEqualByComparingTo("8.00");
        assertThat(resumo.ajustePagamento()).isEqualByComparingTo("3.49");
        assertThat(resumo.totalFinal()).isEqualByComparingTo("379.29");
        assertThat(resumo.parcelas()).isEqualTo(1);
        assertThat(resumo.valorParcela()).isEqualByComparingTo("379.29");
        assertThat(resumo.creditoProximaCompra()).isEqualByComparingTo("0.00");
        assertThat(resumo.brinde()).isFalse();
    }

    @Test
    @DisplayName("exemplo 4: RETIRADA_LOJA, LEVE3PAGUE2, CARTAO 3x, PRATA, SUL")
    void exemplo4() {
        Resumo resumo = calculadora.calcular(pedido()
                .itens(item("Meia", "19.90", 7, "0.10"), camiseta(2))
                .entrega("RETIRADA_LOJA")
                .cupom("LEVE3PAGUE2")
                .pagamento("CARTAO")
                .parcelas(3)
                .clube("PRATA")
                .regiao("SUL")
                .construir());

        assertThat(resumo.subtotalProdutos()).isEqualByComparingTo("299.10");
        assertThat(resumo.descontoCupom()).isEqualByComparingTo("39.80");
        assertThat(resumo.frete()).isEqualByComparingTo("0.00");
        assertThat(resumo.prazoEntregaDias()).isEqualTo(1);
        assertThat(resumo.seguro()).isEqualByComparingTo("2.99");
        assertThat(resumo.ajustePagamento()).isEqualByComparingTo("0.00");
        assertThat(resumo.totalFinal()).isEqualByComparingTo("262.29");
        assertThat(resumo.parcelas()).isEqualTo(3);
        assertThat(resumo.valorParcela()).isEqualByComparingTo("87.43");
        assertThat(resumo.creditoProximaCompra()).isEqualByComparingTo("5.98");
        assertThat(resumo.brinde()).isFalse();
    }

    @Test
    @DisplayName("exemplo 5: EXPRESSA, sem cupom, PIX, OURO, SUDESTE")
    void exemplo5() {
        Resumo resumo = calculadora.calcular(pedido()
                .itens(camiseta(2), tenis(1))
                .entrega("EXPRESSA")
                .pagamento("PIX")
                .clube("OURO")
                .regiao("SUDESTE")
                .construir());

        assertThat(resumo.subtotalProdutos()).isEqualByComparingTo("409.70");
        assertThat(resumo.descontoCupom()).isEqualByComparingTo("0.00");
        assertThat(resumo.frete()).isEqualByComparingTo("0.00");
        assertThat(resumo.prazoEntregaDias()).isEqualTo(2);
        assertThat(resumo.seguro()).isEqualByComparingTo("4.10");
        assertThat(resumo.ajustePagamento()).isEqualByComparingTo("-20.69");
        assertThat(resumo.totalFinal()).isEqualByComparingTo("393.11");
        assertThat(resumo.parcelas()).isEqualTo(1);
        assertThat(resumo.valorParcela()).isEqualByComparingTo("393.11");
        assertThat(resumo.creditoProximaCompra()).isEqualByComparingTo("20.48");
        assertThat(resumo.brinde()).isFalse();
    }

    @Test
    @DisplayName("todo valor em dinheiro sai com duas casas decimais")
    void valoresComDuasCasas() {
        Resumo resumo = calculadora.calcular(pedido().construir());

        assertThat(resumo.subtotalProdutos().scale()).isEqualTo(2);
        assertThat(resumo.descontoCupom().scale()).isEqualTo(2);
        assertThat(resumo.frete().scale()).isEqualTo(2);
        assertThat(resumo.seguro().scale()).isEqualTo(2);
        assertThat(resumo.ajustePagamento().scale()).isEqualTo(2);
        assertThat(resumo.totalFinal().scale()).isEqualTo(2);
        assertThat(resumo.valorParcela().scale()).isEqualTo(2);
        assertThat(resumo.creditoProximaCompra().scale()).isEqualTo(2);
    }

    @Test
    @DisplayName("arredonda meio para o par em cada etapa")
    void arredondaMeioParaOPar() {
        BigDecimal credito = calculadora.calcular(pedido()
                .itens(camiseta(2), tenis(1))
                .entrega("RETIRADA_LOJA")
                .clube("OURO")
                .construir())
                .creditoProximaCompra();

        // 5% de 409,70 = 20,485 -> 20,48
        assertThat(credito).isEqualByComparingTo("20.48");
    }
}

package com.loja.resumo.calculo;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.math.BigDecimal;
import java.util.List;
import org.junit.jupiter.api.Test;

class ResumoCompraServiceTest {

    private final ResumoCompraService service = new ResumoCompraService();

    private static final Item CAMISETA = item("Camiseta", "79.90", 2, "0.30");
    private static final Item TENIS = item("Tênis", "249.90", 1, "1.20");

    @Test
    void exemplo1PixComCupomBemVindo() {
        ResumoCompra r = service.calcular(pedido(List.of(CAMISETA, TENIS),
                "EXPRESSA", "BEMVINDO10", "PIX", null, "BRONZE", "NORTE"));

        assertResumo(r, "409.70", "40.97", "33.10", 2, "10.24", "-20.60", "391.47", 1, "391.47", "0.00", false);
    }

    @Test
    void exemplo2CartaoSeisVezesComClubePrata() {
        ResumoCompra r = service.calcular(pedido(List.of(CAMISETA, TENIS),
                "ECONOMICA", null, "CARTAO", 6, "PRATA", "CENTRO_OESTE"));

        assertResumo(r, "409.70", "0.00", "15.60", 7, "6.15", "30.55", "462.00", 6, "77.00", "8.19", false);
    }

    @Test
    void exemplo3BoletoComMotoboyEMenos50() {
        ResumoCompra r = service.calcular(pedido(List.of(item("Fone", "199.90", 2, "0.25")),
                "MOTOBOY", "MENOS50", "BOLETO", null, "BRONZE", "NORDESTE"));

        assertResumo(r, "399.80", "50.00", "18.00", 0, "8.00", "3.49", "379.29", 1, "379.29", "0.00", false);
    }

    @Test
    void exemplo4CartaoTresVezesLeve3Pague2() {
        ResumoCompra r = service.calcular(pedido(
                List.of(item("Meia", "19.90", 7, "0.10"), item("Camiseta", "79.90", 2, "0.30")),
                "RETIRADA_LOJA", "LEVE3PAGUE2", "CARTAO", 3, "PRATA", "SUL"));

        assertResumo(r, "299.10", "39.80", "0.00", 1, "2.99", "0.00", "262.29", 3, "87.43", "5.98", false);
    }

    @Test
    void exemplo5PixComClubeOuroZeraFrete() {
        ResumoCompra r = service.calcular(pedido(List.of(CAMISETA, TENIS),
                "EXPRESSA", null, "PIX", null, "OURO", "SUDESTE"));

        assertResumo(r, "409.70", "0.00", "0.00", 2, "4.10", "-20.69", "393.11", 1, "393.11", "20.48", false);
    }

    @Test
    void ouroGanhaBrindeSoAcimaDeQuinhentos() {
        ResumoCompra acima = service.calcular(pedido(List.of(item("Jaqueta", "500.01", 1, "0.80")),
                "RETIRADA_LOJA", null, "PIX", null, "OURO", "SUL"));
        ResumoCompra exato = service.calcular(pedido(List.of(item("Jaqueta", "500.00", 1, "0.80")),
                "RETIRADA_LOJA", null, "PIX", null, "OURO", "SUL"));

        assertTrue(acima.brinde());
        assertFalse(exato.brinde());
    }

    @Test
    void freteGratisDescontaOValorDoFrete() {
        ResumoCompra comum = service.calcular(pedido(List.of(CAMISETA, TENIS),
                "EXPRESSA", "FRETEGRATIS", "PIX", null, "BRONZE", "SUDESTE"));
        ResumoCompra ouro = service.calcular(pedido(List.of(CAMISETA, TENIS),
                "EXPRESSA", "FRETEGRATIS", "PIX", null, "OURO", "SUDESTE"));

        assertEquals(new BigDecimal("33.10"), comum.descontoCupom());
        assertEquals(new BigDecimal("0.00"), ouro.descontoCupom());
        assertEquals(new BigDecimal("0.00"), ouro.frete());
    }

    @Test
    void limitesDeBordaSaoAceitos() {
        ResumoCompra cincoKg = service.calcular(pedido(List.of(item("Mala", "300.00", 1, "5.00")),
                "MOTOBOY", "MENOS50", "PIX", null, "BRONZE", "SUL"));
        assertEquals(new BigDecimal("18.00"), cincoKg.frete());
        assertEquals(new BigDecimal("50.00"), cincoKg.descontoCupom());

        ResumoCompra mil = service.calcular(pedido(List.of(item("Casaco", "990.00", 1, "0.50")),
                "RETIRADA_LOJA", null, "BOLETO", null, "BRONZE", "SUDESTE"));
        assertEquals(new BigDecimal("1003.39"), mil.totalFinal());

        ResumoCompra doze = service.calcular(pedido(List.of(CAMISETA),
                "RETIRADA_LOJA", null, "CARTAO", 12, "BRONZE", "SUDESTE"));
        assertEquals(12, doze.parcelas());
        assertEquals(doze.totalFinal(), doze.valorParcela().multiply(new BigDecimal("12")).setScale(2));
    }

    @Test
    void carrinhoInvalidoRecusaPedido() {
        assertEquals("PEDIDO_INVALIDO", recusa(pedido(List.of(),
                "EXPRESSA", null, "PIX", null, "BRONZE", "SUL")));
        assertEquals("PEDIDO_INVALIDO", recusa(pedido(List.of(item("Meia", "19.90", 1, "0")),
                "EXPRESSA", null, "PIX", null, "BRONZE", "SUL")));
        assertEquals("PEDIDO_INVALIDO", recusa(pedido(List.of(item("Meia", "19.90", null, "0.10")),
                "EXPRESSA", null, "PIX", null, "BRONZE", "SUL")));
    }

    @Test
    void errosSeguemaOrdemDeValidacao() {
        assertEquals("NIVEL_CLUBE_INVALIDO", recusa(pedido(List.of(CAMISETA),
                "EXPRESSA", "CUPOM_QUALQUER", "PIX", null, "DIAMANTE", "SUL")));
        assertEquals("REGIAO_INVALIDA", recusa(pedido(List.of(CAMISETA),
                "EXPRESSA", null, "PIX", null, "BRONZE", null)));
        assertEquals("MODALIDADE_INVALIDA", recusa(pedido(List.of(CAMISETA),
                "DRONE", null, "PIX", null, "BRONZE", "SUL")));
        assertEquals("MODALIDADE_INDISPONIVEL", recusa(pedido(List.of(item("Mala", "300.00", 1, "5.01")),
                "MOTOBOY", null, "PIX", null, "BRONZE", "SUL")));
        assertEquals("CUPOM_INVALIDO", recusa(pedido(List.of(CAMISETA),
                "EXPRESSA", "bemvindo10", "PIX", null, "BRONZE", "SUL")));
        assertEquals("CUPOM_NAO_APLICAVEL", recusa(pedido(List.of(item("Meia", "299.99", 1, "0.10")),
                "EXPRESSA", "MENOS50", "PIX", null, "BRONZE", "SUL")));
        assertEquals("FORMA_PAGAMENTO_INVALIDA", recusa(pedido(List.of(CAMISETA),
                "EXPRESSA", null, "DINHEIRO", null, "BRONZE", "SUL")));
        assertEquals("PARCELAMENTO_INVALIDO", recusa(pedido(List.of(CAMISETA),
                "EXPRESSA", null, "PIX", 2, "BRONZE", "SUL")));
        assertEquals("PARCELAMENTO_INVALIDO", recusa(pedido(List.of(CAMISETA),
                "EXPRESSA", null, "CARTAO", 13, "BRONZE", "SUL")));
        assertEquals("FORMA_PAGAMENTO_INDISPONIVEL", recusa(pedido(List.of(item("Casaco", "1000.00", 1, "0.50")),
                "EXPRESSA", null, "BOLETO", null, "BRONZE", "SUL")));
    }

    private String recusa(Pedido pedido) {
        return assertThrows(PedidoRecusadoException.class, () -> service.calcular(pedido)).getCodigo();
    }

    private static void assertResumo(ResumoCompra r, String subtotal, String desconto, String frete, int prazo,
            String seguro, String ajuste, String totalFinal, int parcelas, String valorParcela, String credito,
            boolean brinde) {
        assertEquals(new BigDecimal(subtotal), r.subtotalProdutos(), "subtotal");
        assertEquals(new BigDecimal(desconto), r.descontoCupom(), "desconto");
        assertEquals(new BigDecimal(frete), r.frete(), "frete");
        assertEquals(prazo, r.prazoEntregaDias(), "prazo");
        assertEquals(new BigDecimal(seguro), r.seguro(), "seguro");
        assertEquals(new BigDecimal(ajuste), r.ajustePagamento(), "ajuste");
        assertEquals(new BigDecimal(totalFinal), r.totalFinal(), "total final");
        assertEquals(parcelas, r.parcelas(), "parcelas");
        assertEquals(new BigDecimal(valorParcela), r.valorParcela(), "valor da parcela");
        assertEquals(new BigDecimal(credito), r.creditoProximaCompra(), "crédito");
        assertEquals(brinde, r.brinde(), "brinde");
    }

    private static Item item(String nome, String preco, Integer quantidade, String peso) {
        return new Item(nome, new BigDecimal(preco), quantidade, peso == null ? null : new BigDecimal(peso));
    }

    private static Pedido pedido(List<Item> itens, String entrega, String cupom, String forma, Integer parcelas,
            String nivel, String regiao) {
        return new Pedido(itens, entrega, cupom, forma, parcelas, nivel, regiao);
    }
}

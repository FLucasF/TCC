package loja.checkout;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.math.BigDecimal;
import java.util.List;
import loja.checkout.PedidoRequest.ItemRequest;
import loja.checkout.clube.Bronze;
import loja.checkout.clube.Ouro;
import loja.checkout.clube.Prata;
import loja.checkout.cupom.BemVindo10;
import loja.checkout.cupom.FreteGratis;
import loja.checkout.cupom.Leve3Pague2;
import loja.checkout.cupom.Menos50;
import loja.checkout.entrega.EntregaEconomica;
import loja.checkout.entrega.EntregaExpressa;
import loja.checkout.entrega.Motoboy;
import loja.checkout.entrega.RetiradaLoja;
import loja.checkout.pagamento.Boleto;
import loja.checkout.pagamento.Cartao;
import loja.checkout.pagamento.Pix;
import org.junit.jupiter.api.Test;

class ResumoServiceTest {
    private final ResumoService service = new ResumoService(
            List.of(new EntregaEconomica(), new EntregaExpressa(), new RetiradaLoja(), new Motoboy()),
            List.of(new BemVindo10(), new Menos50(), new FreteGratis(), new Leve3Pague2()),
            List.of(new Bronze(), new Prata(), new Ouro()),
            List.of(new Pix(), new Cartao(), new Boleto()));

    private static ItemRequest item(String nome, String preco, int qtd, String peso) {
        return new ItemRequest(nome, new BigDecimal(preco), qtd, new BigDecimal(peso));
    }

    private static final List<ItemRequest> CAMISETA_TENIS =
            List.of(item("Camiseta", "79.90", 2, "0.30"), item("Tenis", "249.90", 1, "1.20"));

    private static void confere(ResumoResponse r, String subtotal, String cupom, String frete, int prazo,
                                String seguro, String ajuste, String total, int parcelas, String parcela,
                                String credito, boolean brinde) {
        assertEquals(new BigDecimal(subtotal), r.subtotalProdutos());
        assertEquals(new BigDecimal(cupom), r.descontoCupom());
        assertEquals(new BigDecimal(frete), r.frete());
        assertEquals(prazo, r.prazoEntregaDias());
        assertEquals(new BigDecimal(seguro), r.seguro());
        assertEquals(new BigDecimal(ajuste), r.ajustePagamento());
        assertEquals(new BigDecimal(total), r.totalFinal());
        assertEquals(parcelas, r.parcelas());
        assertEquals(new BigDecimal(parcela), r.valorParcela());
        assertEquals(new BigDecimal(credito), r.creditoProximaCompra());
        assertEquals(brinde, r.brinde());
    }

    private static String erro(ResumoService s, PedidoRequest p) {
        return assertThrows(ErroNegocio.class, () -> s.calcular(p)).codigo();
    }

    @Test
    void exemplo1() {
        var r = service.calcular(new PedidoRequest(CAMISETA_TENIS, "EXPRESSA", "BEMVINDO10", "PIX", null, "BRONZE", "NORTE"));
        confere(r, "409.70", "40.97", "33.10", 2, "10.24", "-20.60", "391.47", 1, "391.47", "0.00", false);
    }

    @Test
    void exemplo2() {
        var r = service.calcular(new PedidoRequest(CAMISETA_TENIS, "ECONOMICA", null, "CARTAO", 6, "PRATA", "CENTRO_OESTE"));
        confere(r, "409.70", "0.00", "15.60", 7, "6.15", "30.55", "462.00", 6, "77.00", "8.19", false);
    }

    @Test
    void exemplo3() {
        var r = service.calcular(new PedidoRequest(List.of(item("Fone", "199.90", 2, "0.25")), "MOTOBOY", "MENOS50", "BOLETO", 1, "BRONZE", "NORDESTE"));
        confere(r, "399.80", "50.00", "18.00", 0, "8.00", "3.49", "379.29", 1, "379.29", "0.00", false);
    }

    @Test
    void exemplo4() {
        var itens = List.of(item("Meia", "19.90", 7, "0.10"), item("Camiseta", "79.90", 2, "0.30"));
        var r = service.calcular(new PedidoRequest(itens, "RETIRADA_LOJA", "LEVE3PAGUE2", "CARTAO", 3, "PRATA", "SUL"));
        confere(r, "299.10", "39.80", "0.00", 1, "2.99", "0.00", "262.29", 3, "87.43", "5.98", false);
    }

    @Test
    void exemplo5() {
        var r = service.calcular(new PedidoRequest(CAMISETA_TENIS, "EXPRESSA", null, "PIX", null, "OURO", "SUDESTE"));
        confere(r, "409.70", "0.00", "0.00", 2, "4.10", "-20.69", "393.11", 1, "393.11", "20.48", false);
    }

    @Test
    void freteGratisIgualaODescontoAoFrete() {
        var r = service.calcular(new PedidoRequest(CAMISETA_TENIS, "EXPRESSA", "FRETEGRATIS", "PIX", 1, "BRONZE", "SUL"));
        assertEquals(r.frete(), r.descontoCupom());
    }

    @Test
    void erros() {
        var base = new PedidoRequest(CAMISETA_TENIS, "EXPRESSA", null, "PIX", 1, "BRONZE", "SUL");
        assertEquals("PEDIDO_INVALIDO", erro(service, new PedidoRequest(List.of(), "EXPRESSA", null, "PIX", 1, "BRONZE", "SUL")));
        assertEquals("NIVEL_CLUBE_INVALIDO", erro(service, new PedidoRequest(base.itens(), "EXPRESSA", null, "PIX", 1, "DIAMANTE", "SUL")));
        assertEquals("REGIAO_INVALIDA", erro(service, new PedidoRequest(base.itens(), "EXPRESSA", null, "PIX", 1, "BRONZE", null)));
        assertEquals("MODALIDADE_INVALIDA", erro(service, new PedidoRequest(base.itens(), "DRONE", null, "PIX", 1, "BRONZE", "SUL")));
        var pesado = List.of(item("Sofa", "100.00", 1, "6.00"));
        assertEquals("MODALIDADE_INDISPONIVEL", erro(service, new PedidoRequest(pesado, "MOTOBOY", null, "PIX", 1, "BRONZE", "SUL")));
        assertEquals("CUPOM_INVALIDO", erro(service, new PedidoRequest(base.itens(), "EXPRESSA", "XYZ", "PIX", 1, "BRONZE", "SUL")));
        var barato = List.of(item("Meia", "10.00", 1, "0.10"));
        assertEquals("CUPOM_NAO_APLICAVEL", erro(service, new PedidoRequest(barato, "EXPRESSA", "MENOS50", "PIX", 1, "BRONZE", "SUL")));
        assertEquals("FORMA_PAGAMENTO_INVALIDA", erro(service, new PedidoRequest(base.itens(), "EXPRESSA", null, "CHEQUE", 1, "BRONZE", "SUL")));
        assertEquals("PARCELAMENTO_INVALIDO", erro(service, new PedidoRequest(base.itens(), "EXPRESSA", null, "PIX", 2, "BRONZE", "SUL")));
        var caro = List.of(item("Joia", "1500.00", 1, "0.10"));
        assertEquals("FORMA_PAGAMENTO_INDISPONIVEL", erro(service, new PedidoRequest(caro, "EXPRESSA", null, "BOLETO", 1, "BRONZE", "SUL")));
    }

    @Test
    void bordas() {
        var tre = List.of(item("A", "100.00", 3, "0.10"));
        assertEquals(new BigDecimal("50.00"),
                service.calcular(new PedidoRequest(tre, "RETIRADA_LOJA", "MENOS50", "PIX", 1, "BRONZE", "SUL")).descontoCupom());
        var cinco = List.of(item("A", "10.00", 1, "5.00"));
        assertEquals(new BigDecimal("18.00"),
                service.calcular(new PedidoRequest(cinco, "MOTOBOY", null, "PIX", 1, "BRONZE", "SUL")).frete());
        var quinhentos = List.of(item("A", "500.00", 1, "0.10"));
        var acima = List.of(item("A", "500.01", 1, "0.10"));
        assertEquals(false, service.calcular(new PedidoRequest(quinhentos, "RETIRADA_LOJA", null, "PIX", 1, "OURO", "SUL")).brinde());
        assertEquals(true, service.calcular(new PedidoRequest(acima, "RETIRADA_LOJA", null, "PIX", 1, "OURO", "SUL")).brinde());
        assertEquals("PARCELAMENTO_INVALIDO", erro(service, new PedidoRequest(tre, "RETIRADA_LOJA", null, "CARTAO", 13, "BRONZE", "SUL")));
        assertEquals("PARCELAMENTO_INVALIDO", erro(service, new PedidoRequest(tre, "RETIRADA_LOJA", null, "CARTAO", 0, "BRONZE", "SUL")));
        assertEquals(1, service.calcular(new PedidoRequest(tre, "RETIRADA_LOJA", null, "CARTAO", 4, "BRONZE", "SUL"))
                .ajustePagamento().signum());
        var ruim = List.of(item("A", "10.00", 1, "0"));
        assertEquals("PEDIDO_INVALIDO", erro(service, new PedidoRequest(ruim, "RETIRADA_LOJA", null, "PIX", 1, "BRONZE", "SUL")));
    }
}

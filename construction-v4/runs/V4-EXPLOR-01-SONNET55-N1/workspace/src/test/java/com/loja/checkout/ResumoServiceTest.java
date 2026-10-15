package com.loja.checkout;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.loja.checkout.clube.*;
import com.loja.checkout.cupom.*;
import com.loja.checkout.entrega.*;
import com.loja.checkout.pagamento.*;
import java.math.BigDecimal;
import java.util.List;
import org.junit.jupiter.api.Test;

class ResumoServiceTest {
    private final ResumoService service = new ResumoService(
            new Catalogo<>(List.of(new EntregaEconomica(), new EntregaExpressa(), new EntregaRetiradaLoja(),
                    new EntregaMotoboy())),
            new Catalogo<>(List.of(new CupomBemVindo10(), new CupomMenos50(), new CupomFreteGratis(),
                    new CupomLeve3Pague2())),
            new Catalogo<>(List.of(new NivelBronze(), new NivelPrata(), new NivelOuro())),
            new Catalogo<>(List.of(new PagamentoPix(), new PagamentoCartao(), new PagamentoBoleto())));

    private static Item item(String preco, int qtd, String peso) {
        return new Item("x", new BigDecimal(preco), qtd, new BigDecimal(peso));
    }

    private static final List<Item> CAMISETA_TENIS = List.of(item("79.90", 2, "0.30"), item("249.90", 1, "1.20"));

    private static BigDecimal d(String s) {
        return new BigDecimal(s);
    }

    private void confere(ResumoResponse r, String sub, String cupom, String frete, int prazo, String seguro,
            String ajuste, String total, int parcelas, String parcela, String credito, boolean brinde) {
        assertEquals(d(sub), r.subtotalProdutos());
        assertEquals(d(cupom), r.descontoCupom());
        assertEquals(d(frete), r.frete());
        assertEquals(prazo, r.prazoEntregaDias());
        assertEquals(d(seguro), r.seguro());
        assertEquals(d(ajuste), r.ajustePagamento());
        assertEquals(d(total), r.totalFinal());
        assertEquals(parcelas, r.parcelas());
        assertEquals(d(parcela), r.valorParcela());
        assertEquals(d(credito), r.creditoProximaCompra());
        assertEquals(brinde, r.brinde());
    }

    @Test
    void exemplo1() {
        confere(service.calcular(new PedidoRequest(CAMISETA_TENIS, "EXPRESSA", "BEMVINDO10", "PIX", null,
                "BRONZE", "NORTE")),
                "409.70", "40.97", "33.10", 2, "10.24", "-20.60", "391.47", 1, "391.47", "0.00", false);
    }

    @Test
    void exemplo2() {
        confere(service.calcular(new PedidoRequest(CAMISETA_TENIS, "ECONOMICA", null, "CARTAO", 6, "PRATA",
                "CENTRO_OESTE")),
                "409.70", "0.00", "15.60", 7, "6.15", "30.55", "462.00", 6, "77.00", "8.19", false);
    }

    @Test
    void exemplo3() {
        confere(service.calcular(new PedidoRequest(List.of(item("199.90", 2, "0.25")), "MOTOBOY", "MENOS50",
                "BOLETO", 1, "BRONZE", "NORDESTE")),
                "399.80", "50.00", "18.00", 0, "8.00", "3.49", "379.29", 1, "379.29", "0.00", false);
    }

    @Test
    void exemplo4() {
        confere(service.calcular(new PedidoRequest(List.of(item("19.90", 7, "0.10"), item("79.90", 2, "0.30")),
                "RETIRADA_LOJA", "LEVE3PAGUE2", "CARTAO", 3, "PRATA", "SUL")),
                "299.10", "39.80", "0.00", 1, "2.99", "0.00", "262.29", 3, "87.43", "5.98", false);
    }

    @Test
    void exemplo5() {
        confere(service.calcular(new PedidoRequest(CAMISETA_TENIS, "EXPRESSA", null, "PIX", 1, "OURO",
                "SUDESTE")),
                "409.70", "0.00", "0.00", 2, "4.10", "-20.69", "393.11", 1, "393.11", "20.48", false);
    }

    @Test
    void freteGratisMostraFreteEDescontoIguais() {
        ResumoResponse r = service.calcular(new PedidoRequest(CAMISETA_TENIS, "EXPRESSA", "FRETEGRATIS", "PIX",
                1, "BRONZE", "SUL"));
        assertEquals(r.frete(), r.descontoCupom());
    }

    @Test
    void ouroAcimaDe500GanhaBrinde() {
        assertEquals(true, service.calcular(new PedidoRequest(List.of(item("250.01", 2, "0.1")), "MOTOBOY", null,
                "PIX", 1, "OURO", "SUL")).brinde());
    }

    private CodigoErro erro(PedidoRequest p) {
        return assertThrows(PedidoRecusadoException.class, () -> service.calcular(p)).codigo();
    }

    @Test
    void erros() {
        List<Item> ok = CAMISETA_TENIS;
        assertEquals(CodigoErro.PEDIDO_INVALIDO, erro(new PedidoRequest(List.of(), "PIX", null, "PIX", 1, "OURO", "SUL")));
        assertEquals(CodigoErro.NIVEL_CLUBE_INVALIDO, erro(new PedidoRequest(ok, "MOTOBOY", null, "PIX", 1, null, "SUL")));
        assertEquals(CodigoErro.REGIAO_INVALIDA, erro(new PedidoRequest(ok, "MOTOBOY", null, "PIX", 1, "OURO", "X")));
        assertEquals(CodigoErro.MODALIDADE_INVALIDA, erro(new PedidoRequest(ok, "X", null, "PIX", 1, "OURO", "SUL")));
        assertEquals(CodigoErro.MODALIDADE_INDISPONIVEL, erro(new PedidoRequest(List.of(item("10", 1, "5.01")),
                "MOTOBOY", null, "PIX", 1, "OURO", "SUL")));
        assertEquals(CodigoErro.CUPOM_INVALIDO, erro(new PedidoRequest(ok, "MOTOBOY", "XX", "PIX", 1, "OURO", "SUL")));
        assertEquals(CodigoErro.CUPOM_NAO_APLICAVEL, erro(new PedidoRequest(List.of(item("10", 1, "1")),
                "MOTOBOY", "MENOS50", "PIX", 1, "OURO", "SUL")));
        assertEquals(CodigoErro.FORMA_PAGAMENTO_INVALIDA, erro(new PedidoRequest(ok, "MOTOBOY", null, null, 1, "OURO", "SUL")));
        assertEquals(CodigoErro.PARCELAMENTO_INVALIDO, erro(new PedidoRequest(ok, "MOTOBOY", null, "PIX", 2, "OURO", "SUL")));
        assertEquals(CodigoErro.PARCELAMENTO_INVALIDO, erro(new PedidoRequest(ok, "MOTOBOY", null, "CARTAO", 13, "OURO", "SUL")));
        assertEquals(CodigoErro.FORMA_PAGAMENTO_INDISPONIVEL, erro(new PedidoRequest(List.of(item("1000", 1, "1")),
                "MOTOBOY", null, "BOLETO", 1, "OURO", "SUL")));
    }
}

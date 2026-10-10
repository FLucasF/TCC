package com.loja.checkout;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.loja.checkout.ResumoRequest.ItemRequest;
import com.loja.checkout.clube.*;
import com.loja.checkout.cupom.*;
import com.loja.checkout.entrega.*;
import com.loja.checkout.pagamento.*;
import java.math.BigDecimal;
import java.util.List;
import org.junit.jupiter.api.Test;

class ResumoServiceTest {
    private final ResumoService servico = new ResumoService(
            List.of(new Economica(), new Expressa(), new RetiradaLoja(), new Motoboy()),
            List.of(new Bemvindo10(), new Menos50(), new FreteGratis(), new Leve3Pague2()),
            List.of(new Bronze(), new Prata(), new Ouro()),
            List.of(new Pix(), new Cartao(), new Boleto()));

    private static final List<ItemRequest> CAMISETA_TENIS = List.of(
            new ItemRequest("Camiseta", new BigDecimal("79.90"), 2, new BigDecimal("0.30")),
            new ItemRequest("Tenis", new BigDecimal("249.90"), 1, new BigDecimal("1.20")));

    private static BigDecimal d(String v) { return new BigDecimal(v); }

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
        confere(servico.resumir(new ResumoRequest(CAMISETA_TENIS, "EXPRESSA", "BEMVINDO10", "PIX", null, "BRONZE", "NORTE")),
                "409.70", "40.97", "33.10", 2, "10.24", "-20.60", "391.47", 1, "391.47", "0.00", false);
    }

    @Test
    void exemplo2() {
        confere(servico.resumir(new ResumoRequest(CAMISETA_TENIS, "ECONOMICA", null, "CARTAO", 6, "PRATA", "CENTRO_OESTE")),
                "409.70", "0.00", "15.60", 7, "6.15", "30.55", "462.00", 6, "77.00", "8.19", false);
    }

    @Test
    void exemplo3() {
        var itens = List.of(new ItemRequest("Fone", new BigDecimal("199.90"), 2, new BigDecimal("0.25")));
        confere(servico.resumir(new ResumoRequest(itens, "MOTOBOY", "MENOS50", "BOLETO", 1, "BRONZE", "NORDESTE")),
                "399.80", "50.00", "18.00", 0, "8.00", "3.49", "379.29", 1, "379.29", "0.00", false);
    }

    @Test
    void exemplo4() {
        var itens = List.of(
                new ItemRequest("Meia", new BigDecimal("19.90"), 7, new BigDecimal("0.10")),
                new ItemRequest("Camiseta", new BigDecimal("79.90"), 2, new BigDecimal("0.30")));
        confere(servico.resumir(new ResumoRequest(itens, "RETIRADA_LOJA", "LEVE3PAGUE2", "CARTAO", 3, "PRATA", "SUL")),
                "299.10", "39.80", "0.00", 1, "2.99", "0.00", "262.29", 3, "87.43", "5.98", false);
    }

    @Test
    void exemplo5() {
        confere(servico.resumir(new ResumoRequest(CAMISETA_TENIS, "EXPRESSA", null, "PIX", null, "OURO", "SUDESTE")),
                "409.70", "0.00", "0.00", 2, "4.10", "-20.69", "393.11", 1, "393.11", "20.48", false);
    }

    @Test
    void erros() {
        var base = new ResumoRequest(CAMISETA_TENIS, "EXPRESSA", null, "PIX", null, "OURO", "SUDESTE");
        assertEquals(CodigoErro.PEDIDO_INVALIDO, erro(new ResumoRequest(List.of(), "EXPRESSA", null, "PIX", null, "OURO", "SUDESTE")));
        assertEquals(CodigoErro.NIVEL_CLUBE_INVALIDO, erro(new ResumoRequest(CAMISETA_TENIS, "EXPRESSA", null, "PIX", null, "X", "SUDESTE")));
        assertEquals(CodigoErro.REGIAO_INVALIDA, erro(new ResumoRequest(CAMISETA_TENIS, "EXPRESSA", null, "PIX", null, "OURO", null)));
        assertEquals(CodigoErro.MODALIDADE_INVALIDA, erro(new ResumoRequest(CAMISETA_TENIS, "X", null, "PIX", null, "OURO", "SUL")));
        assertEquals(CodigoErro.CUPOM_INVALIDO, erro(new ResumoRequest(CAMISETA_TENIS, "EXPRESSA", "X", "PIX", null, "OURO", "SUL")));
        assertEquals(CodigoErro.CUPOM_NAO_APLICAVEL, erro(new ResumoRequest(
                List.of(new ItemRequest("A", new BigDecimal("10"), 1, new BigDecimal("1"))), "EXPRESSA", "MENOS50", "PIX", null, "OURO", "SUL")));
        assertEquals(CodigoErro.FORMA_PAGAMENTO_INVALIDA, erro(new ResumoRequest(CAMISETA_TENIS, "EXPRESSA", null, null, null, "OURO", "SUL")));
        assertEquals(CodigoErro.PARCELAMENTO_INVALIDO, erro(new ResumoRequest(CAMISETA_TENIS, "EXPRESSA", null, "PIX", 2, "OURO", "SUL")));
        assertEquals(CodigoErro.MODALIDADE_INDISPONIVEL, erro(new ResumoRequest(
                List.of(new ItemRequest("A", new BigDecimal("10"), 1, new BigDecimal("6"))), "MOTOBOY", null, "PIX", null, "OURO", "SUL")));
        assertEquals(CodigoErro.FORMA_PAGAMENTO_INDISPONIVEL, erro(new ResumoRequest(
                List.of(new ItemRequest("A", new BigDecimal("1100"), 1, new BigDecimal("1"))), "EXPRESSA", null, "BOLETO", null, "OURO", "SUL")));
    }

    private CodigoErro erro(ResumoRequest r) {
        return assertThrows(PedidoRecusadoException.class, () -> servico.resumir(r)).codigo();
    }
}

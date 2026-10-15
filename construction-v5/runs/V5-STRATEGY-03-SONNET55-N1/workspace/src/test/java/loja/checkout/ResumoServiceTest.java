package loja.checkout;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.math.BigDecimal;
import java.util.List;
import loja.checkout.clube.Bronze;
import loja.checkout.clube.Ouro;
import loja.checkout.clube.Prata;
import loja.checkout.cupom.Bemvindo10;
import loja.checkout.cupom.FreteGratis;
import loja.checkout.cupom.Leve3Pague2;
import loja.checkout.cupom.Menos50;
import loja.checkout.dominio.CodigoErro;
import loja.checkout.dominio.PedidoRecusadoException;
import loja.checkout.entrega.EntregaEconomica;
import loja.checkout.entrega.EntregaExpressa;
import loja.checkout.entrega.Motoboy;
import loja.checkout.entrega.RetiradaLoja;
import loja.checkout.pagamento.Boleto;
import loja.checkout.pagamento.Cartao;
import loja.checkout.pagamento.Pix;
import loja.checkout.web.ResumoRequest;
import loja.checkout.web.ResumoRequest.ItemRequest;
import loja.checkout.web.ResumoResponse;
import org.junit.jupiter.api.Test;

class ResumoServiceTest {

    private final ResumoService service = new ResumoService(
            List.of(new EntregaEconomica(), new EntregaExpressa(), new RetiradaLoja(), new Motoboy()),
            List.of(new Bemvindo10(), new Menos50(), new FreteGratis(), new Leve3Pague2()),
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
        assertEquals(new ResumoResponse(new BigDecimal(subtotal), new BigDecimal(cupom), new BigDecimal(frete),
                prazo, new BigDecimal(seguro), new BigDecimal(ajuste), new BigDecimal(total), parcelas,
                new BigDecimal(parcela), new BigDecimal(credito), brinde), r);
    }

    @Test
    void exemplo1() {
        confere(service.resumir(new ResumoRequest(CAMISETA_TENIS, "EXPRESSA", "BEMVINDO10", "PIX", null,
                "BRONZE", "NORTE")),
                "409.70", "40.97", "33.10", 2, "10.24", "-20.60", "391.47", 1, "391.47", "0.00", false);
    }

    @Test
    void exemplo2() {
        confere(service.resumir(new ResumoRequest(CAMISETA_TENIS, "ECONOMICA", null, "CARTAO", 6,
                "PRATA", "CENTRO_OESTE")),
                "409.70", "0.00", "15.60", 7, "6.15", "30.55", "462.00", 6, "77.00", "8.19", false);
    }

    @Test
    void exemplo3() {
        confere(service.resumir(new ResumoRequest(List.of(item("Fone", "199.90", 2, "0.25")), "MOTOBOY",
                "MENOS50", "BOLETO", 1, "BRONZE", "NORDESTE")),
                "399.80", "50.00", "18.00", 0, "8.00", "3.49", "379.29", 1, "379.29", "0.00", false);
    }

    @Test
    void exemplo4() {
        confere(service.resumir(new ResumoRequest(
                List.of(item("Meia", "19.90", 7, "0.10"), item("Camiseta", "79.90", 2, "0.30")),
                "RETIRADA_LOJA", "LEVE3PAGUE2", "CARTAO", 3, "PRATA", "SUL")),
                "299.10", "39.80", "0.00", 1, "2.99", "0.00", "262.29", 3, "87.43", "5.98", false);
    }

    @Test
    void exemplo5() {
        confere(service.resumir(new ResumoRequest(CAMISETA_TENIS, "EXPRESSA", null, "PIX", null,
                "OURO", "SUDESTE")),
                "409.70", "0.00", "0.00", 2, "4.10", "-20.69", "393.11", 1, "393.11", "20.48", false);
    }

    @Test
    void freteGratisIgualaODescontoAoFrete() {
        ResumoResponse r = service.resumir(new ResumoRequest(CAMISETA_TENIS, "EXPRESSA", "FRETEGRATIS", "PIX",
                null, "BRONZE", "SUDESTE"));
        assertEquals(r.frete(), r.descontoCupom());
    }

    @Test
    void ouroAcimaDe500GanhaBrinde() {
        ResumoResponse r = service.resumir(new ResumoRequest(List.of(item("Casaco", "500.01", 1, "1")),
                "RETIRADA_LOJA", null, "PIX", null, "OURO", "SUL"));
        assertEquals(true, r.brinde());
    }

    private CodigoErro erro(ResumoRequest r) {
        return assertThrows(PedidoRecusadoException.class, () -> service.resumir(r)).codigo();
    }

    @Test
    void erros() {
        List<ItemRequest> it = CAMISETA_TENIS;
        assertEquals(CodigoErro.PEDIDO_INVALIDO,
                erro(new ResumoRequest(List.of(), "EXPRESSA", null, "PIX", null, "OURO", "SUL")));
        assertEquals(CodigoErro.NIVEL_CLUBE_INVALIDO,
                erro(new ResumoRequest(it, "EXPRESSA", null, "PIX", null, "DIAMANTE", "SUL")));
        assertEquals(CodigoErro.REGIAO_INVALIDA,
                erro(new ResumoRequest(it, "EXPRESSA", null, "PIX", null, "OURO", null)));
        assertEquals(CodigoErro.MODALIDADE_INVALIDA,
                erro(new ResumoRequest(it, "DRONE", null, "PIX", null, "OURO", "SUL")));
        assertEquals(CodigoErro.MODALIDADE_INDISPONIVEL,
                erro(new ResumoRequest(List.of(item("Sofa", "10", 1, "6")), "MOTOBOY", null, "PIX", null,
                        "OURO", "SUL")));
        assertEquals(CodigoErro.CUPOM_INVALIDO,
                erro(new ResumoRequest(it, "EXPRESSA", "bemvindo10", "PIX", null, "OURO", "SUL")));
        assertEquals(CodigoErro.CUPOM_NAO_APLICAVEL,
                erro(new ResumoRequest(List.of(item("Meia", "10", 1, "0.1")), "EXPRESSA", "MENOS50", "PIX",
                        null, "OURO", "SUL")));
        assertEquals(CodigoErro.FORMA_PAGAMENTO_INVALIDA,
                erro(new ResumoRequest(it, "EXPRESSA", null, "CHEQUE", null, "OURO", "SUL")));
        assertEquals(CodigoErro.PARCELAMENTO_INVALIDO,
                erro(new ResumoRequest(it, "EXPRESSA", null, "PIX", 2, "OURO", "SUL")));
        assertEquals(CodigoErro.PARCELAMENTO_INVALIDO,
                erro(new ResumoRequest(it, "EXPRESSA", null, "CARTAO", 13, "OURO", "SUL")));
        assertEquals(CodigoErro.FORMA_PAGAMENTO_INDISPONIVEL,
                erro(new ResumoRequest(List.of(item("Joia", "1500", 1, "0.1")), "EXPRESSA", null, "BOLETO",
                        null, "OURO", "SUL")));
    }
}

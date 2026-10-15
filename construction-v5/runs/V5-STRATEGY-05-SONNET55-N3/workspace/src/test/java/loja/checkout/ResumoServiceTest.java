package loja.checkout;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.math.BigDecimal;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
class ResumoServiceTest {
    @Autowired
    ResumoService service;

    static BigDecimal d(String v) {
        return new BigDecimal(v);
    }

    static Item camiseta() {
        return new Item("Camiseta", d("79.90"), 2, d("0.30"));
    }

    static Item tenis() {
        return new Item("Tênis", d("249.90"), 1, d("1.20"));
    }

    void confere(ResumoResponse r, String sub, String cupom, String frete, int prazo, String seguro, String ajuste,
            String total, int parcelas, String parcela, String credito, boolean brinde) {
        assertEquals(new ResumoResponse(d(sub), d(cupom), d(frete), prazo, d(seguro), d(ajuste), d(total), parcelas,
                d(parcela), d(credito), brinde), r);
    }

    @Test
    void exemplo1() {
        confere(service.calcular(new ResumoRequest(List.of(camiseta(), tenis()), "EXPRESSA", "BEMVINDO10", "PIX", null,
                "BRONZE", "NORTE")),
                "409.70", "40.97", "33.10", 2, "10.24", "-20.60", "391.47", 1, "391.47", "0.00", false);
    }

    @Test
    void exemplo2() {
        confere(service.calcular(new ResumoRequest(List.of(camiseta(), tenis()), "ECONOMICA", null, "CARTAO", 6,
                "PRATA", "CENTRO_OESTE")),
                "409.70", "0.00", "15.60", 7, "6.15", "30.55", "462.00", 6, "77.00", "8.19", false);
    }

    @Test
    void exemplo3() {
        confere(service.calcular(new ResumoRequest(List.of(new Item("Fone", d("199.90"), 2, d("0.25"))), "MOTOBOY",
                "MENOS50", "BOLETO", 1, "BRONZE", "NORDESTE")),
                "399.80", "50.00", "18.00", 0, "8.00", "3.49", "379.29", 1, "379.29", "0.00", false);
    }

    @Test
    void exemplo4() {
        confere(service.calcular(new ResumoRequest(
                List.of(new Item("Meia", d("19.90"), 7, d("0.10")), camiseta()), "RETIRADA_LOJA", "LEVE3PAGUE2",
                "CARTAO", 3, "PRATA", "SUL")),
                "299.10", "39.80", "0.00", 1, "2.99", "0.00", "262.29", 3, "87.43", "5.98", false);
    }

    @Test
    void exemplo5() {
        confere(service.calcular(new ResumoRequest(List.of(camiseta(), tenis()), "EXPRESSA", null, "PIX", null,
                "OURO", "SUDESTE")),
                "409.70", "0.00", "0.00", 2, "4.10", "-20.69", "393.11", 1, "393.11", "20.48", false);
    }

    @Test
    void freteGratisDescontaOFrete() {
        var r = service.calcular(new ResumoRequest(List.of(camiseta(), tenis()), "EXPRESSA", "FRETEGRATIS", "BOLETO",
                1, "BRONZE", "SUL"));
        assertEquals(d("33.10"), r.descontoCupom());
        assertEquals(d("33.10"), r.frete());
    }

    @Test
    void erros() {
        var ok = new ResumoRequest(List.of(camiseta()), "MOTOBOY", null, "PIX", null, "BRONZE", "SUL");
        assertEquals("PEDIDO_INVALIDO", codigo(new ResumoRequest(List.of(), "MOTOBOY", null, "PIX", null, "BRONZE", "SUL")));
        assertEquals("NIVEL_CLUBE_INVALIDO", codigo(new ResumoRequest(ok.itens(), "MOTOBOY", null, "PIX", null, "X", "SUL")));
        assertEquals("REGIAO_INVALIDA", codigo(new ResumoRequest(ok.itens(), "MOTOBOY", null, "PIX", null, "BRONZE", null)));
        assertEquals("MODALIDADE_INVALIDA", codigo(new ResumoRequest(ok.itens(), "X", null, "PIX", null, "BRONZE", "SUL")));
        assertEquals("MODALIDADE_INDISPONIVEL", codigo(new ResumoRequest(
                List.of(new Item("Sofá", d("10"), 1, d("6"))), "MOTOBOY", null, "PIX", null, "BRONZE", "SUL")));
        assertEquals("CUPOM_INVALIDO", codigo(new ResumoRequest(ok.itens(), "MOTOBOY", "bemvindo10", "PIX", null, "BRONZE", "SUL")));
        assertEquals("CUPOM_NAO_APLICAVEL", codigo(new ResumoRequest(ok.itens(), "MOTOBOY", "MENOS50", "PIX", null, "BRONZE", "SUL")));
        assertEquals("FORMA_PAGAMENTO_INVALIDA", codigo(new ResumoRequest(ok.itens(), "MOTOBOY", null, null, null, "BRONZE", "SUL")));
        assertEquals("PARCELAMENTO_INVALIDO", codigo(new ResumoRequest(ok.itens(), "MOTOBOY", null, "PIX", 2, "BRONZE", "SUL")));
        assertEquals("FORMA_PAGAMENTO_INDISPONIVEL", codigo(new ResumoRequest(
                List.of(new Item("TV", d("1500"), 1, d("2"))), "MOTOBOY", null, "BOLETO", null, "BRONZE", "SUL")));
    }

    @Test
    void limitesEBrinde() {
        assertEquals(false, service.calcular(new ResumoRequest(List.of(new Item("A", d("500"), 1, d("5"))), "MOTOBOY",
                null, "PIX", null, "OURO", "SUL")).brinde());
        assertEquals(true, service.calcular(new ResumoRequest(List.of(new Item("A", d("500.01"), 1, d("1"))),
                "MOTOBOY", null, "PIX", null, "OURO", "SUL")).brinde());
        assertEquals("FORMA_PAGAMENTO_INDISPONIVEL", codigo(new ResumoRequest(
                List.of(new Item("A", d("990.20"), 1, d("1"))), "RETIRADA_LOJA", null, "BOLETO", 1, "BRONZE", "SUL")));
    }

    @Test
    void cartaoComJurosUsaPrice() {
        var r = service.calcular(new ResumoRequest(List.of(camiseta(), tenis()), "ECONOMICA", null, "CARTAO", 12,
                "BRONZE", "SUL"));
        assertEquals(r.valorParcela().multiply(BigDecimal.valueOf(12)), r.totalFinal());
    }

    String codigo(ResumoRequest r) {
        return assertThrows(ErroPedido.class, () -> service.calcular(r)).codigo();
    }
}

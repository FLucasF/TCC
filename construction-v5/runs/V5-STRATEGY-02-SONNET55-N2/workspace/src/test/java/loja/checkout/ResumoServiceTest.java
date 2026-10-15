package loja.checkout;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.math.BigDecimal;
import java.util.List;
import loja.checkout.web.ItemRequest;
import loja.checkout.web.PedidoRequest;
import loja.checkout.web.ResumoResponse;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
class ResumoServiceTest {
    @Autowired
    ResumoService service;

    static final List<ItemRequest> ROUPAS = List.of(
            new ItemRequest("Camiseta", new BigDecimal("79.90"), 2, new BigDecimal("0.30")),
            new ItemRequest("Tenis", new BigDecimal("249.90"), 1, new BigDecimal("1.20")));

    static BigDecimal d(String v) {
        return new BigDecimal(v);
    }

    @Test
    void exemplo1() {
        ResumoResponse r = service.calcular(new PedidoRequest(ROUPAS, "EXPRESSA", "BEMVINDO10", "PIX", null, "BRONZE", "NORTE"));
        assertEquals(new ResumoResponse(d("409.70"), d("40.97"), d("33.10"), 2, d("10.24"), d("-20.60"),
                d("391.47"), 1, d("391.47"), d("0.00"), false), r);
    }

    @Test
    void exemplo2() {
        ResumoResponse r = service.calcular(new PedidoRequest(ROUPAS, "ECONOMICA", null, "CARTAO", 6, "PRATA", "CENTRO_OESTE"));
        assertEquals(new ResumoResponse(d("409.70"), d("0.00"), d("15.60"), 7, d("6.15"), d("30.55"),
                d("462.00"), 6, d("77.00"), d("8.19"), false), r);
    }

    @Test
    void exemplo3() {
        List<ItemRequest> itens = List.of(new ItemRequest("Fone", d("199.90"), 2, d("0.25")));
        ResumoResponse r = service.calcular(new PedidoRequest(itens, "MOTOBOY", "MENOS50", "BOLETO", 1, "BRONZE", "NORDESTE"));
        assertEquals(new ResumoResponse(d("399.80"), d("50.00"), d("18.00"), 0, d("8.00"), d("3.49"),
                d("379.29"), 1, d("379.29"), d("0.00"), false), r);
    }

    @Test
    void exemplo4() {
        List<ItemRequest> itens = List.of(
                new ItemRequest("Meia", d("19.90"), 7, d("0.10")),
                new ItemRequest("Camiseta", d("79.90"), 2, d("0.30")));
        ResumoResponse r = service.calcular(new PedidoRequest(itens, "RETIRADA_LOJA", "LEVE3PAGUE2", "CARTAO", 3, "PRATA", "SUL"));
        assertEquals(new ResumoResponse(d("299.10"), d("39.80"), d("0.00"), 1, d("2.99"), d("0.00"),
                d("262.29"), 3, d("87.43"), d("5.98"), false), r);
    }

    @Test
    void exemplo5() {
        ResumoResponse r = service.calcular(new PedidoRequest(ROUPAS, "EXPRESSA", null, "PIX", 1, "OURO", "SUDESTE"));
        assertEquals(new ResumoResponse(d("409.70"), d("0.00"), d("0.00"), 2, d("4.10"), d("-20.69"),
                d("393.11"), 1, d("393.11"), d("20.48"), false), r);
    }

    @Test
    void freteGratisIgualaDescontoAoFrete() {
        ResumoResponse r = service.calcular(new PedidoRequest(ROUPAS, "EXPRESSA", "FRETEGRATIS", "PIX", 1, "BRONZE", "SUL"));
        assertEquals(r.frete(), r.descontoCupom());
    }

    @Test
    void erros() {
        assertEquals("PEDIDO_INVALIDO", erro(new PedidoRequest(List.of(), "EXPRESSA", null, "PIX", 1, "OURO", "SUL")));
        assertEquals("NIVEL_CLUBE_INVALIDO", erro(new PedidoRequest(ROUPAS, "EXPRESSA", null, "PIX", 1, null, "SUL")));
        assertEquals("REGIAO_INVALIDA", erro(new PedidoRequest(ROUPAS, "EXPRESSA", null, "PIX", 1, "OURO", "MARTE")));
        assertEquals("MODALIDADE_INVALIDA", erro(new PedidoRequest(ROUPAS, "DRONE", null, "PIX", 1, "OURO", "SUL")));
        List<ItemRequest> pesado = List.of(new ItemRequest("Bau", d("100.00"), 1, d("6")));
        assertEquals("MODALIDADE_INDISPONIVEL", erro(new PedidoRequest(pesado, "MOTOBOY", null, "PIX", 1, "OURO", "SUL")));
        assertEquals("CUPOM_INVALIDO", erro(new PedidoRequest(ROUPAS, "EXPRESSA", "XXX", "PIX", 1, "OURO", "SUL")));
        List<ItemRequest> barato = List.of(new ItemRequest("Meia", d("10.00"), 1, d("0.1")));
        assertEquals("CUPOM_NAO_APLICAVEL", erro(new PedidoRequest(barato, "EXPRESSA", "MENOS50", "PIX", 1, "OURO", "SUL")));
        assertEquals("FORMA_PAGAMENTO_INVALIDA", erro(new PedidoRequest(ROUPAS, "EXPRESSA", null, "CHEQUE", 1, "OURO", "SUL")));
        assertEquals("PARCELAMENTO_INVALIDO", erro(new PedidoRequest(ROUPAS, "EXPRESSA", null, "PIX", 2, "OURO", "SUL")));
        assertEquals("PARCELAMENTO_INVALIDO", erro(new PedidoRequest(ROUPAS, "EXPRESSA", null, "CARTAO", 13, "OURO", "SUL")));
        List<ItemRequest> caro = List.of(new ItemRequest("Joia", d("2000.00"), 1, d("0.1")));
        assertEquals("FORMA_PAGAMENTO_INDISPONIVEL", erro(new PedidoRequest(caro, "EXPRESSA", null, "BOLETO", 1, "OURO", "SUL")));
    }

    private String erro(PedidoRequest p) {
        return assertThrows(ErroPedido.class, () -> service.calcular(p)).codigo();
    }
}

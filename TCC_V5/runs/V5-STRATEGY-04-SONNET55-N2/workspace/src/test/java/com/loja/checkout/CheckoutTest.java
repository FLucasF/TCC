package com.loja.checkout;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.loja.checkout.web.ItemRequest;
import com.loja.checkout.web.ResumoRequest;
import com.loja.checkout.web.ResumoResponse;
import java.math.BigDecimal;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
class CheckoutTest {
    @Autowired
    CheckoutService servico;

    static final ItemRequest CAMISETA = new ItemRequest("Camiseta", new BigDecimal("79.90"), 2, new BigDecimal("0.30"));
    static final ItemRequest TENIS = new ItemRequest("Tenis", new BigDecimal("249.90"), 1, new BigDecimal("1.20"));

    static ResumoRequest req(List<ItemRequest> itens, String entrega, String cupom, String pag, Integer parcelas,
            String nivel, String regiao) {
        return new ResumoRequest(itens, entrega, cupom, pag, parcelas, nivel, regiao);
    }

    void confere(ResumoResponse r, String sub, String cupom, String frete, int prazo, String seguro, String ajuste,
            String total, int parcelas, String parcela, String credito, boolean brinde) {
        assertEquals(new BigDecimal(sub), r.subtotalProdutos());
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

    @Test
    void exemplo1() {
        confere(servico.resumir(req(List.of(CAMISETA, TENIS), "EXPRESSA", "BEMVINDO10", "PIX", 1, "BRONZE", "NORTE")),
                "409.70", "40.97", "33.10", 2, "10.24", "-20.60", "391.47", 1, "391.47", "0.00", false);
    }

    @Test
    void exemplo2() {
        confere(servico.resumir(req(List.of(CAMISETA, TENIS), "ECONOMICA", null, "CARTAO", 6, "PRATA", "CENTRO_OESTE")),
                "409.70", "0.00", "15.60", 7, "6.15", "30.55", "462.00", 6, "77.00", "8.19", false);
    }

    @Test
    void exemplo3() {
        ItemRequest fone = new ItemRequest("Fone", new BigDecimal("199.90"), 2, new BigDecimal("0.25"));
        confere(servico.resumir(req(List.of(fone), "MOTOBOY", "MENOS50", "BOLETO", null, "BRONZE", "NORDESTE")),
                "399.80", "50.00", "18.00", 0, "8.00", "3.49", "379.29", 1, "379.29", "0.00", false);
    }

    @Test
    void exemplo4() {
        ItemRequest meia = new ItemRequest("Meia", new BigDecimal("19.90"), 7, new BigDecimal("0.10"));
        confere(servico.resumir(req(List.of(meia, CAMISETA), "RETIRADA_LOJA", "LEVE3PAGUE2", "CARTAO", 3, "PRATA", "SUL")),
                "299.10", "39.80", "0.00", 1, "2.99", "0.00", "262.29", 3, "87.43", "5.98", false);
    }

    @Test
    void exemplo5() {
        confere(servico.resumir(req(List.of(CAMISETA, TENIS), "EXPRESSA", null, "PIX", 1, "OURO", "SUDESTE")),
                "409.70", "0.00", "0.00", 2, "4.10", "-20.69", "393.11", 1, "393.11", "20.48", false);
    }

    void erro(Erro esperado, ResumoRequest r) {
        assertEquals(esperado, assertThrows(ErroNegocioException.class, () -> servico.resumir(r)).erro());
    }

    @Test
    void erros() {
        List<ItemRequest> itens = List.of(CAMISETA);
        erro(Erro.PEDIDO_INVALIDO, req(List.of(), "EXPRESSA", null, "PIX", 1, "OURO", "SUL"));
        erro(Erro.NIVEL_CLUBE_INVALIDO, req(itens, "EXPRESSA", null, "PIX", 1, "X", "SUL"));
        erro(Erro.REGIAO_INVALIDA, req(itens, "EXPRESSA", null, "PIX", 1, "OURO", null));
        erro(Erro.MODALIDADE_INVALIDA, req(itens, "X", null, "PIX", 1, "OURO", "SUL"));
        ItemRequest pesado = new ItemRequest("Piano", new BigDecimal("2000"), 1, new BigDecimal("6"));
        erro(Erro.MODALIDADE_INDISPONIVEL, req(List.of(pesado), "MOTOBOY", null, "PIX", 1, "OURO", "SUL"));
        erro(Erro.CUPOM_INVALIDO, req(itens, "EXPRESSA", "bemvindo10", "PIX", 1, "OURO", "SUL"));
        erro(Erro.CUPOM_NAO_APLICAVEL, req(itens, "EXPRESSA", "MENOS50", "PIX", 1, "OURO", "SUL"));
        erro(Erro.FORMA_PAGAMENTO_INVALIDA, req(itens, "EXPRESSA", null, "X", 1, "OURO", "SUL"));
        erro(Erro.PARCELAMENTO_INVALIDO, req(itens, "EXPRESSA", null, "PIX", 2, "OURO", "SUL"));
        erro(Erro.PARCELAMENTO_INVALIDO, req(itens, "EXPRESSA", null, "CARTAO", 13, "OURO", "SUL"));
        erro(Erro.FORMA_PAGAMENTO_INDISPONIVEL, req(List.of(pesado), "EXPRESSA", null, "BOLETO", 1, "OURO", "SUL"));
    }
}

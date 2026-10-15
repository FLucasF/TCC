package com.loja.checkout;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.loja.checkout.ResumoRequest.ItemRequest;
import java.math.BigDecimal;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
class ResumoServiceTest {
    @Autowired
    ResumoService service;

    static ItemRequest item(String nome, String preco, int qtd, String peso) {
        return new ItemRequest(nome, new BigDecimal(preco), qtd, new BigDecimal(peso));
    }

    static final List<ItemRequest> CAMISETA_TENIS = List.of(item("Camiseta", "79.90", 2, "0.30"),
            item("Tenis", "249.90", 1, "1.20"));

    static ResumoRequest req(List<ItemRequest> itens, String ent, String cupom, String pag, Integer parc,
                             String nivel, String regiao) {
        return new ResumoRequest(itens, ent, cupom, pag, parc, nivel, regiao);
    }

    static void confere(ResumoResponse r, String sub, String cupom, String frete, int prazo, String seguro,
                        String ajuste, String total, int parcelas, String parcela, String credito, boolean brinde) {
        assertThat(r).isEqualTo(new ResumoResponse(new BigDecimal(sub), new BigDecimal(cupom), new BigDecimal(frete),
                prazo, new BigDecimal(seguro), new BigDecimal(ajuste), new BigDecimal(total), parcelas,
                new BigDecimal(parcela), new BigDecimal(credito), brinde));
    }

    @Test
    void exemplo1() {
        confere(service.calcular(req(CAMISETA_TENIS, "EXPRESSA", "BEMVINDO10", "PIX", null, "BRONZE", "NORTE")),
                "409.70", "40.97", "33.10", 2, "10.24", "-20.60", "391.47", 1, "391.47", "0.00", false);
    }

    @Test
    void exemplo2() {
        confere(service.calcular(req(CAMISETA_TENIS, "ECONOMICA", null, "CARTAO", 6, "PRATA", "CENTRO_OESTE")),
                "409.70", "0.00", "15.60", 7, "6.15", "30.55", "462.00", 6, "77.00", "8.19", false);
    }

    @Test
    void exemplo3() {
        confere(service.calcular(req(List.of(item("Fone", "199.90", 2, "0.25")), "MOTOBOY", "MENOS50", "BOLETO",
                        null, "BRONZE", "NORDESTE")),
                "399.80", "50.00", "18.00", 0, "8.00", "3.49", "379.29", 1, "379.29", "0.00", false);
    }

    @Test
    void exemplo4() {
        confere(service.calcular(req(List.of(item("Meia", "19.90", 7, "0.10"), item("Camiseta", "79.90", 2, "0.30")),
                        "RETIRADA_LOJA", "LEVE3PAGUE2", "CARTAO", 3, "PRATA", "SUL")),
                "299.10", "39.80", "0.00", 1, "2.99", "0.00", "262.29", 3, "87.43", "5.98", false);
    }

    @Test
    void exemplo5() {
        confere(service.calcular(req(CAMISETA_TENIS, "EXPRESSA", null, "PIX", 1, "OURO", "SUDESTE")),
                "409.70", "0.00", "0.00", 2, "4.10", "-20.69", "393.11", 1, "393.11", "20.48", false);
    }

    @Test
    void freteGratisDescontaOFrete() {
        var r = service.calcular(req(CAMISETA_TENIS, "EXPRESSA", "FRETEGRATIS", "PIX", 1, "BRONZE", "SUL"));
        assertThat(r.frete()).isEqualByComparingTo("33.10");
        assertThat(r.descontoCupom()).isEqualByComparingTo("33.10");
    }

    @Test
    void ouroAcimaDe500GanhaBrinde() {
        var r = service.calcular(req(List.of(item("Casaco", "500.01", 1, "1")), "ECONOMICA", null, "PIX", 1,
                "OURO", "SUL"));
        assertThat(r.brinde()).isTrue();
    }

    private void erro(ResumoRequest r, String codigo) {
        assertThatThrownBy(() -> service.calcular(r)).isInstanceOfSatisfying(ErroNegocio.class,
                e -> assertThat(e.codigo()).isEqualTo(codigo));
    }

    @Test
    void erros() {
        erro(req(List.of(), "EXPRESSA", null, "PIX", 1, "ZZ", "ZZ"), "PEDIDO_INVALIDO");
        erro(req(CAMISETA_TENIS, "EXPRESSA", null, "PIX", 1, "ZZ", "ZZ"), "NIVEL_CLUBE_INVALIDO");
        erro(req(CAMISETA_TENIS, "EXPRESSA", null, "PIX", 1, "OURO", "ZZ"), "REGIAO_INVALIDA");
        erro(req(CAMISETA_TENIS, "X", null, "PIX", 1, "OURO", "SUL"), "MODALIDADE_INVALIDA");
        erro(req(List.of(item("Pesado", "10", 1, "6")), "MOTOBOY", null, "PIX", 1, "OURO", "SUL"),
                "MODALIDADE_INDISPONIVEL");
        erro(req(CAMISETA_TENIS, "EXPRESSA", "XX", "PIX", 1, "OURO", "SUL"), "CUPOM_INVALIDO");
        erro(req(List.of(item("Meia", "10", 1, "1")), "EXPRESSA", "MENOS50", "PIX", 1, "OURO", "SUL"),
                "CUPOM_NAO_APLICAVEL");
        erro(req(CAMISETA_TENIS, "EXPRESSA", null, "XX", 1, "OURO", "SUL"), "FORMA_PAGAMENTO_INVALIDA");
        erro(req(CAMISETA_TENIS, "EXPRESSA", null, "PIX", 2, "OURO", "SUL"), "PARCELAMENTO_INVALIDO");
        erro(req(CAMISETA_TENIS, "EXPRESSA", null, "CARTAO", 13, "OURO", "SUL"), "PARCELAMENTO_INVALIDO");
        erro(req(List.of(item("Joia", "1000", 1, "1")), "EXPRESSA", null, "BOLETO", 1, "OURO", "SUL"),
                "FORMA_PAGAMENTO_INDISPONIVEL");
    }
}

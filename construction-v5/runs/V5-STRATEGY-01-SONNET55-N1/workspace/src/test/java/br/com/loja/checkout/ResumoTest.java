package br.com.loja.checkout;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class ResumoTest {

    private static final String CAMISETA_TENIS = """
            [{"nome":"Camiseta","precoUnitario":79.90,"quantidade":2,"pesoKg":0.30},
             {"nome":"Tênis","precoUnitario":249.90,"quantidade":1,"pesoKg":1.20}]""";

    @Autowired
    MockMvc mvc;

    private ResultActions enviar(String json) throws Exception {
        return mvc.perform(post("/checkout/resumo").contentType(MediaType.APPLICATION_JSON).content(json));
    }

    private ResultActions enviar(String itens, String resto) throws Exception {
        return enviar("{\"itens\":" + itens + "," + resto + "}");
    }

    private void confere(ResultActions r, String sub, String cupom, String frete, int prazo, String seguro,
                         String ajuste, String total, int parcelas, String parcela, String credito,
                         boolean brinde) throws Exception {
        r.andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("\"subtotalProdutos\":" + sub)))
                .andExpect(jsonPath("$.descontoCupom").value(Double.parseDouble(cupom)))
                .andExpect(jsonPath("$.frete").value(Double.parseDouble(frete)))
                .andExpect(jsonPath("$.prazoEntregaDias").value(prazo))
                .andExpect(jsonPath("$.seguro").value(Double.parseDouble(seguro)))
                .andExpect(jsonPath("$.ajustePagamento").value(Double.parseDouble(ajuste)))
                .andExpect(jsonPath("$.totalFinal").value(Double.parseDouble(total)))
                .andExpect(jsonPath("$.parcelas").value(parcelas))
                .andExpect(jsonPath("$.valorParcela").value(Double.parseDouble(parcela)))
                .andExpect(jsonPath("$.creditoProximaCompra").value(Double.parseDouble(credito)))
                .andExpect(jsonPath("$.brinde").value(brinde));
    }

    private void erro(ResultActions r, String codigo) throws Exception {
        r.andExpect(content().json("{\"erro\":\"" + codigo + "\"}", true));
    }

    @Test
    void exemplo1() throws Exception {
        confere(enviar(CAMISETA_TENIS, """
                "modalidadeEntrega":"EXPRESSA","cupom":"BEMVINDO10","formaPagamento":"PIX",
                "nivelClube":"BRONZE","regiao":"NORTE\""""),
                "409.70", "40.97", "33.10", 2, "10.24", "-20.60", "391.47", 1, "391.47", "0.00", false);
    }

    @Test
    void exemplo2() throws Exception {
        confere(enviar(CAMISETA_TENIS, """
                "modalidadeEntrega":"ECONOMICA","formaPagamento":"CARTAO","parcelas":6,
                "nivelClube":"PRATA","regiao":"CENTRO_OESTE\""""),
                "409.70", "0.00", "15.60", 7, "6.15", "30.55", "462.00", 6, "77.00", "8.19", false);
    }

    @Test
    void exemplo3() throws Exception {
        confere(enviar("""
                [{"nome":"Fone","precoUnitario":199.90,"quantidade":2,"pesoKg":0.25}]""", """
                "modalidadeEntrega":"MOTOBOY","cupom":"MENOS50","formaPagamento":"BOLETO",
                "nivelClube":"BRONZE","regiao":"NORDESTE\""""),
                "399.80", "50.00", "18.00", 0, "8.00", "3.49", "379.29", 1, "379.29", "0.00", false);
    }

    @Test
    void exemplo4() throws Exception {
        confere(enviar("""
                [{"nome":"Meia","precoUnitario":19.90,"quantidade":7,"pesoKg":0.10},
                 {"nome":"Camiseta","precoUnitario":79.90,"quantidade":2,"pesoKg":0.30}]""", """
                "modalidadeEntrega":"RETIRADA_LOJA","cupom":"LEVE3PAGUE2","formaPagamento":"CARTAO","parcelas":3,
                "nivelClube":"PRATA","regiao":"SUL\""""),
                "299.10", "39.80", "0.00", 1, "2.99", "0.00", "262.29", 3, "87.43", "5.98", false);
    }

    @Test
    void exemplo5() throws Exception {
        confere(enviar(CAMISETA_TENIS, """
                "modalidadeEntrega":"EXPRESSA","formaPagamento":"PIX","nivelClube":"OURO","regiao":"SUDESTE\""""),
                "409.70", "0.00", "0.00", 2, "4.10", "-20.69", "393.11", 1, "393.11", "20.48", false);
    }

    @Test
    void freteGratisIgualaOFrete() throws Exception {
        enviar(CAMISETA_TENIS, """
                "modalidadeEntrega":"ECONOMICA","cupom":"FRETEGRATIS","formaPagamento":"BOLETO",
                "nivelClube":"BRONZE","regiao":"SUL\"""")
                .andExpect(jsonPath("$.frete").value(15.6))
                .andExpect(jsonPath("$.descontoCupom").value(15.6));
    }

    @Test
    void erros() throws Exception {
        String base = """
                "modalidadeEntrega":"ECONOMICA","formaPagamento":"PIX","nivelClube":"BRONZE","regiao":"SUL\"""";
        erro(enviar("[]", base), "PEDIDO_INVALIDO");
        erro(enviar("[{\"nome\":\"x\",\"precoUnitario\":1,\"quantidade\":1,\"pesoKg\":0}]", base), "PEDIDO_INVALIDO");
        erro(enviar(CAMISETA_TENIS, base.replace("BRONZE", "DIAMANTE")), "NIVEL_CLUBE_INVALIDO");
        erro(enviar(CAMISETA_TENIS, base.replace("SUL", "MARTE")), "REGIAO_INVALIDA");
        erro(enviar(CAMISETA_TENIS, base.replace("ECONOMICA", "DRONE")), "MODALIDADE_INVALIDA");
        erro(enviar("[{\"nome\":\"x\",\"precoUnitario\":10,\"quantidade\":1,\"pesoKg\":5.01}]",
                base.replace("ECONOMICA", "MOTOBOY")), "MODALIDADE_INDISPONIVEL");
        erro(enviar(CAMISETA_TENIS, base + ",\"cupom\":\"XYZ\""), "CUPOM_INVALIDO");
        erro(enviar("[{\"nome\":\"x\",\"precoUnitario\":10,\"quantidade\":1,\"pesoKg\":1}]", base + ",\"cupom\":\"MENOS50\""),
                "CUPOM_NAO_APLICAVEL");
        erro(enviar(CAMISETA_TENIS, base.replace("PIX", "DINHEIRO")), "FORMA_PAGAMENTO_INVALIDA");
        erro(enviar(CAMISETA_TENIS, base + ",\"parcelas\":2"), "PARCELAMENTO_INVALIDO");
        erro(enviar("[{\"nome\":\"x\",\"precoUnitario\":2000,\"quantidade\":1,\"pesoKg\":1}]",
                base.replace("PIX", "BOLETO")), "FORMA_PAGAMENTO_INDISPONIVEL");
    }
}

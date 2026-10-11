package br.com.loja.checkout;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class ResumoCompraTest {

    private static final String CAMISETA_E_TENIS = """
            [{"nome":"Camiseta","precoUnitario":79.90,"quantidade":2,"pesoKg":0.30},
             {"nome":"Tênis","precoUnitario":249.90,"quantidade":1,"pesoKg":1.20}]""";

    @Autowired
    private MockMvc mvc;

    private ResultActions enviar(String json) throws Exception {
        return mvc.perform(post("/checkout/resumo").contentType(MediaType.APPLICATION_JSON).content(json));
    }

    private static String resumo(String subtotal, String cupom, String frete, int prazo, String seguro, String ajuste,
                                 String total, int parcelas, String parcela, String credito, boolean brinde) {
        return ("{\"subtotalProdutos\":%s,\"descontoCupom\":%s,\"frete\":%s,\"prazoEntregaDias\":%d,\"seguro\":%s,"
                + "\"ajustePagamento\":%s,\"totalFinal\":%s,\"parcelas\":%d,\"valorParcela\":%s,"
                + "\"creditoProximaCompra\":%s,\"brinde\":%b}")
                .formatted(subtotal, cupom, frete, prazo, seguro, ajuste, total, parcelas, parcela, credito, brinde);
    }

    @Test
    void exemploDoAnexo() throws Exception {
        enviar("""
                {"itens":%s,"modalidadeEntrega":"EXPRESSA","cupom":"BEMVINDO10","formaPagamento":"PIX",
                 "parcelas":1,"nivelClube":"OURO","regiao":"SUDESTE"}""".formatted(CAMISETA_E_TENIS))
                .andExpect(status().isOk())
                .andExpect(content().string(resumo("409.70", "40.97", "0.00", 2, "4.10", "-18.64",
                        "354.19", 1, "354.19", "20.48", false)));
    }

    @Test
    void exemplo1() throws Exception {
        enviar("""
                {"itens":%s,"modalidadeEntrega":"EXPRESSA","cupom":"BEMVINDO10","formaPagamento":"PIX",
                 "nivelClube":"BRONZE","regiao":"NORTE"}""".formatted(CAMISETA_E_TENIS))
                .andExpect(status().isOk())
                .andExpect(content().string(resumo("409.70", "40.97", "33.10", 2, "10.24", "-20.60",
                        "391.47", 1, "391.47", "0.00", false)));
    }

    @Test
    void exemplo2() throws Exception {
        enviar("""
                {"itens":%s,"modalidadeEntrega":"ECONOMICA","formaPagamento":"CARTAO","parcelas":6,
                 "nivelClube":"PRATA","regiao":"CENTRO_OESTE"}""".formatted(CAMISETA_E_TENIS))
                .andExpect(status().isOk())
                .andExpect(content().string(resumo("409.70", "0.00", "15.60", 7, "6.15", "30.55",
                        "462.00", 6, "77.00", "8.19", false)));
    }

    @Test
    void exemplo3() throws Exception {
        enviar("""
                {"itens":[{"nome":"Fone","precoUnitario":199.90,"quantidade":2,"pesoKg":0.25}],
                 "modalidadeEntrega":"MOTOBOY","cupom":"MENOS50","formaPagamento":"BOLETO",
                 "nivelClube":"BRONZE","regiao":"NORDESTE"}""")
                .andExpect(status().isOk())
                .andExpect(content().string(resumo("399.80", "50.00", "18.00", 0, "8.00", "3.49",
                        "379.29", 1, "379.29", "0.00", false)));
    }

    @Test
    void exemplo4() throws Exception {
        enviar("""
                {"itens":[{"nome":"Meia","precoUnitario":19.90,"quantidade":7,"pesoKg":0.10},
                          {"nome":"Camiseta","precoUnitario":79.90,"quantidade":2,"pesoKg":0.30}],
                 "modalidadeEntrega":"RETIRADA_LOJA","cupom":"LEVE3PAGUE2","formaPagamento":"CARTAO",
                 "parcelas":3,"nivelClube":"PRATA","regiao":"SUL"}""")
                .andExpect(status().isOk())
                .andExpect(content().string(resumo("299.10", "39.80", "0.00", 1, "2.99", "0.00",
                        "262.29", 3, "87.43", "5.98", false)));
    }

    @Test
    void exemplo5() throws Exception {
        enviar("""
                {"itens":%s,"modalidadeEntrega":"EXPRESSA","formaPagamento":"PIX",
                 "nivelClube":"OURO","regiao":"SUDESTE"}""".formatted(CAMISETA_E_TENIS))
                .andExpect(status().isOk())
                .andExpect(content().string(resumo("409.70", "0.00", "0.00", 2, "4.10", "-20.69",
                        "393.11", 1, "393.11", "20.48", false)));
    }

    @Test
    void freteGratisDescontaOFrete() throws Exception {
        enviar("""
                {"itens":%s,"modalidadeEntrega":"EXPRESSA","cupom":"FRETEGRATIS","formaPagamento":"CARTAO",
                 "nivelClube":"BRONZE","regiao":"SUL"}""".formatted(CAMISETA_E_TENIS))
                .andExpect(status().isOk())
                .andExpect(content().string(resumo("409.70", "33.10", "33.10", 2, "4.10", "0.00",
                        "413.80", 1, "413.80", "0.00", false)));
    }

    @Test
    void ouroAcimaDe500GanhaBrinde() throws Exception {
        enviar("""
                {"itens":[{"nome":"Jaqueta","precoUnitario":500.01,"quantidade":1,"pesoKg":1.00}],
                 "modalidadeEntrega":"RETIRADA_LOJA","formaPagamento":"CARTAO","parcelas":2,
                 "nivelClube":"OURO","regiao":"SUDESTE"}""")
                .andExpect(status().isOk())
                .andExpect(content().string(resumo("500.01", "0.00", "0.00", 1, "5.00", "0.00",
                        "505.01", 2, "252.50", "25.00", true)));
    }

    @ParameterizedTest
    @CsvSource(delimiter = '|', textBlock = """
            []                                                              | EXPRESSA      | PIX    | 1  | BRONZE   | SUL     |             | PEDIDO_INVALIDO
            [{"precoUnitario":10,"quantidade":0,"pesoKg":1}]                | EXPRESSA      | PIX    | 1  | BRONZE   | SUL     |             | PEDIDO_INVALIDO
            [{"precoUnitario":10,"quantidade":1}]                           | EXPRESSA      | PIX    | 1  | BRONZE   | SUL     |             | PEDIDO_INVALIDO
            [{"precoUnitario":-1,"quantidade":1,"pesoKg":1}]                | EXPRESSA      | PIX    | 1  | DIAMANTE | SUL     |             | PEDIDO_INVALIDO
            [{"precoUnitario":10,"quantidade":1,"pesoKg":1}]                | EXPRESSA      | PIX    | 1  | DIAMANTE | SUL     |             | NIVEL_CLUBE_INVALIDO
            [{"precoUnitario":10,"quantidade":1,"pesoKg":1}]                | EXPRESSA      | PIX    | 1  | BRONZE   | LESTE   |             | REGIAO_INVALIDA
            [{"precoUnitario":10,"quantidade":1,"pesoKg":1}]                | DRONE         | PIX    | 1  | BRONZE   | SUL     |             | MODALIDADE_INVALIDA
            [{"precoUnitario":10,"quantidade":6,"pesoKg":1}]                | MOTOBOY       | PIX    | 1  | BRONZE   | SUL     | XYZ         | MODALIDADE_INDISPONIVEL
            [{"precoUnitario":10,"quantidade":1,"pesoKg":1}]                | EXPRESSA      | PIX    | 1  | BRONZE   | SUL     | bemvindo10  | CUPOM_INVALIDO
            [{"precoUnitario":299.99,"quantidade":1,"pesoKg":1}]            | EXPRESSA      | PIX    | 1  | BRONZE   | SUL     | MENOS50     | CUPOM_NAO_APLICAVEL
            [{"precoUnitario":10,"quantidade":1,"pesoKg":1}]                | EXPRESSA      | CHEQUE | 1  | BRONZE   | SUL     |             | FORMA_PAGAMENTO_INVALIDA
            [{"precoUnitario":10,"quantidade":1,"pesoKg":1}]                | EXPRESSA      | PIX    | 2  | BRONZE   | SUL     |             | PARCELAMENTO_INVALIDO
            [{"precoUnitario":10,"quantidade":1,"pesoKg":1}]                | EXPRESSA      | CARTAO | 13 | BRONZE   | SUL     |             | PARCELAMENTO_INVALIDO
            [{"precoUnitario":2000,"quantidade":1,"pesoKg":1}]              | EXPRESSA      | BOLETO | 2  | BRONZE   | SUL     |             | PARCELAMENTO_INVALIDO
            [{"precoUnitario":1000,"quantidade":1,"pesoKg":1}]              | RETIRADA_LOJA | BOLETO | 1  | BRONZE   | SUL     |             | FORMA_PAGAMENTO_INDISPONIVEL
            """)
    void recusaNaOrdemCombinada(String itens, String modalidade, String forma, int parcelas, String nivel,
                                String regiao, String cupom, String erro) throws Exception {
        String campoCupom = cupom == null ? "" : ",\"cupom\":\"" + cupom + "\"";
        enviar("""
                {"itens":%s,"modalidadeEntrega":"%s","formaPagamento":"%s","parcelas":%d,
                 "nivelClube":"%s","regiao":"%s"%s}""".formatted(itens, modalidade, forma, parcelas, nivel, regiao, campoCupom))
                .andExpect(status().isUnprocessableContent())
                .andExpect(content().string("{\"erro\":\"" + erro + "\"}"));
    }

    @Test
    void camposAusentesSaoRecusados() throws Exception {
        enviar("""
                {"itens":[{"precoUnitario":10,"quantidade":1,"pesoKg":1}]}""")
                .andExpect(content().string("{\"erro\":\"NIVEL_CLUBE_INVALIDO\"}"));
    }

    @Test
    void boletoNoLimiteDeMilEhAceito() throws Exception {
        enviar("""
                {"itens":[{"precoUnitario":990.10,"quantidade":1,"pesoKg":1}],"modalidadeEntrega":"RETIRADA_LOJA",
                 "formaPagamento":"BOLETO","nivelClube":"BRONZE","regiao":"SUL"}""")
                .andExpect(status().isOk());
    }
}

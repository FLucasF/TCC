package br.com.loja.checkout;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

@SpringBootTest
@AutoConfigureMockMvc
class ResumoCheckoutTest {

    private static final String CAMISETA_E_TENIS = """
            [{"nome":"Camiseta","precoUnitario":79.90,"quantidade":2,"pesoKg":0.30},
             {"nome":"Tênis","precoUnitario":249.90,"quantidade":1,"pesoKg":1.20}]""";

    @Autowired
    private MockMvc mvc;

    private ResultActions enviar(String json) throws Exception {
        return mvc.perform(post("/checkout/resumo").contentType(MediaType.APPLICATION_JSON).content(json));
    }

    private static String pedido(String itens, String modalidade, String cupom, String pagamento,
            Integer parcelas, String nivel, String regiao) {
        StringBuilder json = new StringBuilder("{\"itens\":").append(itens);
        campo(json, "modalidadeEntrega", modalidade);
        campo(json, "cupom", cupom);
        campo(json, "formaPagamento", pagamento);
        if (parcelas != null) {
            json.append(",\"parcelas\":").append(parcelas);
        }
        campo(json, "nivelClube", nivel);
        campo(json, "regiao", regiao);
        return json.append('}').toString();
    }

    private static void campo(StringBuilder json, String nome, String valor) {
        if (valor != null) {
            json.append(",\"").append(nome).append("\":\"").append(valor).append('"');
        }
    }

    private void confere(String json, String esperado) throws Exception {
        enviar(json).andExpect(status().isOk()).andExpect(content().json(esperado, true));
    }

    private void recusa(String json, String codigo) throws Exception {
        enviar(json).andExpect(status().is4xxClientError())
                .andExpect(content().json("{\"erro\":\"" + codigo + "\"}", true));
    }

    @Test
    void exemploDoAnexo() throws Exception {
        confere(pedido(CAMISETA_E_TENIS, "EXPRESSA", "BEMVINDO10", "PIX", 1, "OURO", "SUDESTE"), """
                {"subtotalProdutos":409.70,"descontoCupom":40.97,"frete":0.00,"prazoEntregaDias":2,
                 "seguro":4.10,"ajustePagamento":-18.64,"totalFinal":354.19,"parcelas":1,
                 "valorParcela":354.19,"creditoProximaCompra":20.48,"brinde":false}""");
    }

    @Test
    void exemplo1() throws Exception {
        confere(pedido(CAMISETA_E_TENIS, "EXPRESSA", "BEMVINDO10", "PIX", null, "BRONZE", "NORTE"), """
                {"subtotalProdutos":409.70,"descontoCupom":40.97,"frete":33.10,"prazoEntregaDias":2,
                 "seguro":10.24,"ajustePagamento":-20.60,"totalFinal":391.47,"parcelas":1,
                 "valorParcela":391.47,"creditoProximaCompra":0.00,"brinde":false}""");
    }

    @Test
    void exemplo2() throws Exception {
        confere(pedido(CAMISETA_E_TENIS, "ECONOMICA", null, "CARTAO", 6, "PRATA", "CENTRO_OESTE"), """
                {"subtotalProdutos":409.70,"descontoCupom":0.00,"frete":15.60,"prazoEntregaDias":7,
                 "seguro":6.15,"ajustePagamento":30.55,"totalFinal":462.00,"parcelas":6,
                 "valorParcela":77.00,"creditoProximaCompra":8.19,"brinde":false}""");
    }

    @Test
    void exemplo3() throws Exception {
        String fone = "[{\"nome\":\"Fone\",\"precoUnitario\":199.90,\"quantidade\":2,\"pesoKg\":0.25}]";
        confere(pedido(fone, "MOTOBOY", "MENOS50", "BOLETO", 1, "BRONZE", "NORDESTE"), """
                {"subtotalProdutos":399.80,"descontoCupom":50.00,"frete":18.00,"prazoEntregaDias":0,
                 "seguro":8.00,"ajustePagamento":3.49,"totalFinal":379.29,"parcelas":1,
                 "valorParcela":379.29,"creditoProximaCompra":0.00,"brinde":false}""");
    }

    @Test
    void exemplo4() throws Exception {
        String itens = """
                [{"nome":"Meia","precoUnitario":19.90,"quantidade":7,"pesoKg":0.10},
                 {"nome":"Camiseta","precoUnitario":79.90,"quantidade":2,"pesoKg":0.30}]""";
        confere(pedido(itens, "RETIRADA_LOJA", "LEVE3PAGUE2", "CARTAO", 3, "PRATA", "SUL"), """
                {"subtotalProdutos":299.10,"descontoCupom":39.80,"frete":0.00,"prazoEntregaDias":1,
                 "seguro":2.99,"ajustePagamento":0.00,"totalFinal":262.29,"parcelas":3,
                 "valorParcela":87.43,"creditoProximaCompra":5.98,"brinde":false}""");
    }

    @Test
    void exemplo5() throws Exception {
        confere(pedido(CAMISETA_E_TENIS, "EXPRESSA", null, "PIX", 1, "OURO", "SUDESTE"), """
                {"subtotalProdutos":409.70,"descontoCupom":0.00,"frete":0.00,"prazoEntregaDias":2,
                 "seguro":4.10,"ajustePagamento":-20.69,"totalFinal":393.11,"parcelas":1,
                 "valorParcela":393.11,"creditoProximaCompra":20.48,"brinde":false}""");
    }

    @Test
    void freteGratisDescontaOValorDoFrete() throws Exception {
        confere(pedido(CAMISETA_E_TENIS, "EXPRESSA", "FRETEGRATIS", "CARTAO", 2, "BRONZE", "SUDESTE"), """
                {"subtotalProdutos":409.70,"descontoCupom":33.10,"frete":33.10,"prazoEntregaDias":2,
                 "seguro":4.10,"ajustePagamento":0.00,"totalFinal":413.80,"parcelas":2,
                 "valorParcela":206.90,"creditoProximaCompra":0.00,"brinde":false}""");
    }

    @Test
    void ouroAcimaDe500GanhaBrinde() throws Exception {
        String itens = "[{\"nome\":\"Jaqueta\",\"precoUnitario\":500.01,\"quantidade\":1,\"pesoKg\":1}]";
        enviar(pedido(itens, "ECONOMICA", null, "PIX", 1, "OURO", "SUL"))
                .andExpect(status().isOk())
                .andExpect(content().json("{\"brinde\":true,\"frete\":0.00}"));
    }

    @ParameterizedTest
    @CsvSource(nullValues = "-", value = {
            "[], EXPRESSA, -, PIX, 1, BRONZE, SUL, PEDIDO_INVALIDO",
            "'[{\"nome\":\"X\",\"precoUnitario\":0,\"quantidade\":1,\"pesoKg\":1}]', -, -, -, -, -, -, PEDIDO_INVALIDO",
            "'[{\"nome\":\"X\",\"precoUnitario\":10,\"quantidade\":1}]', EXPRESSA, -, PIX, 1, BRONZE, SUL, PEDIDO_INVALIDO",
            "'[{\"nome\":\"X\",\"precoUnitario\":10,\"quantidade\":1.5,\"pesoKg\":1}]', EXPRESSA, -, PIX, 1, BRONZE, SUL, PEDIDO_INVALIDO",
            "'[{\"nome\":\"X\",\"precoUnitario\":10,\"quantidade\":-1,\"pesoKg\":1}]', EXPRESSA, -, PIX, 1, BRONZE, SUL, PEDIDO_INVALIDO",
            "ITENS, EXPRESSA, -, PIX, 1, DIAMANTE, SUL, NIVEL_CLUBE_INVALIDO",
            "ITENS, -, -, -, -, -, SUL, NIVEL_CLUBE_INVALIDO",
            "ITENS, -, -, -, -, OURO, -, REGIAO_INVALIDA",
            "ITENS, DRONE, -, PIX, 1, OURO, SUL, MODALIDADE_INVALIDA",
            "'[{\"nome\":\"X\",\"precoUnitario\":10,\"quantidade\":6,\"pesoKg\":1}]', MOTOBOY, XYZ, PIX, 1, OURO, SUL, MODALIDADE_INDISPONIVEL",
            "ITENS, EXPRESSA, bemvindo10, PIX, 1, OURO, SUL, CUPOM_INVALIDO",
            "'[{\"nome\":\"X\",\"precoUnitario\":299.99,\"quantidade\":1,\"pesoKg\":1}]', EXPRESSA, MENOS50, -, 1, OURO, SUL, CUPOM_NAO_APLICAVEL",
            "ITENS, EXPRESSA, -, CHEQUE, 1, OURO, SUL, FORMA_PAGAMENTO_INVALIDA",
            "ITENS, EXPRESSA, -, PIX, 2, OURO, SUL, PARCELAMENTO_INVALIDO",
            "ITENS, EXPRESSA, -, CARTAO, 13, OURO, SUL, PARCELAMENTO_INVALIDO",
            "ITENS, EXPRESSA, -, CARTAO, 0, OURO, SUL, PARCELAMENTO_INVALIDO",
            "'[{\"nome\":\"X\",\"precoUnitario\":1000,\"quantidade\":1,\"pesoKg\":1}]', EXPRESSA, -, BOLETO, 2, BRONZE, SUL, PARCELAMENTO_INVALIDO",
            "'[{\"nome\":\"X\",\"precoUnitario\":1000,\"quantidade\":1,\"pesoKg\":1}]', EXPRESSA, -, BOLETO, 1, BRONZE, SUL, FORMA_PAGAMENTO_INDISPONIVEL",
    })
    void recusaNaOrdemCombinada(String itens, String modalidade, String cupom, String pagamento,
            Integer parcelas, String nivel, String regiao, String codigo) throws Exception {
        recusa(pedido("ITENS".equals(itens) ? CAMISETA_E_TENIS : itens, modalidade, cupom, pagamento,
                parcelas, nivel, regiao), codigo);
    }

    @Test
    void semItensEJsonInvalidoSaoPedidoInvalido() throws Exception {
        recusa("{}", "PEDIDO_INVALIDO");
        recusa("{\"itens\": 12", "PEDIDO_INVALIDO");
    }

    @Test
    void motoboyAceitaExatamente5Kg() throws Exception {
        String itens = "[{\"nome\":\"X\",\"precoUnitario\":10,\"quantidade\":5,\"pesoKg\":1}]";
        enviar(pedido(itens, "MOTOBOY", null, "PIX", null, "BRONZE", "SUL"))
                .andExpect(status().isOk())
                .andExpect(content().json("{\"frete\":18.00,\"prazoEntregaDias\":0}"));
    }
}

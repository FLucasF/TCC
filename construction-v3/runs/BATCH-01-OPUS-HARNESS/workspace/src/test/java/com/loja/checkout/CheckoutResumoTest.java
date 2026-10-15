package com.loja.checkout;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class CheckoutResumoTest {

    private static final String CAMISETA = "{\"nome\":\"Camiseta\",\"precoUnitario\":79.90,\"quantidade\":2,\"pesoKg\":0.30}";
    private static final String TENIS = "{\"nome\":\"Tenis\",\"precoUnitario\":249.90,\"quantidade\":1,\"pesoKg\":1.20}";

    @Autowired
    private MockMvc mockMvc;

    private org.springframework.test.web.servlet.ResultActions resumo(String corpo) throws Exception {
        return mockMvc.perform(post("/checkout/resumo")
                .contentType(MediaType.APPLICATION_JSON)
                .content(corpo));
    }

    private void erro(String corpo, String codigo) throws Exception {
        resumo(corpo).andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.erro").value(codigo));
    }

    @Test
    @DisplayName("Exemplo 1: expressa, BEMVINDO10, pix")
    void exemplo1() throws Exception {
        resumo("""
                {"itens":[%s,%s],"modalidadeEntrega":"EXPRESSA","cupom":"BEMVINDO10","formaPagamento":"PIX","parcelas":1}
                """.formatted(CAMISETA, TENIS))
                .andExpect(status().isOk())
                .andExpect(content().json("""
                        {"subtotalProdutos":409.70,"descontoCupom":40.97,"frete":33.10,"prazoEntregaDias":2,
                         "ajustePagamento":-20.09,"totalFinal":381.74,"parcelas":1,"valorParcela":381.74}
                        """));
    }

    @Test
    @DisplayName("Exemplo 2: economica, sem cupom, cartao 6x com juros")
    void exemplo2() throws Exception {
        resumo("""
                {"itens":[%s,%s],"modalidadeEntrega":"ECONOMICA","formaPagamento":"CARTAO","parcelas":6}
                """.formatted(CAMISETA, TENIS))
                .andExpect(status().isOk())
                .andExpect(content().json("""
                        {"subtotalProdutos":409.70,"descontoCupom":0.00,"frete":15.60,"prazoEntregaDias":7,
                         "ajustePagamento":30.10,"totalFinal":455.40,"parcelas":6,"valorParcela":75.90}
                        """));
    }

    @Test
    @DisplayName("Exemplo 3: motoboy, MENOS50, boleto")
    void exemplo3() throws Exception {
        resumo("""
                {"itens":[{"nome":"Fone","precoUnitario":199.90,"quantidade":2,"pesoKg":0.25}],
                 "modalidadeEntrega":"MOTOBOY","cupom":"MENOS50","formaPagamento":"BOLETO"}
                """)
                .andExpect(status().isOk())
                .andExpect(content().json("""
                        {"subtotalProdutos":399.80,"descontoCupom":50.00,"frete":18.00,"prazoEntregaDias":0,
                         "ajustePagamento":3.49,"totalFinal":371.29,"parcelas":1,"valorParcela":371.29}
                        """));
    }

    @Test
    @DisplayName("Exemplo 4: retirada, LEVE3PAGUE2, cartao 3x sem juros")
    void exemplo4() throws Exception {
        resumo("""
                {"itens":[{"nome":"Meia","precoUnitario":19.90,"quantidade":7,"pesoKg":0.10},%s],
                 "modalidadeEntrega":"RETIRADA_LOJA","cupom":"LEVE3PAGUE2","formaPagamento":"CARTAO","parcelas":3}
                """.formatted(CAMISETA))
                .andExpect(status().isOk())
                .andExpect(content().json("""
                        {"subtotalProdutos":299.10,"descontoCupom":39.80,"frete":0.00,"prazoEntregaDias":1,
                         "ajustePagamento":0.00,"totalFinal":259.30,"parcelas":3,"valorParcela":86.43}
                        """));
    }

    @Test
    @DisplayName("FRETEGRATIS zera o frete no total e aparece como desconto")
    void freteGratis() throws Exception {
        resumo("""
                {"itens":[%s],"modalidadeEntrega":"EXPRESSA","cupom":"FRETEGRATIS","formaPagamento":"CARTAO"}
                """.formatted(CAMISETA))
                .andExpect(status().isOk())
                .andExpect(content().json("""
                        {"subtotalProdutos":159.80,"descontoCupom":27.70,"frete":27.70,"prazoEntregaDias":2,
                         "ajustePagamento":0.00,"totalFinal":159.80,"parcelas":1,"valorParcela":159.80}
                        """));
    }

    @Test
    @DisplayName("Carrinho vazio ou item invalido")
    void pedidoInvalido() throws Exception {
        erro("""
                {"itens":[],"modalidadeEntrega":"EXPRESSA","formaPagamento":"PIX"}
                """, "PEDIDO_INVALIDO");
        erro("""
                {"modalidadeEntrega":"EXPRESSA","formaPagamento":"PIX"}
                """, "PEDIDO_INVALIDO");
        erro("""
                {"itens":[{"nome":"Camiseta","precoUnitario":79.90,"quantidade":0,"pesoKg":0.30}],
                 "modalidadeEntrega":"EXPRESSA","formaPagamento":"PIX"}
                """, "PEDIDO_INVALIDO");
        erro("""
                {"itens":[{"nome":"Camiseta","precoUnitario":79.90,"quantidade":1}],
                 "modalidadeEntrega":"EXPRESSA","formaPagamento":"PIX"}
                """, "PEDIDO_INVALIDO");
        erro("""
                {"itens":[{"nome":"Camiseta","precoUnitario":-1,"quantidade":1,"pesoKg":0.30}],
                 "modalidadeEntrega":"EXPRESSA","formaPagamento":"PIX"}
                """, "PEDIDO_INVALIDO");
    }

    @Test
    @DisplayName("Modalidade inexistente ou ausente")
    void modalidadeInvalida() throws Exception {
        erro("""
                {"itens":[%s],"modalidadeEntrega":"DRONE","formaPagamento":"PIX"}
                """.formatted(CAMISETA), "MODALIDADE_INVALIDA");
        erro("""
                {"itens":[%s],"formaPagamento":"PIX"}
                """.formatted(CAMISETA), "MODALIDADE_INVALIDA");
    }

    @Test
    @DisplayName("Motoboy acima de 5 kg")
    void modalidadeIndisponivel() throws Exception {
        erro("""
                {"itens":[{"nome":"Halter","precoUnitario":100.00,"quantidade":3,"pesoKg":2.00}],
                 "modalidadeEntrega":"MOTOBOY","formaPagamento":"PIX"}
                """, "MODALIDADE_INDISPONIVEL");
    }

    @Test
    @DisplayName("Cupom inexistente, inclusive em minusculas")
    void cupomInvalido() throws Exception {
        erro("""
                {"itens":[%s],"modalidadeEntrega":"EXPRESSA","cupom":"NATAL50","formaPagamento":"PIX"}
                """.formatted(CAMISETA), "CUPOM_INVALIDO");
        erro("""
                {"itens":[%s],"modalidadeEntrega":"EXPRESSA","cupom":"bemvindo10","formaPagamento":"PIX"}
                """.formatted(CAMISETA), "CUPOM_INVALIDO");
    }

    @Test
    @DisplayName("MENOS50 abaixo de R$ 300,00 em produtos")
    void cupomNaoAplicavel() throws Exception {
        erro("""
                {"itens":[%s],"modalidadeEntrega":"EXPRESSA","cupom":"MENOS50","formaPagamento":"PIX"}
                """.formatted(CAMISETA), "CUPOM_NAO_APLICAVEL");
    }

    @Test
    @DisplayName("Forma de pagamento inexistente ou ausente")
    void formaPagamentoInvalida() throws Exception {
        erro("""
                {"itens":[%s],"modalidadeEntrega":"EXPRESSA","formaPagamento":"CRIPTO"}
                """.formatted(CAMISETA), "FORMA_PAGAMENTO_INVALIDA");
        erro("""
                {"itens":[%s],"modalidadeEntrega":"EXPRESSA"}
                """.formatted(CAMISETA), "FORMA_PAGAMENTO_INVALIDA");
    }

    @Test
    @DisplayName("Parcelamento fora do permitido")
    void parcelamentoInvalido() throws Exception {
        erro("""
                {"itens":[%s],"modalidadeEntrega":"EXPRESSA","formaPagamento":"PIX","parcelas":2}
                """.formatted(CAMISETA), "PARCELAMENTO_INVALIDO");
        erro("""
                {"itens":[%s],"modalidadeEntrega":"EXPRESSA","formaPagamento":"BOLETO","parcelas":3}
                """.formatted(CAMISETA), "PARCELAMENTO_INVALIDO");
        erro("""
                {"itens":[%s],"modalidadeEntrega":"EXPRESSA","formaPagamento":"CARTAO","parcelas":13}
                """.formatted(CAMISETA), "PARCELAMENTO_INVALIDO");
        erro("""
                {"itens":[%s],"modalidadeEntrega":"EXPRESSA","formaPagamento":"CARTAO","parcelas":0}
                """.formatted(CAMISETA), "PARCELAMENTO_INVALIDO");
    }

    @Test
    @DisplayName("Boleto acima de R$ 1.000,00 no total do pedido")
    void formaPagamentoIndisponivel() throws Exception {
        erro("""
                {"itens":[{"nome":"Jaqueta","precoUnitario":600.00,"quantidade":2,"pesoKg":1.00}],
                 "modalidadeEntrega":"RETIRADA_LOJA","formaPagamento":"BOLETO"}
                """, "FORMA_PAGAMENTO_INDISPONIVEL");
    }

    @Test
    @DisplayName("Erros seguem a ordem combinada")
    void ordemDosErros() throws Exception {
        erro("""
                {"itens":[],"modalidadeEntrega":"DRONE","cupom":"NATAL50","formaPagamento":"CRIPTO","parcelas":99}
                """, "PEDIDO_INVALIDO");
        erro("""
                {"itens":[{"nome":"Halter","precoUnitario":100.00,"quantidade":3,"pesoKg":2.00}],
                 "modalidadeEntrega":"MOTOBOY","cupom":"NATAL50","formaPagamento":"CRIPTO"}
                """, "MODALIDADE_INDISPONIVEL");
        erro("""
                {"itens":[%s],"modalidadeEntrega":"EXPRESSA","cupom":"MENOS50","formaPagamento":"CRIPTO"}
                """.formatted(CAMISETA), "CUPOM_NAO_APLICAVEL");
        erro("""
                {"itens":[{"nome":"Jaqueta","precoUnitario":600.00,"quantidade":2,"pesoKg":1.00}],
                 "modalidadeEntrega":"RETIRADA_LOJA","formaPagamento":"BOLETO","parcelas":2}
                """, "PARCELAMENTO_INVALIDO");
    }
}

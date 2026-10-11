package com.loja.resumo.api;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class ResumoCompraTest {

    private static final String CAMISETA_E_TENIS = """
            {"nome": "Camiseta", "precoUnitario": 79.90, "quantidade": 2, "pesoKg": 0.30},
            {"nome": "Tenis", "precoUnitario": 249.90, "quantidade": 1, "pesoKg": 1.20}
            """;

    @Autowired
    private MockMvc mvc;

    @Test
    void exemplo1PixComCupomBemVindo() throws Exception {
        resumo("""
                {"itens": [%s],
                 "modalidadeEntrega": "EXPRESSA", "cupom": "BEMVINDO10", "formaPagamento": "PIX",
                 "nivelClube": "BRONZE", "regiao": "NORTE"}
                """.formatted(CAMISETA_E_TENIS))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.subtotalProdutos").value(409.70))
                .andExpect(jsonPath("$.descontoCupom").value(40.97))
                .andExpect(jsonPath("$.frete").value(33.10))
                .andExpect(jsonPath("$.prazoEntregaDias").value(2))
                .andExpect(jsonPath("$.seguro").value(10.24))
                .andExpect(jsonPath("$.ajustePagamento").value(-20.60))
                .andExpect(jsonPath("$.totalFinal").value(391.47))
                .andExpect(jsonPath("$.parcelas").value(1))
                .andExpect(jsonPath("$.valorParcela").value(391.47))
                .andExpect(jsonPath("$.creditoProximaCompra").value(0.00))
                .andExpect(jsonPath("$.brinde").value(false));
    }

    @Test
    void exemplo2CartaoSeisVezesComJurosPrata() throws Exception {
        resumo("""
                {"itens": [%s],
                 "modalidadeEntrega": "ECONOMICA", "formaPagamento": "CARTAO", "parcelas": 6,
                 "nivelClube": "PRATA", "regiao": "CENTRO_OESTE"}
                """.formatted(CAMISETA_E_TENIS))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.subtotalProdutos").value(409.70))
                .andExpect(jsonPath("$.descontoCupom").value(0.00))
                .andExpect(jsonPath("$.frete").value(15.60))
                .andExpect(jsonPath("$.prazoEntregaDias").value(7))
                .andExpect(jsonPath("$.seguro").value(6.15))
                .andExpect(jsonPath("$.ajustePagamento").value(30.55))
                .andExpect(jsonPath("$.totalFinal").value(462.00))
                .andExpect(jsonPath("$.parcelas").value(6))
                .andExpect(jsonPath("$.valorParcela").value(77.00))
                .andExpect(jsonPath("$.creditoProximaCompra").value(8.19))
                .andExpect(jsonPath("$.brinde").value(false));
    }

    @Test
    void exemplo3BoletoComMotoboyEMenos50() throws Exception {
        resumo("""
                {"itens": [{"nome": "Fone", "precoUnitario": 199.90, "quantidade": 2, "pesoKg": 0.25}],
                 "modalidadeEntrega": "MOTOBOY", "cupom": "MENOS50", "formaPagamento": "BOLETO",
                 "nivelClube": "BRONZE", "regiao": "NORDESTE"}
                """)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.subtotalProdutos").value(399.80))
                .andExpect(jsonPath("$.descontoCupom").value(50.00))
                .andExpect(jsonPath("$.frete").value(18.00))
                .andExpect(jsonPath("$.prazoEntregaDias").value(0))
                .andExpect(jsonPath("$.seguro").value(8.00))
                .andExpect(jsonPath("$.ajustePagamento").value(3.49))
                .andExpect(jsonPath("$.totalFinal").value(379.29))
                .andExpect(jsonPath("$.parcelas").value(1))
                .andExpect(jsonPath("$.valorParcela").value(379.29))
                .andExpect(jsonPath("$.creditoProximaCompra").value(0.00))
                .andExpect(jsonPath("$.brinde").value(false));
    }

    @Test
    void exemplo4CartaoTresVezesSemJurosLeve3Pague2() throws Exception {
        resumo("""
                {"itens": [
                   {"nome": "Meia", "precoUnitario": 19.90, "quantidade": 7, "pesoKg": 0.10},
                   {"nome": "Camiseta", "precoUnitario": 79.90, "quantidade": 2, "pesoKg": 0.30}],
                 "modalidadeEntrega": "RETIRADA_LOJA", "cupom": "LEVE3PAGUE2", "formaPagamento": "CARTAO",
                 "parcelas": 3, "nivelClube": "PRATA", "regiao": "SUL"}
                """)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.subtotalProdutos").value(299.10))
                .andExpect(jsonPath("$.descontoCupom").value(39.80))
                .andExpect(jsonPath("$.frete").value(0.00))
                .andExpect(jsonPath("$.prazoEntregaDias").value(1))
                .andExpect(jsonPath("$.seguro").value(2.99))
                .andExpect(jsonPath("$.ajustePagamento").value(0.00))
                .andExpect(jsonPath("$.totalFinal").value(262.29))
                .andExpect(jsonPath("$.parcelas").value(3))
                .andExpect(jsonPath("$.valorParcela").value(87.43))
                .andExpect(jsonPath("$.creditoProximaCompra").value(5.98))
                .andExpect(jsonPath("$.brinde").value(false));
    }

    @Test
    void exemplo5PixOuroFreteZeradoSemBrinde() throws Exception {
        resumo("""
                {"itens": [%s],
                 "modalidadeEntrega": "EXPRESSA", "formaPagamento": "PIX",
                 "nivelClube": "OURO", "regiao": "SUDESTE"}
                """.formatted(CAMISETA_E_TENIS))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.subtotalProdutos").value(409.70))
                .andExpect(jsonPath("$.descontoCupom").value(0.00))
                .andExpect(jsonPath("$.frete").value(0.00))
                .andExpect(jsonPath("$.prazoEntregaDias").value(2))
                .andExpect(jsonPath("$.seguro").value(4.10))
                .andExpect(jsonPath("$.ajustePagamento").value(-20.69))
                .andExpect(jsonPath("$.totalFinal").value(393.11))
                .andExpect(jsonPath("$.creditoProximaCompra").value(20.48))
                .andExpect(jsonPath("$.brinde").value(false));
    }

    @Test
    void ouroComSubtotalAcimaDe500GanhaBrinde() throws Exception {
        resumo("""
                {"itens": [{"nome": "Tenis", "precoUnitario": 249.90, "quantidade": 3, "pesoKg": 1.20}],
                 "modalidadeEntrega": "ECONOMICA", "formaPagamento": "PIX",
                 "nivelClube": "OURO", "regiao": "SUL"}
                """)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.subtotalProdutos").value(749.70))
                .andExpect(jsonPath("$.frete").value(0.00))
                .andExpect(jsonPath("$.brinde").value(true));
    }

    @Test
    void ouroComSubtotalExatamente500NaoGanhaBrinde() throws Exception {
        resumo("""
                {"itens": [{"nome": "Bolsa", "precoUnitario": 250.00, "quantidade": 2, "pesoKg": 0.50}],
                 "modalidadeEntrega": "RETIRADA_LOJA", "formaPagamento": "PIX",
                 "nivelClube": "OURO", "regiao": "SUL"}
                """)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.brinde").value(false));
    }

    @Test
    void motoboyComExatamente5KgEhAceito() throws Exception {
        resumo("""
                {"itens": [{"nome": "Sofa", "precoUnitario": 900.00, "quantidade": 1, "pesoKg": 5.00}],
                 "modalidadeEntrega": "MOTOBOY", "formaPagamento": "PIX",
                 "nivelClube": "BRONZE", "regiao": "SUL"}
                """)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.frete").value(18.00));
    }

    @Test
    void menos50ComSubtotalExatamente300EhAplicavel() throws Exception {
        resumo("""
                {"itens": [{"nome": "Bolsa", "precoUnitario": 150.00, "quantidade": 2, "pesoKg": 0.50}],
                 "modalidadeEntrega": "RETIRADA_LOJA", "cupom": "MENOS50", "formaPagamento": "PIX",
                 "nivelClube": "BRONZE", "regiao": "SUL"}
                """)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.descontoCupom").value(50.00));
    }

    @Test
    void nivelInvalidoTemPrecedenciaSobreModalidadeIndisponivel() throws Exception {
        erro("""
                {"itens": [{"nome": "Sofa", "precoUnitario": 900.00, "quantidade": 1, "pesoKg": 6.00}],
                 "modalidadeEntrega": "MOTOBOY", "cupom": "NATAL", "formaPagamento": "CHEQUE",
                 "nivelClube": "DIAMANTE", "regiao": "SUL"}
                """, "NIVEL_CLUBE_INVALIDO");
    }

    @Test
    void cupomInvalidoTemPrecedenciaSobreFormaPagamentoInvalida() throws Exception {
        erro("""
                {"itens": [{"nome": "Meia", "precoUnitario": 19.90, "quantidade": 1, "pesoKg": 0.10}],
                 "modalidadeEntrega": "RETIRADA_LOJA", "cupom": "NATAL", "formaPagamento": "CHEQUE",
                 "nivelClube": "BRONZE", "regiao": "SUL"}
                """, "CUPOM_INVALIDO");
    }

    @Test
    void itemNuloEhPedidoInvalido() throws Exception {
        erro("""
                {"itens": [null],
                 "modalidadeEntrega": "RETIRADA_LOJA", "formaPagamento": "PIX",
                 "nivelClube": "BRONZE", "regiao": "SUL"}
                """, "PEDIDO_INVALIDO");
    }

    @Test
    void freteGratisDaCupomIgualAoFrete() throws Exception {
        resumo("""
                {"itens": [%s],
                 "modalidadeEntrega": "EXPRESSA", "cupom": "FRETEGRATIS", "formaPagamento": "PIX",
                 "nivelClube": "BRONZE", "regiao": "SUL"}
                """.formatted(CAMISETA_E_TENIS))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.frete").value(33.10))
                .andExpect(jsonPath("$.descontoCupom").value(33.10));
    }

    @Test
    void boletoComTotalAteMilReaisEhAceito() throws Exception {
        resumo("""
                {"itens": [{"nome": "Casaco", "precoUnitario": 450.00, "quantidade": 2, "pesoKg": 0.80}],
                 "modalidadeEntrega": "RETIRADA_LOJA", "formaPagamento": "BOLETO",
                 "nivelClube": "BRONZE", "regiao": "SUDESTE"}
                """)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.seguro").value(9.00))
                .andExpect(jsonPath("$.totalFinal").value(912.49));
    }

    @Test
    void carrinhoVazioEhPedidoInvalido() throws Exception {
        erro("""
                {"itens": [], "modalidadeEntrega": "RETIRADA_LOJA", "formaPagamento": "PIX",
                 "nivelClube": "BRONZE", "regiao": "SUL"}
                """, "PEDIDO_INVALIDO");
    }

    @Test
    void itemComPesoZeroEhPedidoInvalido() throws Exception {
        erro("""
                {"itens": [{"nome": "Meia", "precoUnitario": 19.90, "quantidade": 1, "pesoKg": 0}],
                 "modalidadeEntrega": "RETIRADA_LOJA", "formaPagamento": "PIX",
                 "nivelClube": "BRONZE", "regiao": "SUL"}
                """, "PEDIDO_INVALIDO");
    }

    @Test
    void nivelDesconhecidoEhNivelClubeInvalido() throws Exception {
        erro("""
                {"itens": [{"nome": "Meia", "precoUnitario": 19.90, "quantidade": 1, "pesoKg": 0.10}],
                 "modalidadeEntrega": "RETIRADA_LOJA", "formaPagamento": "PIX",
                 "nivelClube": "DIAMANTE", "regiao": "SUL"}
                """, "NIVEL_CLUBE_INVALIDO");
    }

    @Test
    void regiaoDesconhecidaEhRegiaoInvalida() throws Exception {
        erro("""
                {"itens": [{"nome": "Meia", "precoUnitario": 19.90, "quantidade": 1, "pesoKg": 0.10}],
                 "modalidadeEntrega": "RETIRADA_LOJA", "formaPagamento": "PIX",
                 "nivelClube": "BRONZE", "regiao": "LUA"}
                """, "REGIAO_INVALIDA");
    }

    @Test
    void modalidadeDesconhecidaEhModalidadeInvalida() throws Exception {
        erro("""
                {"itens": [{"nome": "Meia", "precoUnitario": 19.90, "quantidade": 1, "pesoKg": 0.10}],
                 "modalidadeEntrega": "DRONE", "formaPagamento": "PIX",
                 "nivelClube": "BRONZE", "regiao": "SUL"}
                """, "MODALIDADE_INVALIDA");
    }

    @Test
    void motoboyAcimaDe5KgEhModalidadeIndisponivel() throws Exception {
        erro("""
                {"itens": [{"nome": "Sofa", "precoUnitario": 900.00, "quantidade": 1, "pesoKg": 6.00}],
                 "modalidadeEntrega": "MOTOBOY", "formaPagamento": "PIX",
                 "nivelClube": "BRONZE", "regiao": "SUL"}
                """, "MODALIDADE_INDISPONIVEL");
    }

    @Test
    void cupomDesconhecidoEhCupomInvalido() throws Exception {
        erro("""
                {"itens": [{"nome": "Meia", "precoUnitario": 19.90, "quantidade": 1, "pesoKg": 0.10}],
                 "modalidadeEntrega": "RETIRADA_LOJA", "cupom": "NATAL", "formaPagamento": "PIX",
                 "nivelClube": "BRONZE", "regiao": "SUL"}
                """, "CUPOM_INVALIDO");
    }

    @Test
    void menos50AbaixoDe300EhCupomNaoAplicavel() throws Exception {
        erro("""
                {"itens": [{"nome": "Meia", "precoUnitario": 19.90, "quantidade": 2, "pesoKg": 0.10}],
                 "modalidadeEntrega": "RETIRADA_LOJA", "cupom": "MENOS50", "formaPagamento": "PIX",
                 "nivelClube": "BRONZE", "regiao": "SUL"}
                """, "CUPOM_NAO_APLICAVEL");
    }

    @Test
    void formaPagamentoDesconhecidaEhFormaPagamentoInvalida() throws Exception {
        erro("""
                {"itens": [{"nome": "Meia", "precoUnitario": 19.90, "quantidade": 1, "pesoKg": 0.10}],
                 "modalidadeEntrega": "RETIRADA_LOJA", "formaPagamento": "CHEQUE",
                 "nivelClube": "BRONZE", "regiao": "SUL"}
                """, "FORMA_PAGAMENTO_INVALIDA");
    }

    @Test
    void pixEmDuasVezesEhParcelamentoInvalido() throws Exception {
        erro("""
                {"itens": [%s],
                 "modalidadeEntrega": "RETIRADA_LOJA", "formaPagamento": "PIX", "parcelas": 2,
                 "nivelClube": "BRONZE", "regiao": "SUL"}
                """.formatted(CAMISETA_E_TENIS), "PARCELAMENTO_INVALIDO");
    }

    @Test
    void cartaoEm13VezesEhParcelamentoInvalido() throws Exception {
        erro("""
                {"itens": [%s],
                 "modalidadeEntrega": "RETIRADA_LOJA", "formaPagamento": "CARTAO", "parcelas": 13,
                 "nivelClube": "BRONZE", "regiao": "SUL"}
                """.formatted(CAMISETA_E_TENIS), "PARCELAMENTO_INVALIDO");
    }

    @Test
    void boletoAcimaDeMilReaisEhFormaPagamentoIndisponivel() throws Exception {
        erro("""
                {"itens": [{"nome": "Casaco", "precoUnitario": 500.00, "quantidade": 2, "pesoKg": 0.80}],
                 "modalidadeEntrega": "EXPRESSA", "formaPagamento": "BOLETO",
                 "nivelClube": "BRONZE", "regiao": "SUDESTE"}
                """, "FORMA_PAGAMENTO_INDISPONIVEL");
    }

    private ResultActions resumo(String json) throws Exception {
        return mvc.perform(post("/checkout/resumo").contentType(MediaType.APPLICATION_JSON).content(json));
    }

    private void erro(String json, String codigo) throws Exception {
        resumo(json)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.erro").value(codigo));
    }

}

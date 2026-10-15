package com.loja.checkout;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

/** Quando nao da para calcular, o servico recusa o pedido com o codigo do problema. */
@SpringBootTest
@AutoConfigureMockMvc
class ResumoRecusasTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    @DisplayName("carrinho vazio e pedido invalido")
    void carrinhoVazio() throws Exception {
        recusa("""
                {
                  "itens": [],
                  "modalidadeEntrega": "EXPRESSA",
                  "formaPagamento": "PIX",
                  "nivelClube": "BRONZE",
                  "regiao": "SUDESTE"
                }
                """, "PEDIDO_INVALIDO");
    }

    @Test
    @DisplayName("item sem itens e pedido invalido")
    void semListaDeItens() throws Exception {
        recusa("""
                {
                  "modalidadeEntrega": "EXPRESSA",
                  "formaPagamento": "PIX",
                  "nivelClube": "BRONZE",
                  "regiao": "SUDESTE"
                }
                """, "PEDIDO_INVALIDO");
    }

    @Test
    @DisplayName("quantidade zero e pedido invalido")
    void quantidadeZero() throws Exception {
        recusa(pedidoCom("""
                { "nome": "Camiseta", "precoUnitario": 79.90, "quantidade": 0, "pesoKg": 0.30 }
                """), "PEDIDO_INVALIDO");
    }

    @Test
    @DisplayName("preco negativo e pedido invalido")
    void precoNegativo() throws Exception {
        recusa(pedidoCom("""
                { "nome": "Camiseta", "precoUnitario": -79.90, "quantidade": 1, "pesoKg": 0.30 }
                """), "PEDIDO_INVALIDO");
    }

    @Test
    @DisplayName("peso ausente e pedido invalido")
    void pesoAusente() throws Exception {
        recusa(pedidoCom("""
                { "nome": "Camiseta", "precoUnitario": 79.90, "quantidade": 1 }
                """), "PEDIDO_INVALIDO");
    }

    @Test
    @DisplayName("nivel do clube que nao existe")
    void nivelInexistente() throws Exception {
        recusa("""
                {
                  "itens": [ { "nome": "Camiseta", "precoUnitario": 79.90, "quantidade": 1, "pesoKg": 0.30 } ],
                  "modalidadeEntrega": "EXPRESSA",
                  "formaPagamento": "PIX",
                  "nivelClube": "DIAMANTE",
                  "regiao": "SUDESTE"
                }
                """, "NIVEL_CLUBE_INVALIDO");
    }

    @Test
    @DisplayName("nivel do clube nao informado")
    void nivelAusente() throws Exception {
        recusa("""
                {
                  "itens": [ { "nome": "Camiseta", "precoUnitario": 79.90, "quantidade": 1, "pesoKg": 0.30 } ],
                  "modalidadeEntrega": "EXPRESSA",
                  "formaPagamento": "PIX",
                  "regiao": "SUDESTE"
                }
                """, "NIVEL_CLUBE_INVALIDO");
    }

    @Test
    @DisplayName("regiao que nao existe")
    void regiaoInexistente() throws Exception {
        recusa("""
                {
                  "itens": [ { "nome": "Camiseta", "precoUnitario": 79.90, "quantidade": 1, "pesoKg": 0.30 } ],
                  "modalidadeEntrega": "EXPRESSA",
                  "formaPagamento": "PIX",
                  "nivelClube": "BRONZE",
                  "regiao": "SUDOESTE"
                }
                """, "REGIAO_INVALIDA");
    }

    @Test
    @DisplayName("modalidade de entrega que nao existe")
    void modalidadeInexistente() throws Exception {
        recusa("""
                {
                  "itens": [ { "nome": "Camiseta", "precoUnitario": 79.90, "quantidade": 1, "pesoKg": 0.30 } ],
                  "modalidadeEntrega": "DRONE",
                  "formaPagamento": "PIX",
                  "nivelClube": "BRONZE",
                  "regiao": "SUDESTE"
                }
                """, "MODALIDADE_INVALIDA");
    }

    @Test
    @DisplayName("motoboy acima de 5 kg nao atende o pedido")
    void motoboyAcimaDoPesoMaximo() throws Exception {
        recusa("""
                {
                  "itens": [ { "nome": "Bota", "precoUnitario": 100.00, "quantidade": 3, "pesoKg": 2.00 } ],
                  "modalidadeEntrega": "MOTOBOY",
                  "formaPagamento": "PIX",
                  "nivelClube": "BRONZE",
                  "regiao": "SUDESTE"
                }
                """, "MODALIDADE_INDISPONIVEL");
    }

    @Test
    @DisplayName("cupom que nao existe")
    void cupomInexistente() throws Exception {
        recusa("""
                {
                  "itens": [ { "nome": "Camiseta", "precoUnitario": 79.90, "quantidade": 1, "pesoKg": 0.30 } ],
                  "modalidadeEntrega": "EXPRESSA",
                  "cupom": "PROMOCAOQUENAOEXISTE",
                  "formaPagamento": "PIX",
                  "nivelClube": "BRONZE",
                  "regiao": "SUDESTE"
                }
                """, "CUPOM_INVALIDO");
    }

    @Test
    @DisplayName("cupom em minusculas nao existe")
    void cupomEmMinusculas() throws Exception {
        recusa("""
                {
                  "itens": [ { "nome": "Camiseta", "precoUnitario": 79.90, "quantidade": 1, "pesoKg": 0.30 } ],
                  "modalidadeEntrega": "EXPRESSA",
                  "cupom": "bemvindo10",
                  "formaPagamento": "PIX",
                  "nivelClube": "BRONZE",
                  "regiao": "SUDESTE"
                }
                """, "CUPOM_INVALIDO");
    }

    @Test
    @DisplayName("MENOS50 abaixo de R$ 300,00 em produtos")
    void menos50SemOMinimo() throws Exception {
        recusa("""
                {
                  "itens": [ { "nome": "Camiseta", "precoUnitario": 79.90, "quantidade": 1, "pesoKg": 0.30 } ],
                  "modalidadeEntrega": "EXPRESSA",
                  "cupom": "MENOS50",
                  "formaPagamento": "PIX",
                  "nivelClube": "BRONZE",
                  "regiao": "SUDESTE"
                }
                """, "CUPOM_NAO_APLICAVEL");
    }

    @Test
    @DisplayName("forma de pagamento que nao existe")
    void formaPagamentoInexistente() throws Exception {
        recusa("""
                {
                  "itens": [ { "nome": "Camiseta", "precoUnitario": 79.90, "quantidade": 1, "pesoKg": 0.30 } ],
                  "modalidadeEntrega": "EXPRESSA",
                  "formaPagamento": "CRIPTOMOEDA",
                  "nivelClube": "BRONZE",
                  "regiao": "SUDESTE"
                }
                """, "FORMA_PAGAMENTO_INVALIDA");
    }

    @Test
    @DisplayName("pix nao parcela")
    void pixParcelado() throws Exception {
        recusa("""
                {
                  "itens": [ { "nome": "Camiseta", "precoUnitario": 79.90, "quantidade": 1, "pesoKg": 0.30 } ],
                  "modalidadeEntrega": "EXPRESSA",
                  "formaPagamento": "PIX",
                  "parcelas": 2,
                  "nivelClube": "BRONZE",
                  "regiao": "SUDESTE"
                }
                """, "PARCELAMENTO_INVALIDO");
    }

    @Test
    @DisplayName("boleto nao parcela")
    void boletoParcelado() throws Exception {
        recusa("""
                {
                  "itens": [ { "nome": "Camiseta", "precoUnitario": 79.90, "quantidade": 1, "pesoKg": 0.30 } ],
                  "modalidadeEntrega": "EXPRESSA",
                  "formaPagamento": "BOLETO",
                  "parcelas": 3,
                  "nivelClube": "BRONZE",
                  "regiao": "SUDESTE"
                }
                """, "PARCELAMENTO_INVALIDO");
    }

    @Test
    @DisplayName("cartao acima de 12x")
    void cartaoAcimaDoLimiteDeParcelas() throws Exception {
        recusa("""
                {
                  "itens": [ { "nome": "Camiseta", "precoUnitario": 79.90, "quantidade": 1, "pesoKg": 0.30 } ],
                  "modalidadeEntrega": "EXPRESSA",
                  "formaPagamento": "CARTAO",
                  "parcelas": 13,
                  "nivelClube": "BRONZE",
                  "regiao": "SUDESTE"
                }
                """, "PARCELAMENTO_INVALIDO");
    }

    @Test
    @DisplayName("boleto acima de R$ 1.000,00 no total do pedido")
    void boletoAcimaDoTotalMaximo() throws Exception {
        recusa("""
                {
                  "itens": [ { "nome": "Sofa", "precoUnitario": 1200.00, "quantidade": 1, "pesoKg": 0.50 } ],
                  "modalidadeEntrega": "RETIRADA_LOJA",
                  "formaPagamento": "BOLETO",
                  "nivelClube": "BRONZE",
                  "regiao": "SUDESTE"
                }
                """, "FORMA_PAGAMENTO_INDISPONIVEL");
    }

    @Test
    @DisplayName("quando ha vários problemas, vale o primeiro da ordem de conferencia")
    void devolveOPrimeiroProblema() throws Exception {
        recusa("""
                {
                  "itens": [],
                  "modalidadeEntrega": "DRONE",
                  "cupom": "NAOEXISTE",
                  "formaPagamento": "CRIPTOMOEDA",
                  "parcelas": 99,
                  "nivelClube": "DIAMANTE",
                  "regiao": "SUDOESTE"
                }
                """, "PEDIDO_INVALIDO");
    }

    @Test
    @DisplayName("clube invalido vem antes da regiao invalida")
    void clubeAntesDaRegiao() throws Exception {
        recusa("""
                {
                  "itens": [ { "nome": "Camiseta", "precoUnitario": 79.90, "quantidade": 1, "pesoKg": 0.30 } ],
                  "modalidadeEntrega": "DRONE",
                  "formaPagamento": "CRIPTOMOEDA",
                  "nivelClube": "DIAMANTE",
                  "regiao": "SUDOESTE"
                }
                """, "NIVEL_CLUBE_INVALIDO");
    }

    @Test
    @DisplayName("corpo com tipo errado e pedido invalido")
    void corpoComTipoErrado() throws Exception {
        recusa("""
                {
                  "itens": [ { "nome": "Camiseta", "precoUnitario": "setenta", "quantidade": 1, "pesoKg": 0.30 } ],
                  "modalidadeEntrega": "EXPRESSA",
                  "formaPagamento": "PIX",
                  "nivelClube": "BRONZE",
                  "regiao": "SUDESTE"
                }
                """, "PEDIDO_INVALIDO");
    }

    private String pedidoCom(String item) {
        return """
                {
                  "itens": [ %s ],
                  "modalidadeEntrega": "EXPRESSA",
                  "formaPagamento": "PIX",
                  "nivelClube": "BRONZE",
                  "regiao": "SUDESTE"
                }
                """.formatted(item);
    }

    private void recusa(String pedido, String codigoEsperado) throws Exception {
        mockMvc.perform(post("/checkout/resumo")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(pedido))
                .andExpect(status().isBadRequest())
                .andExpect(content().json("{ \"erro\": \"%s\" }".formatted(codigoEsperado), true));
    }
}

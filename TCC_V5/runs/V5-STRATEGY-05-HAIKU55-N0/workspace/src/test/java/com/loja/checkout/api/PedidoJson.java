package com.loja.checkout.api;

import java.util.ArrayList;
import java.util.List;

final class PedidoJson {

    static final String ITENS_PADRAO = """
            [
              {"nome": "Camiseta", "precoUnitario": 79.90, "quantidade": 2, "pesoKg": 0.30},
              {"nome": "Tenis", "precoUnitario": 249.90, "quantidade": 1, "pesoKg": 1.20}
            ]
            """;

    private PedidoJson() {
    }

    static String montar(String itens, String modalidade, String cupom, String pagamento,
                         String parcelas, String nivel, String regiao) {
        List<String> campos = new ArrayList<>();
        campos.add("\"itens\": " + itens);
        adicionarTexto(campos, "modalidadeEntrega", modalidade);
        adicionarTexto(campos, "cupom", cupom);
        adicionarTexto(campos, "formaPagamento", pagamento);
        if (parcelas != null) {
            campos.add("\"parcelas\": " + parcelas);
        }
        adicionarTexto(campos, "nivelClube", nivel);
        adicionarTexto(campos, "regiao", regiao);
        return "{" + String.join(", ", campos) + "}";
    }

    private static void adicionarTexto(List<String> campos, String nome, String valor) {
        if (valor != null) {
            campos.add("\"" + nome + "\": \"" + valor + "\"");
        }
    }
}

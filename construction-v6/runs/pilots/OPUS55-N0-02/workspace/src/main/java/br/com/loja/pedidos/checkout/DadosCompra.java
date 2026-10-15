package br.com.loja.pedidos.checkout;

import br.com.loja.pedidos.comum.ErroNegocio;
import br.com.loja.pedidos.comum.Json;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Os dados da compra, como chegam do site. Só a estrutura básica (cliente e carrinho) é conferida
 * aqui; o resto fica como texto e é conferido pela {@link CalculadoraCheckout}, na ordem combinada.
 *
 * @param parcelas {@code null} quando o valor informado não é um número inteiro
 */
public record DadosCompra(
        String cpf,
        String email,
        List<ItemCompra> itens,
        String cep,
        String regiao,
        String modalidadeEntrega,
        String cupom,
        String formaPagamento,
        Integer parcelas,
        String nivelClube) {

    public static DadosCompra ler(Object corpo) {
        if (!(corpo instanceof Map<?, ?> dados)) {
            throw invalido();
        }
        List<ItemCompra> itens = lerItens(dados.get("itens"));
        String cpf = null;
        String email = null;
        if (dados.get("cliente") instanceof Map<?, ?> cliente) {
            cpf = Json.texto(cliente.get("cpf"));
            email = Json.texto(cliente.get("email"));
        }
        if (cpf == null || email == null) {
            throw invalido();
        }
        Object cupom = dados.get("cupom");
        Object parcelas = dados.get("parcelas");
        return new DadosCompra(
                cpf,
                email,
                itens,
                Json.texto(dados.get("cep")),
                Json.texto(dados.get("regiao")),
                Json.texto(dados.get("modalidadeEntrega")),
                cupom == null ? null : String.valueOf(cupom),
                Json.texto(dados.get("formaPagamento")),
                parcelas == null ? Integer.valueOf(1) : Json.inteiroPequeno(parcelas),
                Json.texto(dados.get("nivelClube")));
    }

    private static List<ItemCompra> lerItens(Object valor) {
        if (!(valor instanceof List<?> lista) || lista.isEmpty()) {
            throw invalido();
        }
        List<ItemCompra> itens = new ArrayList<>();
        Set<String> skus = new HashSet<>();
        for (Object elemento : lista) {
            if (!(elemento instanceof Map<?, ?> item)) {
                throw invalido();
            }
            String sku = Json.texto(item.get("sku"));
            Long lida = Json.inteiro(item.get("quantidade"));
            // quantidades acima do que cabe num inteiro nunca têm estoque: viram o maior inteiro possível
            Integer quantidade = lida == null || lida <= 0 ? null : (int) Math.min(lida, Integer.MAX_VALUE);
            if (sku == null || quantidade == null || quantidade <= 0 || !skus.add(sku)) {
                throw invalido();
            }
            itens.add(new ItemCompra(sku, quantidade));
        }
        return List.copyOf(itens);
    }

    private static ErroNegocio invalido() {
        return ErroNegocio.naoProcessavel("PEDIDO_INVALIDO");
    }
}

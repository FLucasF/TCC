package br.com.loja.pedidos.pedido;

import br.com.loja.pedidos.checkout.Resumo;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public record Pedido(
        long id,
        String cpf,
        SituacaoPedido situacao,
        Instant criadoEm,
        Resumo resumo,
        List<ItemPedido> itens,
        String codigoRastreio,
        String idTransacao) {

    public Pedido comItens(List<ItemPedido> novosItens) {
        return new Pedido(id, cpf, situacao, criadoEm, resumo, List.copyOf(novosItens), codigoRastreio, idTransacao);
    }

    /** Quantidade de cada produto (SKU → quantidade). */
    public Map<String, Integer> quantidades() {
        Map<String, Integer> quantidades = new LinkedHashMap<>();
        itens.forEach(item -> quantidades.put(item.sku(), item.quantidade()));
        return quantidades;
    }
}

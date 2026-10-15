package br.com.loja.checkout.api;

import br.com.loja.checkout.resumo.Compra;
import br.com.loja.checkout.resumo.Item;
import java.util.List;
import java.util.Objects;

public record ResumoRequest(
        List<ItemRequest> itens,
        String modalidadeEntrega,
        String cupom,
        String formaPagamento,
        Integer parcelas,
        String nivelClube,
        String regiao) {

    Compra paraCompra() {
        return new Compra(
                itens == null ? null : itens.stream().map(ResumoRequest::paraItem).toList(),
                modalidadeEntrega,
                cupom,
                formaPagamento,
                Objects.requireNonNullElse(parcelas, 1),
                nivelClube,
                regiao);
    }

    /** Item ausente ou sem quantidade vira item com quantidade zero, que a validação recusa. */
    private static Item paraItem(ItemRequest item) {
        if (item == null) {
            return new Item(null, null, 0, null);
        }
        return new Item(
                item.nome(),
                item.precoUnitario(),
                Objects.requireNonNullElse(item.quantidade(), 0),
                item.pesoKg());
    }
}

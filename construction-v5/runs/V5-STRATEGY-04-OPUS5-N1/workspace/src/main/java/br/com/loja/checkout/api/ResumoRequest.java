package br.com.loja.checkout.api;

import java.math.BigDecimal;
import java.util.List;

import br.com.loja.checkout.dominio.Compra;
import br.com.loja.checkout.dominio.Item;

/** O que o site envia para /checkout/resumo. */
public record ResumoRequest(List<ItemRequest> itens,
                            String modalidadeEntrega,
                            String cupom,
                            String formaPagamento,
                            Integer parcelas,
                            String nivelClube,
                            String regiao) {

    public record ItemRequest(String nome,
                              BigDecimal precoUnitario,
                              Integer quantidade,
                              BigDecimal pesoKg) {

        Item paraDominio() {
            return new Item(nome, precoUnitario, quantidade == null ? 0 : quantidade, pesoKg);
        }
    }

    public Compra paraDominio() {
        List<Item> itensDominio = itens == null ? null : itens.stream()
                .map(item -> item == null ? null : item.paraDominio())
                .toList();
        return new Compra(itensDominio, modalidadeEntrega, cupom, formaPagamento,
                parcelas, nivelClube, regiao);
    }
}

package loja.api;

import java.util.List;

/** Dados da compra, como o site envia. Textos livres são validados pelo checkout, na ordem combinada. */
public record PedidoRequest(
        Cliente cliente,
        List<Item> itens,
        String cep,
        String regiao,
        String modalidadeEntrega,
        String cupom,
        String formaPagamento,
        Integer parcelas,
        String nivelClube) {

    public record Cliente(String cpf, String email) {
    }

    public record Item(String sku, Integer quantidade) {
    }
}

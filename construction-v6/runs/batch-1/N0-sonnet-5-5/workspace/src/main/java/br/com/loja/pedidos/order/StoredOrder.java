package br.com.loja.pedidos.order;

/** An order together with the data that is not part of its public representation. */
public record StoredOrder(String cpf, Order order) {
}

package br.com.loja.checkout;

/** Dados da compra como o site enviou; códigos ainda não conferidos. */
public record Pedido(
        Carrinho carrinho,
        String modalidadeEntrega,
        String cupom,
        String formaPagamento,
        int parcelas,
        String nivelClube,
        String regiao) {
}

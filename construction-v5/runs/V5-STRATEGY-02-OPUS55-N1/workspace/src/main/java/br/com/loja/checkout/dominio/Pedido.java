package br.com.loja.checkout.dominio;

/** Dados da compra como chegaram do site; os códigos ainda não foram validados. */
public record Pedido(
        Carrinho carrinho,
        String modalidadeEntrega,
        String cupom,
        String formaPagamento,
        int parcelas,
        String nivelClube,
        String regiao) {
}

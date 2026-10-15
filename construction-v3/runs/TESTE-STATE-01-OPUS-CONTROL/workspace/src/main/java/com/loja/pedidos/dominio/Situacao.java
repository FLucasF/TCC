package com.loja.pedidos.dominio;

/**
 * Situacoes pelas quais um pedido pode passar.
 *
 * <p>A unica coisa que varia de uma situacao para outra e o texto mostrado ao cliente,
 * por isso a descricao fica aqui. As regras de o que pode acontecer em cada situacao
 * ficam em {@link FluxoPedido}.
 */
public enum Situacao {

    AGUARDANDO_PAGAMENTO("Aguardando pagamento"),
    PAGO("Pagamento confirmado"),
    EM_SEPARACAO("Separando seus produtos"),
    ENVIADO("A caminho"),
    ENTREGUE("Entregue"),
    CANCELADO("Cancelado"),
    DEVOLVIDO("Devolvido");

    private final String descricao;

    Situacao(String descricao) {
        this.descricao = descricao;
    }

    public String getDescricao() {
        return descricao;
    }
}

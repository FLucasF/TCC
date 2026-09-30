package com.loja.pedidos;

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

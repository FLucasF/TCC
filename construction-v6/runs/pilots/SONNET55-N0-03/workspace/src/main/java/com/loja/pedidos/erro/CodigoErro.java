package com.loja.pedidos.erro;

public enum CodigoErro {
    PEDIDO_INVALIDO(422),
    PRODUTO_INEXISTENTE(422),
    PRODUTO_FORA_DE_LINHA(422),
    ESTOQUE_INSUFICIENTE(422),
    NIVEL_CLUBE_INVALIDO(422),
    REGIAO_INVALIDA(422),
    CEP_INVALIDO(422),
    MODALIDADE_INVALIDA(422),
    MODALIDADE_INDISPONIVEL(422),
    TRANSPORTADORA_INDISPONIVEL(503),
    CUPOM_INVALIDO(422),
    CUPOM_NAO_APLICAVEL(422),
    FORMA_PAGAMENTO_INVALIDA(422),
    PARCELAMENTO_INVALIDO(422),
    FORMA_PAGAMENTO_INDISPONIVEL(422),
    PEDIDO_NAO_ENCONTRADO(404),
    OPERACAO_NAO_PERMITIDA(409),
    PEDIDO_EXPIRADO(409),
    ESTOQUE_INSUFICIENTE_PAGAMENTO(409),
    PRODUTO_NAO_ENCONTRADO(404),
    CLIENTE_NAO_ENCONTRADO(404),
    AVISO_NAO_ENCONTRADO(404);

    private final int status;

    CodigoErro(int status) {
        this.status = status;
    }

    public int status() {
        return status;
    }

    /** Código exposto ao cliente (o de estoque no pagamento tem o mesmo nome do de estoque no checkout). */
    public String codigoPublico() {
        return this == ESTOQUE_INSUFICIENTE_PAGAMENTO ? "ESTOQUE_INSUFICIENTE" : name();
    }
}

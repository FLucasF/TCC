package loja.dominio;

public record Aviso(long id, String tipo, String pedidoId, String situacao) {

    public static final String PAGAMENTO_APROVADO = "PAGAMENTO_APROVADO";
    public static final String PEDIDO_ENVIADO = "PEDIDO_ENVIADO";
    public static final String PEDIDO_CANCELADO = "PEDIDO_CANCELADO";
    public static final String NOVO = "NOVO";
    public static final String LIDO = "LIDO";
}

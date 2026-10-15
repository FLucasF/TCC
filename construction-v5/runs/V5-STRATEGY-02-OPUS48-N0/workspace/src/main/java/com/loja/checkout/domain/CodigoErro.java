package com.loja.checkout.domain;

/** Codigos de erro que o servico pode devolver, na ordem em que sao conferidos. */
public final class CodigoErro {

    public static final String PEDIDO_INVALIDO = "PEDIDO_INVALIDO";
    public static final String NIVEL_CLUBE_INVALIDO = "NIVEL_CLUBE_INVALIDO";
    public static final String REGIAO_INVALIDA = "REGIAO_INVALIDA";
    public static final String MODALIDADE_INVALIDA = "MODALIDADE_INVALIDA";
    public static final String MODALIDADE_INDISPONIVEL = "MODALIDADE_INDISPONIVEL";
    public static final String CUPOM_INVALIDO = "CUPOM_INVALIDO";
    public static final String CUPOM_NAO_APLICAVEL = "CUPOM_NAO_APLICAVEL";
    public static final String FORMA_PAGAMENTO_INVALIDA = "FORMA_PAGAMENTO_INVALIDA";
    public static final String PARCELAMENTO_INVALIDO = "PARCELAMENTO_INVALIDO";
    public static final String FORMA_PAGAMENTO_INDISPONIVEL = "FORMA_PAGAMENTO_INDISPONIVEL";

    private CodigoErro() {
    }
}

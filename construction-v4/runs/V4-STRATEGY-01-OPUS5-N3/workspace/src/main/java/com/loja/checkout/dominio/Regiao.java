package com.loja.checkout.dominio;

import java.math.BigDecimal;
import java.util.List;

/**
 * Regiao do cliente. Entre as regioes muda apenas a aliquota do seguro: a
 * conta e a mesma em todas, por isso aqui basta o numero.
 */
public enum Regiao {

    SUDESTE("0.01"),
    SUL("0.01"),
    CENTRO_OESTE("0.015"),
    NORTE("0.025"),
    NORDESTE("0.02");

    private static final Catalogo<Regiao> CATALOGO = Catalogo.de(List.of(values()), Enum::name);

    private final BigDecimal aliquotaSeguro;

    Regiao(String aliquotaSeguro) {
        this.aliquotaSeguro = new BigDecimal(aliquotaSeguro);
    }

    public static Regiao exigir(String codigo) {
        return CATALOGO.buscar(codigo)
                .orElseThrow(() -> new PedidoRecusadoException(Erro.REGIAO_INVALIDA));
    }

    /** Seguro do envio: aliquota sobre os produtos, sem desconto e sem frete. */
    public BigDecimal seguro(BigDecimal subtotalProdutos) {
        return Dinheiro.percentual(subtotalProdutos, aliquotaSeguro);
    }
}

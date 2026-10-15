package com.loja.checkout.dominio;

import java.math.BigDecimal;

/**
 * Região do cliente. No seguro a conta é a mesma em todas (alíquota sobre o
 * valor dos produtos); só a alíquota muda, por isso aqui é só o número.
 */
public enum Regiao {

    SUDESTE("0.01"),
    SUL("0.01"),
    CENTRO_OESTE("0.015"),
    NORTE("0.025"),
    NORDESTE("0.02");

    private final BigDecimal aliquotaSeguro;

    Regiao(String aliquotaSeguro) {
        this.aliquotaSeguro = new BigDecimal(aliquotaSeguro);
    }

    public BigDecimal seguroSobre(BigDecimal subtotalProdutos) {
        return Dinheiro.percentual(subtotalProdutos, aliquotaSeguro);
    }

    public static Regiao doCodigo(String codigo) {
        for (Regiao regiao : values()) {
            if (regiao.name().equals(codigo)) {
                return regiao;
            }
        }
        throw new PedidoRecusadoException(CodigoErro.REGIAO_INVALIDA);
    }
}

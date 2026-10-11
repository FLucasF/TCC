package com.loja.checkout.domain;

import java.math.BigDecimal;
import java.util.List;

/**
 * Regiao do cliente. O seguro contra extravio e roubo e cobrado por regiao:
 * e sempre a mesma conta (a porcentagem sobre o valor dos produtos, sem
 * desconto e sem frete), so muda a porcentagem.
 *
 * Para adicionar uma regiao nova basta acrescentar uma constante aqui com a
 * sua aliquota.
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

    /** Seguro = aliquota x subtotal dos produtos, arredondado para centavos. */
    public BigDecimal calcularSeguro(BigDecimal subtotalProdutos) {
        return Money.cents(subtotalProdutos.multiply(aliquotaSeguro));
    }

    public static Regiao fromCodigo(String codigo) {
        if (codigo == null) {
            return null;
        }
        for (Regiao r : values()) {
            if (r.name().equals(codigo)) {
                return r;
            }
        }
        return null;
    }

    public static List<Regiao> todas() {
        return List.of(values());
    }
}

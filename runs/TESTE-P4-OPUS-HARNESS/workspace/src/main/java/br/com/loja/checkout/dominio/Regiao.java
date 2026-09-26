package br.com.loja.checkout.dominio;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.Optional;

/**
 * Imposto da regiao do cliente. De uma regiao para outra muda so a aliquota,
 * por isso a conta fica num lugar so.
 */
public enum Regiao {

    SUDESTE("0.12"),
    SUL("0.11"),
    CENTRO_OESTE("0.09"),
    NORTE("0.07"),
    NORDESTE("0.07");

    private final BigDecimal aliquota;

    Regiao(String aliquota) {
        this.aliquota = new BigDecimal(aliquota);
    }

    public static Optional<Regiao> porNome(String nome) {
        return Arrays.stream(values()).filter(regiao -> regiao.name().equals(nome)).findFirst();
    }

    /** Aliquota sobre os produtos ja com o desconto do cupom. */
    public BigDecimal imposto(BigDecimal produtosComDesconto) {
        return Dinheiro.percentual(produtosComDesconto, aliquota);
    }
}

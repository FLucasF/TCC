package br.com.loja.checkout.seguro;

import br.com.loja.checkout.dominio.Dinheiro;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.Optional;

/** Região do cliente. Só a porcentagem do seguro muda entre as regiões. */
public enum Regiao {
    SUDESTE("1"),
    SUL("1"),
    CENTRO_OESTE("1.5"),
    NORTE("2.5"),
    NORDESTE("2");

    private final BigDecimal percentualSeguro;

    Regiao(String percentualSeguro) {
        this.percentualSeguro = new BigDecimal(percentualSeguro);
    }

    public BigDecimal seguro(BigDecimal subtotalProdutos) {
        return Dinheiro.percentual(subtotalProdutos, percentualSeguro);
    }

    public static Optional<Regiao> buscar(String codigo) {
        return Arrays.stream(values()).filter(regiao -> regiao.name().equals(codigo)).findFirst();
    }
}

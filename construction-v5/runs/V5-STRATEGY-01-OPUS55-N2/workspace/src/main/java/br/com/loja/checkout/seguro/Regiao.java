package br.com.loja.checkout.seguro;

import br.com.loja.checkout.resumo.Dinheiro;
import java.math.BigDecimal;
import java.util.Arrays;
import java.util.Optional;

/** Só a porcentagem do seguro muda por região; a conta é a mesma. */
public enum Regiao {
    SUDESTE("1"),
    SUL("1"),
    CENTRO_OESTE("1.5"),
    NORTE("2.5"),
    NORDESTE("2");

    private final String percentualSeguro;

    Regiao(String percentualSeguro) {
        this.percentualSeguro = percentualSeguro;
    }

    public BigDecimal seguro(BigDecimal subtotalProdutos) {
        return Dinheiro.percentual(subtotalProdutos, percentualSeguro);
    }

    public static Optional<Regiao> buscar(String codigo) {
        return Arrays.stream(values()).filter(regiao -> regiao.name().equals(codigo)).findFirst();
    }
}

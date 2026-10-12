package br.com.loja.checkout;

import java.math.BigDecimal;
import java.util.Arrays;

/** Regiões atendidas; só a porcentagem do seguro muda entre elas. */
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

    public static Regiao obter(String codigo) {
        return Arrays.stream(values())
                .filter(regiao -> regiao.name().equals(codigo))
                .findFirst()
                .orElseThrow(() -> new PedidoRecusadoException("REGIAO_INVALIDA"));
    }
}

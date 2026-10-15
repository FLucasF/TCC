package br.com.loja.checkout.regiao;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.Optional;

/** Regiões atendidas e a porcentagem que a seguradora cobra em cada uma. */
public enum Regiao {
    SUDESTE("1"),
    SUL("1"),
    CENTRO_OESTE("1.5"),
    NORTE("2.5"),
    NORDESTE("2");

    private final BigDecimal porcentagemSeguro;

    Regiao(String porcentagemSeguro) {
        this.porcentagemSeguro = new BigDecimal(porcentagemSeguro);
    }

    public BigDecimal porcentagemSeguro() {
        return porcentagemSeguro;
    }

    public static Optional<Regiao> buscar(String codigo) {
        return Arrays.stream(values()).filter(r -> r.name().equals(codigo)).findFirst();
    }
}

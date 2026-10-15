package com.loja.checkout.regiao;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Região do cliente. O seguro é a mesma conta em todas: a porcentagem sobre o
 * valor dos produtos. Só a porcentagem muda de região para região, então ela é
 * apenas um dado — a conta mora num lugar só (ver {@link #seguro}).
 */
public enum Regiao {

    SUDESTE("0.01"),
    SUL("0.01"),
    CENTRO_OESTE("0.015"),
    NORTE("0.025"),
    NORDESTE("0.02");

    private static final Map<String, Regiao> POR_NOME =
            Arrays.stream(values()).collect(Collectors.toMap(Enum::name, Function.identity()));

    private final BigDecimal percentual;

    Regiao(String percentual) {
        this.percentual = new BigDecimal(percentual);
    }

    public static Optional<Regiao> porNome(String nome) {
        return Optional.ofNullable(nome).map(POR_NOME::get);
    }

    /** Seguro do envio: a porcentagem da região sobre o valor dos produtos (sem desconto, sem frete). */
    public BigDecimal seguro(BigDecimal subtotalProdutos) {
        return subtotalProdutos.multiply(percentual);
    }
}

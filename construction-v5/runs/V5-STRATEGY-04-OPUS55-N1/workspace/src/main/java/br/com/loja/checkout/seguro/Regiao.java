package br.com.loja.checkout.seguro;

import br.com.loja.checkout.Dinheiro;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.Optional;

/** Região do cliente. Só a porcentagem do seguro muda; a conta é a mesma em todas. */
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

    /** Seguro contra extravio e roubo, sobre o valor dos produtos (sem desconto e sem frete). */
    public BigDecimal seguro(BigDecimal subtotalProdutos) {
        return Dinheiro.percentual(subtotalProdutos, porcentagemSeguro);
    }

    public static Optional<Regiao> buscar(String codigo) {
        return Arrays.stream(values()).filter(regiao -> regiao.name().equals(codigo)).findFirst();
    }
}

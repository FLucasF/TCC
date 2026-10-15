package br.com.loja.pedidos.checkout;

import br.com.loja.pedidos.comum.Dinheiro;
import java.math.BigDecimal;
import java.util.Arrays;
import java.util.Optional;

/** Região do cliente: define o seguro do envio e os dias a mais de despacho do centro de distribuição. */
public enum Regiao {
    SUDESTE("1", null, 0),
    SUL("1", null, 0),
    CENTRO_OESTE("1.5", null, 0),
    NORTE("2.5", "30.00", 3),
    NORDESTE("2", "30.00", 2);

    private final BigDecimal percentualSeguro;
    private final BigDecimal limiteSeguro;
    private final int diasAdicionaisDespacho;

    Regiao(String percentualSeguro, String limiteSeguro, int diasAdicionaisDespacho) {
        this.percentualSeguro = new BigDecimal(percentualSeguro);
        this.limiteSeguro = limiteSeguro == null ? null : Dinheiro.reais(limiteSeguro);
        this.diasAdicionaisDespacho = diasAdicionaisDespacho;
    }

    public static Optional<Regiao> de(String codigo) {
        return Arrays.stream(values()).filter(r -> r.name().equals(codigo)).findFirst();
    }

    /** Seguro contra extravio e roubo, sobre o valor dos produtos (sem desconto e sem frete). */
    public BigDecimal seguro(BigDecimal subtotalProdutos) {
        BigDecimal seguro = Dinheiro.percentual(subtotalProdutos, percentualSeguro);
        return limiteSeguro == null ? seguro : Dinheiro.menor(seguro, limiteSeguro);
    }

    /** Dias a mais que o centro de distribuição leva para despachar envios por transportadora. */
    public int diasAdicionaisDespacho() {
        return diasAdicionaisDespacho;
    }
}

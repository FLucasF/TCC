package br.com.loja.pedidos.checkout;

import java.math.BigDecimal;
import java.util.Optional;

import br.com.loja.pedidos.comum.Dinheiro;

/** Região do cliente: define o seguro e os dias a mais do centro de distribuição. */
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

    public BigDecimal seguro(BigDecimal subtotalProdutos) {
        BigDecimal seguro = Dinheiro.porcentagem(subtotalProdutos, percentualSeguro);
        if (limiteSeguro != null && seguro.compareTo(limiteSeguro) > 0) {
            return limiteSeguro;
        }
        return seguro;
    }

    /** Dias a mais para despachar pelas transportadoras. */
    public int diasAdicionaisDespacho() {
        return diasAdicionaisDespacho;
    }

    public static Optional<Regiao> deCodigo(String codigo) {
        for (Regiao regiao : values()) {
            if (regiao.name().equals(codigo)) {
                return Optional.of(regiao);
            }
        }
        return Optional.empty();
    }
}

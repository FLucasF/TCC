package loja.dominio;

import java.math.BigDecimal;
import java.util.Optional;

public enum Regiao {
    SUDESTE("0.01", null, 0),
    SUL("0.01", null, 0),
    CENTRO_OESTE("0.015", null, 0),
    NORTE("0.025", "30.00", 3),
    NORDESTE("0.02", "30.00", 2);

    private final BigDecimal taxaSeguro;
    private final BigDecimal limiteSeguro;
    private final int diasExtraTransportadora;

    Regiao(String taxaSeguro, String limiteSeguro, int diasExtraTransportadora) {
        this.taxaSeguro = new BigDecimal(taxaSeguro);
        this.limiteSeguro = limiteSeguro == null ? null : new BigDecimal(limiteSeguro);
        this.diasExtraTransportadora = diasExtraTransportadora;
    }

    /** Seguro sobre o valor dos produtos (sem desconto e sem frete), respeitando o limite da região. */
    public BigDecimal seguro(BigDecimal subtotalProdutos) {
        BigDecimal valor = Dinheiro.arredondar(subtotalProdutos.multiply(taxaSeguro));
        if (limiteSeguro != null && valor.compareTo(limiteSeguro) > 0) {
            return Dinheiro.arredondar(limiteSeguro);
        }
        return valor;
    }

    /** Dias a mais de despacho para as opções de transportadora. */
    public int diasExtraTransportadora() {
        return diasExtraTransportadora;
    }

    public static Optional<Regiao> de(String texto) {
        if (texto == null) {
            return Optional.empty();
        }
        try {
            return Optional.of(valueOf(texto));
        } catch (IllegalArgumentException e) {
            return Optional.empty();
        }
    }
}

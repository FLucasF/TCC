package br.tcc.checkout.model;

import java.math.BigDecimal;

public class Cupom {
    private final String codigo;
    private final String tipo;
    private final BigDecimal valor;
    private final BigDecimal minimoSubtotal;

    public static final Cupom BEMVINDO10 = new Cupom("BEMVINDO10", "PERCENTUAL", new BigDecimal("0.10"), BigDecimal.ZERO);
    public static final Cupom MENOS50 = new Cupom("MENOS50", "FIXO", new BigDecimal("50.00"), new BigDecimal("300.00"));
    public static final Cupom FRETEGRATIS = new Cupom("FRETEGRATIS", "FRETE_GRATIS", BigDecimal.ZERO, BigDecimal.ZERO);
    public static final Cupom LEVE3PAGUE2 = new Cupom("LEVE3PAGUE2", "LEVE3PAGUE2", BigDecimal.ZERO, BigDecimal.ZERO);

    public Cupom(String codigo, String tipo, BigDecimal valor, BigDecimal minimoSubtotal) {
        this.codigo = codigo;
        this.tipo = tipo;
        this.valor = valor;
        this.minimoSubtotal = minimoSubtotal;
    }

    public String getCodigo() {
        return codigo;
    }

    public String getTipo() {
        return tipo;
    }

    public BigDecimal getValor() {
        return valor;
    }

    public BigDecimal getMinimoSubtotal() {
        return minimoSubtotal;
    }

    public static Cupom porCodigo(String codigo) {
        if (codigo == null) {
            return null;
        }
        return switch (codigo) {
            case "BEMVINDO10" -> BEMVINDO10;
            case "MENOS50" -> MENOS50;
            case "FRETEGRATIS" -> FRETEGRATIS;
            case "LEVE3PAGUE2" -> LEVE3PAGUE2;
            default -> null;
        };
    }
}

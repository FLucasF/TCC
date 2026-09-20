package br.tcc.checkout.model;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;

public class Cupom {
    private static final Map<String, Cupom> CUPONS = new HashMap<>();

    static {
        CUPONS.put("BEMVINDO10", new Cupom("BEMVINDO10", TipoCupom.PERCENTUAL, new BigDecimal("0.10"), null));
        CUPONS.put("MENOS50", new Cupom("MENOS50", TipoCupom.FIXO, new BigDecimal("50.00"), new BigDecimal("300.00")));
        CUPONS.put("FRETEGRATIS", new Cupom("FRETEGRATIS", TipoCupom.FRETE_GRATIS, null, null));
        CUPONS.put("LEVE3PAGUE2", new Cupom("LEVE3PAGUE2", TipoCupom.LEVE3PAGUE2, null, null));
    }

    private String codigo;
    private TipoCupom tipo;
    private BigDecimal valor;
    private BigDecimal minimo;

    public Cupom(String codigo, TipoCupom tipo, BigDecimal valor, BigDecimal minimo) {
        this.codigo = codigo;
        this.tipo = tipo;
        this.valor = valor;
        this.minimo = minimo;
    }

    public static Cupom obter(String codigo) {
        return CUPONS.get(codigo);
    }

    public String getCodigo() {
        return codigo;
    }

    public TipoCupom getTipo() {
        return tipo;
    }

    public BigDecimal getValor() {
        return valor;
    }

    public BigDecimal getMinimo() {
        return minimo;
    }

    public enum TipoCupom {
        PERCENTUAL,
        FIXO,
        FRETE_GRATIS,
        LEVE3PAGUE2
    }
}

package com.loja.model.cupom;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

public class CupomRegistry {
    private static final Map<String, CalculadoraDesconto> CUPONS = new HashMap<>();
    private static final Map<String, DescontoFixo> CUPONS_FIXOS = new HashMap<>();

    static {
        CUPONS.put("BEMVINDO10", new DescontoPercentual(new BigDecimal("0.10")));
        CUPONS.put("FRETEGRATIS", new FreteGratis());
        CUPONS.put("LEVE3PAGUE2", new Leve3Pague2());

        DescontoFixo menos50 = new DescontoFixo(new BigDecimal("50.00"), new BigDecimal("300.00"));
        CUPONS.put("MENOS50", menos50);
        CUPONS_FIXOS.put("MENOS50", menos50);
    }

    public static Optional<CalculadoraDesconto> obter(String codigo) {
        return Optional.ofNullable(CUPONS.get(codigo));
    }

    public static boolean temValorMinimo(String codigo) {
        return CUPONS_FIXOS.containsKey(codigo);
    }

    public static boolean podeAplicar(String codigo, BigDecimal subtotal) {
        DescontoFixo cupom = CUPONS_FIXOS.get(codigo);
        if (cupom != null) {
            return cupom.podeAplicar(subtotal);
        }
        return true;
    }
}

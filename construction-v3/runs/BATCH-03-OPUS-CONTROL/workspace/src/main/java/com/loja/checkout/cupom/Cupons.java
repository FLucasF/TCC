package com.loja.checkout.cupom;

import com.loja.checkout.dominio.CheckoutException;
import com.loja.checkout.dominio.ErroCheckout;
import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/** Catalogo das promocoes que valem hoje. */
@Component
public class Cupons {

    private final Map<String, Cupom> porCodigo;

    public Cupons(List<Cupom> cupons) {
        this.porCodigo = cupons.stream().collect(Collectors.toMap(
                Cupom::codigo, Function.identity(), (a, b) -> a, LinkedHashMap::new));
    }

    public Cupom buscar(String codigo) {
        Cupom cupom = porCodigo.get(codigo);
        if (cupom == null) {
            throw new CheckoutException(ErroCheckout.CUPOM_INVALIDO);
        }
        return cupom;
    }
}

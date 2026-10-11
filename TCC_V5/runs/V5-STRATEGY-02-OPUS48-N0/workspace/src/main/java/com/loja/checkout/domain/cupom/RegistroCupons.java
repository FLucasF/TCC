package com.loja.checkout.domain.cupom;

import com.loja.checkout.domain.CheckoutException;
import com.loja.checkout.domain.CodigoErro;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.stereotype.Component;

/**
 * Reune todos os cupons que valem hoje. Qualquer cupom novo marcado com
 * {@code @Component} entra aqui automaticamente.
 */
@Component
public class RegistroCupons {

    private final Map<String, Cupom> porCodigo;

    public RegistroCupons(List<Cupom> cupons) {
        this.porCodigo = cupons.stream()
                .collect(Collectors.toMap(Cupom::codigo, Function.identity()));
    }

    /** Busca pelo codigo do cupom; lanca CUPOM_INVALIDO se nao existir. */
    public Cupom buscar(String codigo) {
        Cupom cupom = codigo == null ? null : porCodigo.get(codigo);
        if (cupom == null) {
            throw new CheckoutException(CodigoErro.CUPOM_INVALIDO);
        }
        return cupom;
    }
}

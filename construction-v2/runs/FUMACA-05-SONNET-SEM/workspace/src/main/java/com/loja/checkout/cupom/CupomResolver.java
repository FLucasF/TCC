package com.loja.checkout.cupom;

import com.loja.checkout.exception.CheckoutException;
import com.loja.checkout.exception.ErroCodigo;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Component
public class CupomResolver {

    private final Map<String, Cupom> porCodigo;

    public CupomResolver(List<Cupom> cupons) {
        this.porCodigo = cupons.stream()
                .collect(Collectors.toMap(Cupom::getCodigo, Function.identity()));
    }

    public Cupom resolver(String codigo) {
        if (codigo == null || !porCodigo.containsKey(codigo)) {
            throw new CheckoutException(ErroCodigo.CUPOM_INVALIDO);
        }
        return porCodigo.get(codigo);
    }
}

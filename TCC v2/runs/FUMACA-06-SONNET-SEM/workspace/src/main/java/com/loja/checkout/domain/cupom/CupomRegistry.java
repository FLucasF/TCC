package com.loja.checkout.domain.cupom;

import com.loja.checkout.exception.CheckoutException;
import com.loja.checkout.exception.CodigoErro;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Component
public class CupomRegistry {

    private final Map<String, Cupom> cuponsPorCodigo;

    public CupomRegistry(List<Cupom> cupons) {
        this.cuponsPorCodigo = cupons.stream()
                .collect(Collectors.toMap(Cupom::codigo, Function.identity()));
    }

    public Cupom buscar(String codigo) {
        if (!cuponsPorCodigo.containsKey(codigo)) {
            throw new CheckoutException(CodigoErro.CUPOM_INVALIDO);
        }
        return cuponsPorCodigo.get(codigo);
    }
}

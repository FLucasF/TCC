package com.loja.checkout.domain.clube;

import com.loja.checkout.domain.erro.CheckoutException;
import com.loja.checkout.domain.erro.ErroCheckout;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/** Acha o nivel pelo codigo; nao achou ou nao informado -> NIVEL_CLUBE_INVALIDO. */
@Component
public class ClubeResolver {

    private final Map<String, NivelClube> porCodigo;

    public ClubeResolver(List<NivelClube> niveis) {
        this.porCodigo = niveis.stream()
                .collect(Collectors.toMap(NivelClube::codigo, Function.identity()));
    }

    public NivelClube resolver(String codigo) {
        NivelClube nivel = codigo == null ? null : porCodigo.get(codigo);
        if (nivel == null) {
            throw new CheckoutException(ErroCheckout.NIVEL_CLUBE_INVALIDO);
        }
        return nivel;
    }
}

package com.loja.checkout.cupom;

import com.loja.checkout.exception.CheckoutException;
import com.loja.checkout.exception.CodigoErro;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class CupomService {

    private final Map<String, Cupom> cuponsPorCodigo;

    public CupomService(List<Cupom> cupons) {
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

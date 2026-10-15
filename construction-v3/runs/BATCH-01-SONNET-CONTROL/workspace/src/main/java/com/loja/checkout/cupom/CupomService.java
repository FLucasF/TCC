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

    private final Map<String, CupomEstrategia> estrategiasPorCodigo;

    public CupomService(List<CupomEstrategia> estrategias) {
        this.estrategiasPorCodigo = estrategias.stream()
                .collect(Collectors.toMap(CupomEstrategia::getCodigo, Function.identity()));
    }

    public CupomEstrategia buscarEstrategia(String codigo) {
        CupomEstrategia estrategia = estrategiasPorCodigo.get(codigo);
        if (estrategia == null) {
            throw new CheckoutException(CodigoErro.CUPOM_INVALIDO);
        }
        return estrategia;
    }
}

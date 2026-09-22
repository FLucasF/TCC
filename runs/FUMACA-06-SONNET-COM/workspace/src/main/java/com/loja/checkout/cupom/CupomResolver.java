package com.loja.checkout.cupom;

import com.loja.checkout.erro.RegraNegocioException;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.stereotype.Component;

@Component
public class CupomResolver {

    private final Map<String, Cupom> cuponsPorCodigo;

    public CupomResolver(List<Cupom> cupons) {
        this.cuponsPorCodigo = cupons.stream().collect(Collectors.toMap(Cupom::codigo, Function.identity()));
    }

    public Cupom resolver(String codigo) {
        Cupom cupom = cuponsPorCodigo.get(codigo);
        if (cupom == null) {
            throw new RegraNegocioException("CUPOM_INVALIDO");
        }
        return cupom;
    }
}

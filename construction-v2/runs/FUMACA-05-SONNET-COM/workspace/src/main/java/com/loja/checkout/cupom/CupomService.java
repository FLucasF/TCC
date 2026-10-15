package com.loja.checkout.cupom;

import com.loja.checkout.dominio.Dinheiro;
import com.loja.checkout.erro.CheckoutException;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
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

    public BigDecimal calcularDesconto(String codigo, ContextoCupom contexto) {
        if (codigo == null) {
            return BigDecimal.ZERO;
        }
        Cupom cupom = cuponsPorCodigo.get(codigo);
        if (cupom == null) {
            throw new CheckoutException("CUPOM_INVALIDO");
        }
        if (!cupom.aplicavel(contexto)) {
            throw new CheckoutException("CUPOM_NAO_APLICAVEL");
        }
        return Dinheiro.arredondar(cupom.calcularDesconto(contexto));
    }
}

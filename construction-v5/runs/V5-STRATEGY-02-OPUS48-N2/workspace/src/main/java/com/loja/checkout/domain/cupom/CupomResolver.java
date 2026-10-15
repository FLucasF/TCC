package com.loja.checkout.domain.cupom;

import com.loja.checkout.domain.erro.CheckoutException;
import com.loja.checkout.domain.erro.ErroCheckout;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Acha o cupom pelo codigo. Sem codigo -> pedido sem cupom; codigo que nao existe
 * -> CUPOM_INVALIDO.
 */
@Component
public class CupomResolver {

    private static final Cupom SEM_CUPOM = new SemCupom();

    private final Map<String, Cupom> porCodigo;

    public CupomResolver(List<Cupom> cupons) {
        this.porCodigo = cupons.stream()
                .collect(Collectors.toMap(Cupom::codigo, Function.identity()));
    }

    public Cupom resolver(String codigo) {
        if (codigo == null || codigo.isBlank()) {
            return SEM_CUPOM;
        }
        Cupom cupom = porCodigo.get(codigo);
        if (cupom == null) {
            throw new CheckoutException(ErroCheckout.CUPOM_INVALIDO);
        }
        return cupom;
    }
}

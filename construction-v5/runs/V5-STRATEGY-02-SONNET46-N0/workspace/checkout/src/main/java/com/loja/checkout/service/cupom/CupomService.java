package com.loja.checkout.service.cupom;

import com.loja.checkout.dto.ItemDto;
import com.loja.checkout.model.CheckoutException;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class CupomService {

    private final Map<String, Cupom> cupons;
    private final CupomLeve3Pague2 leve3Pague2;

    public CupomService(List<Cupom> cupons, CupomLeve3Pague2 leve3Pague2) {
        this.cupons = cupons.stream()
                .collect(Collectors.toMap(Cupom::getCodigo, Function.identity()));
        this.leve3Pague2 = leve3Pague2;
    }

    public BigDecimal calcularDesconto(String codigoCupom, BigDecimal subtotalProdutos,
                                       BigDecimal frete, List<ItemDto> itens) {
        if (codigoCupom == null || codigoCupom.isBlank()) {
            return BigDecimal.ZERO.setScale(2);
        }
        if (!cupons.containsKey(codigoCupom)) {
            throw new CheckoutException("CUPOM_INVALIDO");
        }
        Cupom cupom = cupons.get(codigoCupom);
        if (!cupom.aplicavel(subtotalProdutos)) {
            throw new CheckoutException("CUPOM_NAO_APLICAVEL");
        }
        if (cupom instanceof CupomLeve3Pague2 c) {
            c.setItens(itens);
        }
        return cupom.calcularDesconto(subtotalProdutos, frete);
    }
}

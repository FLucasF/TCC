package com.loja.checkout.estrategia;

import java.util.List;
import com.loja.checkout.dto.ItemRequest;

public interface EstrategiaCupom {
    double calcularDesconto(double subtotalProdutos, double frete, List<ItemRequest> itens);
    boolean aplicavel(double subtotalProdutos, List<ItemRequest> itens);
    double ajustarFretePorCupom(double frete);
    List<ItemRequest> ajustarItensPorCupom(List<ItemRequest> itens);
}

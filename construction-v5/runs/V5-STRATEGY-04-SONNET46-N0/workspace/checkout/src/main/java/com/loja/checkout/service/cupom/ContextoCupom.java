package com.loja.checkout.service.cupom;

import com.loja.checkout.dto.ItemRequest;
import java.util.List;

public record ContextoCupom(List<ItemRequest> itens, double subtotalProdutos, double frete) {}

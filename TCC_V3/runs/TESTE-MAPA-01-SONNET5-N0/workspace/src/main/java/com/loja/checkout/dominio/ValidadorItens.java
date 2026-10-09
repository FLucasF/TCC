package com.loja.checkout.dominio;

import com.loja.checkout.api.CodigoErro;
import com.loja.checkout.api.ErroPedidoException;
import com.loja.checkout.api.dto.ItemRequest;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import org.springframework.stereotype.Component;

@Component
public class ValidadorItens {

    public List<Item> validar(List<ItemRequest> itensRequest) {
        if (itensRequest == null || itensRequest.isEmpty()) {
            throw new ErroPedidoException(CodigoErro.PEDIDO_INVALIDO);
        }

        List<Item> itens = new ArrayList<>();
        for (ItemRequest itemRequest : itensRequest) {
            if (itemRequest.precoUnitario() == null || itemRequest.precoUnitario().compareTo(BigDecimal.ZERO) <= 0
                    || itemRequest.quantidade() == null || itemRequest.quantidade() <= 0
                    || itemRequest.pesoKg() == null || itemRequest.pesoKg().compareTo(BigDecimal.ZERO) <= 0) {
                throw new ErroPedidoException(CodigoErro.PEDIDO_INVALIDO);
            }
            itens.add(new Item(itemRequest.nome(), itemRequest.precoUnitario(), itemRequest.quantidade(),
                    itemRequest.pesoKg()));
        }
        return itens;
    }
}

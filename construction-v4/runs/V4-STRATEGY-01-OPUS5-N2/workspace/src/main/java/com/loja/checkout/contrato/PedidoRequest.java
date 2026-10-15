package com.loja.checkout.contrato;

import java.util.List;

/**
 * Os dados da compra como o site envia. Os campos que identificam um caso
 * (entrega, cupom, pagamento, clube, regiao) chegam como texto porque um valor
 * desconhecido e' uma recusa com codigo proprio, nao um erro de formato.
 */
public record PedidoRequest(
        List<ItemRequest> itens,
        String modalidadeEntrega,
        String cupom,
        String formaPagamento,
        Integer parcelas,
        String nivelClube,
        String regiao) {
}

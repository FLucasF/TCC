package com.loja.checkout.api;

import java.util.List;

/**
 * Os dados da compra como o site envia. As opcoes chegam como texto para que
 * um codigo desconhecido seja recusado com o codigo de erro certo.
 */
public record ResumoRequest(List<ItemRequest> itens, String modalidadeEntrega, String cupom,
                            String formaPagamento, Integer parcelas, String nivelClube,
                            String regiao) {
}

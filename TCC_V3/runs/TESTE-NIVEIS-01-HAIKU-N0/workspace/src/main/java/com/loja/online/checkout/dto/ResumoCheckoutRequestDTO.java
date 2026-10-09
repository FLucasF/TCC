package com.loja.online.checkout.dto;

import com.loja.online.checkout.enums.FormaPagamento;
import com.loja.online.checkout.enums.ModalidadeEntrega;
import com.loja.online.checkout.enums.NivelClube;
import com.loja.online.checkout.enums.Regiao;
import java.util.List;

public record ResumoCheckoutRequestDTO(
    List<ItemCarrinhoDTO> itens,
    ModalidadeEntrega modalidadeEntrega,
    String cupom,
    FormaPagamento formaPagamento,
    Integer parcelas,
    NivelClube nivelClube,
    Regiao regiao
) {}

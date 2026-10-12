package com.loja.checkout.web.dto;

import java.util.List;

public record ResumoRequestDTO(
        List<ItemRequestDTO> itens,
        String modalidadeEntrega,
        String cupom,
        String formaPagamento,
        Integer parcelas,
        String nivelClube,
        String regiao) {
}

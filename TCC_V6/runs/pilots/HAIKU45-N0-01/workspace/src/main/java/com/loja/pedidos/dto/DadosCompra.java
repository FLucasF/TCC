package com.loja.pedidos.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DadosCompra {
    private Cliente cliente;
    private List<Item> itens;
    private String cep;
    private Regiao regiao;
    private ModalidadeEntrega modalidadeEntrega;
    private String cupom;
    private FormaPagamento formaPagamento;
    private Integer parcelas;
    private NivelClube nivelClube;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Cliente {
        private String cpf;
        private String email;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Item {
        private String sku;
        private Integer quantidade;
    }

    public enum Regiao {
        SUDESTE, SUL, CENTRO_OESTE, NORTE, NORDESTE
    }

    public enum ModalidadeEntrega {
        ECONOMICA, EXPRESSA, RETIRADA_LOJA, MOTOBOY
    }

    public enum FormaPagamento {
        PIX, CARTAO, BOLETO
    }

    public enum NivelClube {
        BRONZE, PRATA, OURO
    }
}

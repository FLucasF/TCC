package com.loja.checkout.service;

import com.loja.checkout.dto.CheckoutRequest;
import com.loja.checkout.dto.Item;
import com.loja.checkout.domain.NivelClube;
import com.loja.checkout.domain.Regiao;
import com.loja.checkout.domain.strategy.AjustePagamentoCalculadorFactory;
import com.loja.checkout.domain.strategy.CupomAplicadorFactory;
import com.loja.checkout.domain.strategy.FreteCalculadorFactory;

public class ValidadorCheckout {
    public String validar(CheckoutRequest request) {
        // 1. Validar itens
        if (!itemsValidos(request)) {
            return "PEDIDO_INVALIDO";
        }

        // 2. Validar nível do clube
        if (request.getNivelClube() == null || NivelClube.parse(request.getNivelClube()) == null) {
            return "NIVEL_CLUBE_INVALIDO";
        }

        // 3. Validar região
        if (request.getRegiao() == null || Regiao.parse(request.getRegiao()) == null) {
            return "REGIAO_INVALIDA";
        }

        // 4. Validar modalidade de entrega
        if (request.getModalidadeEntrega() == null || FreteCalculadorFactory.criar(request.getModalidadeEntrega()) == null) {
            return "MODALIDADE_INVALIDA";
        }

        // 5. Validar se modalidade de entrega é disponível para o pedido
        if (!FreteCalculadorFactory.criar(request.getModalidadeEntrega()).ehDisponivel(request.getItens())) {
            return "MODALIDADE_INDISPONIVEL";
        }

        // 6. Validar cupom
        if (request.getCupom() != null && CupomAplicadorFactory.criar(request.getCupom()) == null) {
            return "CUPOM_INVALIDO";
        }

        // 7. Validar se cupom é aplicável (será feito após calcular subtotal e frete)
        // Será feito no serviço principal

        // 8. Validar forma de pagamento
        if (request.getFormaPagamento() == null || AjustePagamentoCalculadorFactory.criar(request.getFormaPagamento()) == null) {
            return "FORMA_PAGAMENTO_INVALIDA";
        }

        // 9. Validar parcelas
        if (!parcelasValidas(request.getFormaPagamento(), request.getParcelas())) {
            return "PARCELAMENTO_INVALIDO";
        }

        // 10. Validar se forma de pagamento é disponível (será feito no serviço principal)

        return null;
    }

    private boolean itemsValidos(CheckoutRequest request) {
        if (request.getItens() == null || request.getItens().isEmpty()) {
            return false;
        }

        for (Item item : request.getItens()) {
            if (item.getPrecoUnitario() == null || item.getPrecoUnitario() <= 0) {
                return false;
            }
            if (item.getQuantidade() == null || item.getQuantidade() <= 0) {
                return false;
            }
            if (item.getPesoKg() == null || item.getPesoKg() <= 0) {
                return false;
            }
        }

        return true;
    }

    private boolean parcelasValidas(String formaPagamento, Integer parcelas) {
        if (parcelas == null || parcelas < 1) {
            return false;
        }

        return switch (formaPagamento) {
            case "PIX", "BOLETO" -> parcelas == 1;
            case "CARTAO" -> parcelas >= 1 && parcelas <= 12;
            default -> false;
        };
    }
}

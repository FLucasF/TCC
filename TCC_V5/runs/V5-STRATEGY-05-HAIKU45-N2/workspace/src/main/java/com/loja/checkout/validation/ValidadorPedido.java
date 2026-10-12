package com.loja.checkout.validation;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import com.loja.checkout.domain.Item;
import com.loja.checkout.domain.FormaPagamento;
import com.loja.checkout.domain.ModalidadeEntrega;
import com.loja.checkout.domain.NivelClube;
import com.loja.checkout.domain.Regiao;
import com.loja.checkout.domain.cupom.RegistroCupons;
import com.loja.checkout.domain.entrega.RegistroEntregas;
import com.loja.checkout.domain.pagamento.RegistroPagamentos;

public class ValidadorPedido {
    public Optional<String> validar(
            List<Item> itens,
            String modalidadeEntregaStr,
            String cupomStr,
            String formaPagamentoStr,
            Integer parcelas,
            String nivelClubeStr,
            String regiao) {

        if (!validarItens(itens)) {
            return Optional.of("PEDIDO_INVALIDO");
        }

        try {
            NivelClube.valueOf(nivelClubeStr);
        } catch (IllegalArgumentException | NullPointerException e) {
            return Optional.of("NIVEL_CLUBE_INVALIDO");
        }

        try {
            Regiao.valueOf(regiao);
        } catch (IllegalArgumentException | NullPointerException e) {
            return Optional.of("REGIAO_INVALIDA");
        }

        ModalidadeEntrega modalidade;
        try {
            modalidade = ModalidadeEntrega.valueOf(modalidadeEntregaStr);
        } catch (IllegalArgumentException | NullPointerException e) {
            return Optional.of("MODALIDADE_INVALIDA");
        }

        if (!RegistroEntregas.obter(modalidade).isPresent()) {
            return Optional.of("MODALIDADE_INVALIDA");
        }

        if (cupomStr != null && !cupomStr.isEmpty()) {
            if (!RegistroCupons.existe(cupomStr)) {
                return Optional.of("CUPOM_INVALIDO");
            }
        }

        FormaPagamento formaPagamento;
        try {
            formaPagamento = FormaPagamento.valueOf(formaPagamentoStr);
        } catch (IllegalArgumentException | NullPointerException e) {
            return Optional.of("FORMA_PAGAMENTO_INVALIDA");
        }

        if (!RegistroPagamentos.obter(formaPagamento).isPresent()) {
            return Optional.of("FORMA_PAGAMENTO_INVALIDA");
        }

        int parcelasEfetivas = parcelas != null ? parcelas : 1;
        if (!RegistroPagamentos.obter(formaPagamento).get().disponivel(BigDecimal.ZERO, parcelasEfetivas)) {
            return Optional.of("PARCELAMENTO_INVALIDO");
        }

        return Optional.empty();
    }

    private boolean validarItens(List<Item> itens) {
        if (itens == null || itens.isEmpty()) {
            return false;
        }

        for (Item item : itens) {
            if (item.getNome() == null || item.getNome().isEmpty()) {
                return false;
            }
            if (item.getPrecoUnitario() == null || item.getPrecoUnitario().compareTo(BigDecimal.ZERO) <= 0) {
                return false;
            }
            if (item.getQuantidade() <= 0) {
                return false;
            }
            if (item.getPesoKg() == null || item.getPesoKg().compareTo(BigDecimal.ZERO) < 0) {
                return false;
            }
        }

        return true;
    }
}

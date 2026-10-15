package com.loja.service;

import com.loja.dto.CheckoutRequest;
import com.loja.dto.ItemRequest;
import com.loja.enums.Cupom;
import com.loja.enums.FormaPagamento;
import com.loja.enums.ModalidadeEntrega;
import com.loja.exception.CheckoutException;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.List;

@Service
public class ValidadorCheckout {

    public void validar(CheckoutRequest request) {
        validarItens(request.getItens());
        validarModalidade(request.getModalidadeEntrega());
        validarCupom(request.getCupom(), request.getItens());
        validarFormaPagamento(request.getFormaPagamento());
        validarParcelamento(request.getFormaPagamento(), request.getParcelas());
    }

    public void validarModalidadeDisponivel(ModalidadeEntrega modalidade, BigDecimal pesoTotal) {
        if (modalidade.isMotoboy() && pesoTotal.compareTo(new BigDecimal("5")) > 0) {
            throw new CheckoutException("MODALIDADE_INDISPONIVEL");
        }
    }

    public void validarFormaDisponivel(FormaPagamento forma, BigDecimal total) {
        if (forma == FormaPagamento.BOLETO && total.compareTo(new BigDecimal("1000.00")) > 0) {
            throw new CheckoutException("FORMA_PAGAMENTO_INDISPONIVEL");
        }
    }

    private void validarItens(List<ItemRequest> itens) {
        if (itens == null || itens.isEmpty()) {
            throw new CheckoutException("PEDIDO_INVALIDO");
        }

        for (ItemRequest item : itens) {
            if (item.getPrecoUnitario() == null ||
                item.getPrecoUnitario().compareTo(BigDecimal.ZERO) <= 0) {
                throw new CheckoutException("PEDIDO_INVALIDO");
            }

            if (item.getQuantidade() == null || item.getQuantidade() <= 0) {
                throw new CheckoutException("PEDIDO_INVALIDO");
            }

            if (item.getPesoKg() == null || item.getPesoKg().compareTo(BigDecimal.ZERO) <= 0) {
                throw new CheckoutException("PEDIDO_INVALIDO");
            }
        }
    }

    private void validarModalidade(String modalidade) {
        if (modalidade == null) {
            throw new CheckoutException("MODALIDADE_INVALIDA");
        }

        try {
            ModalidadeEntrega.valueOf(modalidade);
        } catch (IllegalArgumentException e) {
            throw new CheckoutException("MODALIDADE_INVALIDA");
        }
    }

    private void validarCupom(String cupom, List<ItemRequest> itens) {
        if (cupom == null) {
            return;
        }

        Cupom cupomEnum = Cupom.fromString(cupom);
        if (cupomEnum == null) {
            throw new CheckoutException("CUPOM_INVALIDO");
        }

        if (cupomEnum == Cupom.MENOS50) {
            BigDecimal subtotal = CalculadorSubtotal.calcular(itens);
            if (subtotal.compareTo(new BigDecimal("300.00")) < 0) {
                throw new CheckoutException("CUPOM_NAO_APLICAVEL");
            }
        }
    }

    private void validarFormaPagamento(String forma) {
        if (forma == null) {
            throw new CheckoutException("FORMA_PAGAMENTO_INVALIDA");
        }

        FormaPagamento formaPagamento = FormaPagamento.fromString(forma);
        if (formaPagamento == null) {
            throw new CheckoutException("FORMA_PAGAMENTO_INVALIDA");
        }
    }

    private void validarParcelamento(String forma, Integer parcelas) {
        FormaPagamento formaPagamento = FormaPagamento.fromString(forma);

        int numParcelas = parcelas != null ? parcelas : 1;

        if (formaPagamento == FormaPagamento.PIX || formaPagamento == FormaPagamento.BOLETO) {
            if (numParcelas != 1) {
                throw new CheckoutException("PARCELAMENTO_INVALIDO");
            }
        } else if (formaPagamento == FormaPagamento.CARTAO) {
            if (numParcelas < 1 || numParcelas > 12) {
                throw new CheckoutException("PARCELAMENTO_INVALIDO");
            }
        }
    }
}

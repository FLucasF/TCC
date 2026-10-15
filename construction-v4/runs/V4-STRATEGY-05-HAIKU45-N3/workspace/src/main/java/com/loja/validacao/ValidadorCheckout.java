package com.loja.validacao;

import com.loja.dto.CheckoutRequest;
import com.loja.dto.ItemRequest;
import com.loja.enums.Cupom;
import com.loja.enums.FormaPagamento;
import com.loja.enums.NivelClube;
import com.loja.enums.Regiao;
import com.loja.enums.TipoEntrega;
import java.math.BigDecimal;

public class ValidadorCheckout {

    public String validar(CheckoutRequest request) {
        // Ordem 1: Carrinho inválido
        String erroCarrinho = validarCarrinho(request.getItens());
        if (erroCarrinho != null) return erroCarrinho;

        // Ordem 2: Nível do clube
        if (NivelClube.fromString(request.getNivelClube()) == null) {
            return "NIVEL_CLUBE_INVALIDO";
        }

        // Ordem 3: Região
        if (Regiao.fromString(request.getRegiao()) == null) {
            return "REGIAO_INVALIDA";
        }

        // Ordem 4: Modalidade de entrega
        TipoEntrega tipo = TipoEntrega.fromString(request.getModalidadeEntrega());
        if (tipo == null) {
            return "MODALIDADE_INVALIDA";
        }

        // Ordem 5: Modalidade indisponível
        String erroModalidade = validarModalidadeDisponivel(tipo, request.getItens());
        if (erroModalidade != null) return erroModalidade;

        // Ordem 6: Cupom inválido
        Cupom cupom = Cupom.fromString(request.getCupom());
        if (request.getCupom() != null && cupom == null) {
            return "CUPOM_INVALIDO";
        }

        // Ordem 7: Cupom não aplicável
        if (cupom != null) {
            BigDecimal subtotal = calcularSubtotal(request.getItens());
            String erroCupom = validarCupomAplicavel(cupom, subtotal);
            if (erroCupom != null) return erroCupom;
        }

        // Ordem 8: Forma de pagamento
        FormaPagamento forma = FormaPagamento.fromString(request.getFormaPagamento());
        if (forma == null) {
            return "FORMA_PAGAMENTO_INVALIDA";
        }

        // Ordem 9: Parcelamento inválido
        int parcelas = request.getParcelas() != null ? request.getParcelas() : 1;
        String erroParcelamento = validarParcelamento(forma, parcelas);
        if (erroParcelamento != null) return erroParcelamento;

        // Ordem 10: Forma de pagamento indisponível
        String erroFormaPagamento = validarFormaPagamentoDisponivel(forma, request.getItens(), request.getNivelClube());
        if (erroFormaPagamento != null) return erroFormaPagamento;

        return null;
    }

    private String validarCarrinho(java.util.List<ItemRequest> itens) {
        if (itens == null || itens.isEmpty()) {
            return "PEDIDO_INVALIDO";
        }
        for (ItemRequest item : itens) {
            if (item.getPrecoUnitario() == null || item.getPrecoUnitario().signum() <= 0) {
                return "PEDIDO_INVALIDO";
            }
            if (item.getQuantidade() <= 0) {
                return "PEDIDO_INVALIDO";
            }
            if (item.getPesoKg() == null || item.getPesoKg().signum() < 0) {
                return "PEDIDO_INVALIDO";
            }
        }
        return null;
    }

    private String validarModalidadeDisponivel(TipoEntrega tipo, java.util.List<ItemRequest> itens) {
        if (tipo == TipoEntrega.MOTOBOY) {
            BigDecimal pesoTotal = BigDecimal.ZERO;
            for (ItemRequest item : itens) {
                pesoTotal = pesoTotal.add(item.getPesoKg().multiply(new BigDecimal(item.getQuantidade())));
            }
            if (pesoTotal.compareTo(new BigDecimal("5")) > 0) {
                return "MODALIDADE_INDISPONIVEL";
            }
        }
        return null;
    }

    private String validarCupomAplicavel(Cupom cupom, BigDecimal subtotal) {
        if (cupom == Cupom.MENOS50) {
            if (subtotal.compareTo(new BigDecimal("300.00")) < 0) {
                return "CUPOM_NAO_APLICAVEL";
            }
        }
        return null;
    }

    private String validarParcelamento(FormaPagamento forma, int parcelas) {
        if (forma == FormaPagamento.PIX || forma == FormaPagamento.BOLETO) {
            if (parcelas != 1) {
                return "PARCELAMENTO_INVALIDO";
            }
        } else if (forma == FormaPagamento.CARTAO) {
            if (parcelas < 1 || parcelas > 12) {
                return "PARCELAMENTO_INVALIDO";
            }
        }
        return null;
    }

    private String validarFormaPagamentoDisponivel(FormaPagamento forma, java.util.List<ItemRequest> itens, String nivelClube) {
        if (forma == FormaPagamento.BOLETO) {
            BigDecimal subtotal = calcularSubtotal(itens);
            if (subtotal.compareTo(new BigDecimal("1000.00")) > 0) {
                return "FORMA_PAGAMENTO_INDISPONIVEL";
            }
        }
        return null;
    }

    private BigDecimal calcularSubtotal(java.util.List<ItemRequest> itens) {
        BigDecimal subtotal = BigDecimal.ZERO;
        for (ItemRequest item : itens) {
            subtotal = subtotal.add(item.getPrecoUnitario().multiply(new BigDecimal(item.getQuantidade())));
        }
        return subtotal;
    }
}

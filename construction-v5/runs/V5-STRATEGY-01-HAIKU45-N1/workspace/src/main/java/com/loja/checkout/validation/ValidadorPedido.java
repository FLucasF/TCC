package com.loja.checkout.validation;

import com.loja.checkout.coupon.ResolvedorCupom;
import com.loja.checkout.delivery.ResolvedorModalidadeEntrega;
import com.loja.checkout.dto.ItemPedido;
import com.loja.checkout.dto.RequisicaoPedido;
import com.loja.checkout.model.FormaPagamento;
import com.loja.checkout.model.NivelClube;
import com.loja.checkout.model.Regiao;

public class ValidadorPedido {
    public String validar(RequisicaoPedido req) {
        if (!validarItens(req)) {
            return "PEDIDO_INVALIDO";
        }

        if (!validarNivelClube(req)) {
            return "NIVEL_CLUBE_INVALIDO";
        }

        if (!validarRegiao(req)) {
            return "REGIAO_INVALIDA";
        }

        if (!validarModalidadeEntrega(req)) {
            return "MODALIDADE_INVALIDA";
        }

        double pesoTotal = calcularPesoTotal(req.getItens());
        if (!verificarDisponibilidadeModalidade(req.getModalidadeEntrega(), pesoTotal)) {
            return "MODALIDADE_INDISPONIVEL";
        }

        if (!validarCupom(req)) {
            return "CUPOM_INVALIDO";
        }

        double subtotal = calcularSubtotal(req.getItens());
        if (!verificarAplicabilidadeCupom(req.getCupom(), subtotal, req.getItens())) {
            return "CUPOM_NAO_APLICAVEL";
        }

        if (!validarFormaPagamento(req)) {
            return "FORMA_PAGAMENTO_INVALIDA";
        }

        int parcelas = req.getParcelas() != null ? req.getParcelas() : 1;
        if (!validarParcelamento(req.getFormaPagamento(), parcelas)) {
            return "PARCELAMENTO_INVALIDO";
        }

        if (!verificarDisponibilidadeFormaPagamento(req.getFormaPagamento(), subtotal)) {
            return "FORMA_PAGAMENTO_INDISPONIVEL";
        }

        return null;
    }

    private boolean validarItens(RequisicaoPedido req) {
        if (req.getItens() == null || req.getItens().isEmpty()) {
            return false;
        }

        for (ItemPedido item : req.getItens()) {
            if (item.getPrecoUnitario() == null || item.getPrecoUnitario() <= 0) {
                return false;
            }
            if (item.getQuantidade() == null || item.getQuantidade() <= 0) {
                return false;
            }
            if (item.getPesoKg() == null || item.getPesoKg() < 0) {
                return false;
            }
        }

        return true;
    }

    private boolean validarNivelClube(RequisicaoPedido req) {
        String nivel = req.getNivelClube();
        return NivelClube.de(nivel) != null;
    }

    private boolean validarRegiao(RequisicaoPedido req) {
        String regiao = req.getRegiao();
        return Regiao.de(regiao) != null;
    }

    private boolean validarModalidadeEntrega(RequisicaoPedido req) {
        String modalidade = req.getModalidadeEntrega();
        return ResolvedorModalidadeEntrega.resolver(modalidade) != null;
    }

    private boolean validarCupom(RequisicaoPedido req) {
        String cupom = req.getCupom();
        if (cupom == null || cupom.isBlank()) {
            return true;
        }
        return ResolvedorCupom.resolver(cupom) != null;
    }

    private boolean validarFormaPagamento(RequisicaoPedido req) {
        String forma = req.getFormaPagamento();
        return FormaPagamento.de(forma) != null;
    }

    private double calcularPesoTotal(java.util.List<ItemPedido> itens) {
        return itens.stream()
            .mapToDouble(item -> item.getPesoKg() * item.getQuantidade())
            .sum();
    }

    private double calcularSubtotal(java.util.List<ItemPedido> itens) {
        return itens.stream()
            .mapToDouble(item -> item.getPrecoUnitario() * item.getQuantidade())
            .sum();
    }

    private boolean verificarDisponibilidadeModalidade(String modalidade, double pesoTotal) {
        var entrega = ResolvedorModalidadeEntrega.resolver(modalidade);
        return entrega != null && entrega.estaDisponivel(pesoTotal);
    }

    private boolean verificarAplicabilidadeCupom(String codigoCupom, double subtotal, java.util.List<ItemPedido> itens) {
        if (codigoCupom == null || codigoCupom.isBlank()) {
            return true;
        }
        var cupom = ResolvedorCupom.resolver(codigoCupom);
        return cupom != null && cupom.estaAplicavel(subtotal, itens);
    }

    private boolean validarParcelamento(String formaPagamento, int parcelas) {
        FormaPagamento forma = FormaPagamento.de(formaPagamento);
        if (forma == FormaPagamento.PIX || forma == FormaPagamento.BOLETO) {
            return parcelas == 1;
        }
        if (forma == FormaPagamento.CARTAO) {
            return parcelas >= 1 && parcelas <= 12;
        }
        return false;
    }

    private boolean verificarDisponibilidadeFormaPagamento(String formaPagamento, double subtotal) {
        FormaPagamento forma = FormaPagamento.de(formaPagamento);
        if (forma == FormaPagamento.BOLETO) {
            return subtotal <= 1000.00;
        }
        return true;
    }
}

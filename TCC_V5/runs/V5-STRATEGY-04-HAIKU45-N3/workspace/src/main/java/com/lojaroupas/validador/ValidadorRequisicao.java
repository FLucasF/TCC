package com.lojaroupas.validador;

import com.lojaroupas.dto.ItemCarrinho;
import com.lojaroupas.dto.RequisicaoCheckout;
import com.lojaroupas.enums.Cupom;
import com.lojaroupas.enums.FormaPagamento;
import com.lojaroupas.enums.ModalidadeEntrega;
import com.lojaroupas.enums.NivelClube;
import com.lojaroupas.enums.Regiao;
import com.lojaroupas.util.ArredondadorBanqueiro;
import com.lojaroupas.util.UtilCalculosValidacao;
import org.springframework.stereotype.Component;

@Component
public class ValidadorRequisicao {

    public String validar(RequisicaoCheckout req) {
        String erro = validarItens(req);
        if (erro != null) return erro;

        erro = validarNivelClube(req);
        if (erro != null) return erro;

        erro = validarRegiao(req);
        if (erro != null) return erro;

        Double pesoTotal = UtilCalculosValidacao.calcularPesoTotal(req.itens);
        erro = validarModalidadeEntrega(req, pesoTotal);
        if (erro != null) return erro;

        Double subtotal = UtilCalculosValidacao.calcularSubtotal(req.itens);
        erro = validarCupom(req, subtotal);
        if (erro != null) return erro;

        erro = validarFormaPagamento(req);
        if (erro != null) return erro;

        erro = validarParcelamento(req);
        if (erro != null) return erro;

        erro = validarDisponibilidadeFormaPagamento(req);
        if (erro != null) return erro;

        return null;
    }

    private String validarItens(RequisicaoCheckout req) {
        if (req.itens == null || req.itens.isEmpty()) {
            return "PEDIDO_INVALIDO";
        }

        for (ItemCarrinho item : req.itens) {
            if (item.precoUnitario == null || item.precoUnitario <= 0 ||
                item.quantidade == null || item.quantidade <= 0 ||
                item.pesoKg == null || item.pesoKg <= 0) {
                return "PEDIDO_INVALIDO";
            }
        }

        return null;
    }

    private String validarNivelClube(RequisicaoCheckout req) {
        if (req.nivelClube == null || NivelClube.fromString(req.nivelClube) == null) {
            return "NIVEL_CLUBE_INVALIDO";
        }
        return null;
    }

    private String validarRegiao(RequisicaoCheckout req) {
        if (req.regiao == null || Regiao.fromString(req.regiao) == null) {
            return "REGIAO_INVALIDA";
        }
        return null;
    }

    private String validarModalidadeEntrega(RequisicaoCheckout req, Double pesoTotal) {
        if (req.modalidadeEntrega == null || ModalidadeEntrega.fromString(req.modalidadeEntrega) == null) {
            return "MODALIDADE_INVALIDA";
        }

        ModalidadeEntrega modalidade = ModalidadeEntrega.fromString(req.modalidadeEntrega);
        if (!modalidade.validaParaPedido(pesoTotal)) {
            return "MODALIDADE_INDISPONIVEL";
        }

        return null;
    }

    private String validarCupom(RequisicaoCheckout req, Double subtotal) {
        if (req.cupom == null) {
            return null;
        }

        Cupom cupom = Cupom.fromString(req.cupom);
        if (cupom == null) {
            return "CUPOM_INVALIDO";
        }

        if (!cupom.validaParaPedido(subtotal)) {
            return "CUPOM_NAO_APLICAVEL";
        }

        return null;
    }

    private String validarFormaPagamento(RequisicaoCheckout req) {
        if (req.formaPagamento == null || FormaPagamento.fromString(req.formaPagamento) == null) {
            return "FORMA_PAGAMENTO_INVALIDA";
        }
        return null;
    }

    private String validarParcelamento(RequisicaoCheckout req) {
        FormaPagamento forma = FormaPagamento.fromString(req.formaPagamento);
        Integer parcelas = req.getParcelas();

        if (!forma.validaParcelamento(parcelas)) {
            return "PARCELAMENTO_INVALIDO";
        }

        return null;
    }

    private String validarDisponibilidadeFormaPagamento(RequisicaoCheckout req) {
        Double subtotal = ArredondadorBanqueiro.arredondar(UtilCalculosValidacao.calcularSubtotal(req.itens));
        Double pesoTotal = UtilCalculosValidacao.calcularPesoTotal(req.itens);

        Cupom cupom = req.cupom != null ? Cupom.fromString(req.cupom) : null;
        Double descontoCupom = ArredondadorBanqueiro.arredondar(cupom != null ? cupom.aplicarDesconto(subtotal, req.itens) : 0.0);

        ModalidadeEntrega modalidade = ModalidadeEntrega.fromString(req.modalidadeEntrega);
        NivelClube nivelClube = NivelClube.fromString(req.nivelClube);

        Double frete = calcularFrete(modalidade, pesoTotal, nivelClube, cupom);
        frete = ArredondadorBanqueiro.arredondar(frete);

        Regiao regiao = Regiao.fromString(req.regiao);
        Double seguro = ArredondadorBanqueiro.arredondar(regiao.calcularSeguro(subtotal));

        Double totalBase = subtotal - descontoCupom + frete + seguro;

        FormaPagamento forma = FormaPagamento.fromString(req.formaPagamento);
        if (!forma.validaParaPedido(totalBase)) {
            return "FORMA_PAGAMENTO_INDISPONIVEL";
        }

        return null;
    }

    private Double calcularFrete(ModalidadeEntrega modalidade, Double pesoTotal,
                                NivelClube nivelClube, Cupom cupom) {
        if (nivelClube.temFreteGratis()) {
            return 0.0;
        }

        if (cupom != null && cupom.ehFreteGratis()) {
            return 0.0;
        }

        return modalidade.calcularFrete(pesoTotal);
    }
}

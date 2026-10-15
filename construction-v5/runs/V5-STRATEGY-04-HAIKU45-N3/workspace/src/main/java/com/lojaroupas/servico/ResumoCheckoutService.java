package com.lojaroupas.servico;

import com.lojaroupas.dto.ItemCarrinho;
import com.lojaroupas.dto.RequisicaoCheckout;
import com.lojaroupas.dto.RespostaCheckout;
import com.lojaroupas.enums.Cupom;
import com.lojaroupas.enums.FormaPagamento;
import com.lojaroupas.enums.ModalidadeEntrega;
import com.lojaroupas.enums.NivelClube;
import com.lojaroupas.enums.Regiao;
import com.lojaroupas.util.ArredondadorBanqueiro;
import com.lojaroupas.util.UtilCalculosValidacao;
import org.springframework.stereotype.Service;
import java.util.List;

@Service
public class ResumoCheckoutService {

    public RespostaCheckout calcular(RequisicaoCheckout req) {
        Double subtotalOriginal = calcularSubtotal(req);
        Double subtotal = ArredondadorBanqueiro.arredondar(subtotalOriginal);

        Cupom cupom = req.cupom != null ? Cupom.fromString(req.cupom) : null;
        Double descontoCupom = calcularDescontoCupom(cupom, subtotal, req.itens);
        descontoCupom = ArredondadorBanqueiro.arredondar(descontoCupom);

        Double pesoTotal = calcularPesoTotal(req);
        ModalidadeEntrega modalidade = ModalidadeEntrega.fromString(req.modalidadeEntrega);
        NivelClube nivelClube = NivelClube.fromString(req.nivelClube);

        Double frete = calcularFrete(modalidade, pesoTotal, nivelClube, cupom);
        frete = ArredondadorBanqueiro.arredondar(frete);

        Regiao regiao = Regiao.fromString(req.regiao);
        Double seguro = ArredondadorBanqueiro.arredondar(regiao.calcularSeguro(subtotal));

        Double totalBase = subtotal - descontoCupom + frete + seguro;

        FormaPagamento forma = FormaPagamento.fromString(req.formaPagamento);
        Integer parcelas = req.getParcelas();
        Double totalFinal = forma.calcularTotalFinal(totalBase, parcelas);
        Double ajustePagamento = ArredondadorBanqueiro.arredondar(totalFinal - totalBase);
        Double valorParcela = ArredondadorBanqueiro.arredondar(forma.calcularValorParcela(totalFinal, parcelas));

        Double credito = ArredondadorBanqueiro.arredondar(nivelClube.calcularCredito(subtotal));
        Boolean brinde = nivelClube.temBrinde(subtotal);

        return new RespostaCheckout(
            subtotal,
            descontoCupom,
            frete,
            modalidade.getPrazo(),
            seguro,
            ajustePagamento,
            totalFinal,
            parcelas,
            valorParcela,
            credito,
            brinde
        );
    }

    private Double calcularSubtotal(RequisicaoCheckout req) {
        return UtilCalculosValidacao.calcularSubtotal(req.itens);
    }

    private Double calcularDescontoCupom(Cupom cupom, Double subtotal, List<ItemCarrinho> itens) {
        if (cupom == null) {
            return 0.0;
        }
        return cupom.aplicarDesconto(subtotal, itens);
    }

    private Double calcularPesoTotal(RequisicaoCheckout req) {
        return UtilCalculosValidacao.calcularPesoTotal(req.itens);
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

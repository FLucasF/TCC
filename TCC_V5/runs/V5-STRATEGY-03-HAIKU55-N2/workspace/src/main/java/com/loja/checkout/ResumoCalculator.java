package com.loja.checkout;

import org.springframework.stereotype.Service;

import java.math.BigDecimal;

import static com.loja.checkout.Dinheiro.centavos;

@Service
class ResumoCalculator {

    ResumoResponse calcular(ResumoRequest pedido) {
        Carrinho carrinho = Carrinho.de(pedido.itens());
        NivelClube nivel = NivelClube.de(pedido.nivelClube());
        Regiao regiao = Regiao.de(pedido.regiao());
        ModalidadeEntrega modalidade = ModalidadeEntrega.de(pedido.modalidadeEntrega());
        if (!modalidade.disponivel(carrinho)) {
            throw new ResumoException("MODALIDADE_INDISPONIVEL");
        }

        BigDecimal frete = centavos(nivel.isentaFrete() ? BigDecimal.ZERO : modalidade.frete(carrinho));

        BigDecimal desconto = centavos(BigDecimal.ZERO);
        if (pedido.cupom() != null) {
            Cupom cupom = Cupom.de(pedido.cupom());
            if (!cupom.aplicaA(carrinho)) {
                throw new ResumoException("CUPOM_NAO_APLICAVEL");
            }
            desconto = centavos(cupom.desconto(carrinho, frete));
        }

        BigDecimal subtotal = carrinho.subtotal();
        BigDecimal seguro = centavos(subtotal.multiply(regiao.percentualSeguro()));
        BigDecimal total = subtotal.subtract(desconto).add(frete).add(seguro);

        FormaPagamento forma = FormaPagamento.de(pedido.formaPagamento());
        int parcelas = pedido.parcelas() == null ? 1 : pedido.parcelas();
        if (!forma.aceitaParcelas(parcelas)) {
            throw new ResumoException("PARCELAMENTO_INVALIDO");
        }
        if (!forma.disponivel(total)) {
            throw new ResumoException("FORMA_PAGAMENTO_INDISPONIVEL");
        }
        Pagamento pagamento = forma.calcular(total, parcelas);

        return new ResumoResponse(
                subtotal,
                desconto,
                frete,
                modalidade.prazoDias(),
                seguro,
                pagamento.totalFinal().subtract(total),
                pagamento.totalFinal(),
                pagamento.parcelas(),
                pagamento.valorParcela(),
                centavos(subtotal.multiply(nivel.percentualCredito())),
                nivel.temBrinde(subtotal));
    }
}

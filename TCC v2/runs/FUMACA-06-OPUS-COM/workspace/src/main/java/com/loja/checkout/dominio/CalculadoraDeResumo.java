package com.loja.checkout.dominio;

import java.math.BigDecimal;
import java.util.Optional;
import org.springframework.stereotype.Service;

@Service
public class CalculadoraDeResumo {

    public ResumoCompra calcular(SolicitacaoResumo solicitacao) {
        Pedido pedido = new Pedido(solicitacao.itens());

        ModalidadeEntrega modalidade = ModalidadeEntrega.de(solicitacao.modalidadeEntrega());
        if (!modalidade.atende(pedido)) {
            throw new ErroCheckout(CodigoErro.MODALIDADE_INDISPONIVEL);
        }
        BigDecimal frete = modalidade.frete(pedido);

        Optional<Cupom> cupom = Optional.ofNullable(solicitacao.cupom()).map(Cupom::de);
        cupom.filter(aplicado -> !aplicado.aplicavel(pedido)).ifPresent(aplicado -> {
            throw new ErroCheckout(CodigoErro.CUPOM_NAO_APLICAVEL);
        });
        BigDecimal subtotalProdutos = pedido.subtotalProdutos();
        BigDecimal descontoCupom = cupom
                .map(aplicado -> aplicado.desconto(pedido, frete))
                .orElse(Dinheiro.ZERO);

        FormaPagamento formaPagamento = FormaPagamento.de(solicitacao.formaPagamento());
        int parcelas = Optional.ofNullable(solicitacao.parcelas()).orElse(1);
        if (!formaPagamento.aceitaParcelas(parcelas)) {
            throw new ErroCheckout(CodigoErro.PARCELAMENTO_INVALIDO);
        }

        BigDecimal totalPedido = Dinheiro.emCentavos(subtotalProdutos.subtract(descontoCupom).add(frete));
        if (!formaPagamento.atende(totalPedido)) {
            throw new ErroCheckout(CodigoErro.FORMA_PAGAMENTO_INDISPONIVEL);
        }

        ResultadoPagamento cobranca = formaPagamento.cobranca(totalPedido, parcelas);
        BigDecimal ajuste = Dinheiro.emCentavos(cobranca.totalFinal().subtract(totalPedido));

        return new ResumoCompra(
                subtotalProdutos,
                descontoCupom,
                frete,
                modalidade.prazoDias(),
                ajuste,
                cobranca.totalFinal(),
                parcelas,
                cobranca.valorParcela());
    }
}

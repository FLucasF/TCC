package com.loja.checkout.dominio;

import com.loja.checkout.dominio.clube.NivelClube;
import com.loja.checkout.dominio.cupom.CatalogoCupons;
import com.loja.checkout.dominio.cupom.ContextoCupom;
import com.loja.checkout.dominio.cupom.Cupom;
import com.loja.checkout.dominio.entrega.CatalogoEntregas;
import com.loja.checkout.dominio.entrega.ModalidadeEntrega;
import com.loja.checkout.dominio.pagamento.ContextoPagamento;
import com.loja.checkout.dominio.pagamento.FormaPagamento;
import com.loja.checkout.dominio.pagamento.ResultadoPagamento;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.function.Supplier;

/** Monta o resumo da compra na ordem combinada com o financeiro. */
@Service
public class CalculadoraResumo {

    private final CatalogoEntregas entregas;
    private final CatalogoCupons cupons;

    public CalculadoraResumo(CatalogoEntregas entregas, CatalogoCupons cupons) {
        this.entregas = entregas;
        this.cupons = cupons;
    }

    public ResumoCompra calcular(SolicitacaoResumo solicitacao) {
        Pedido pedido = solicitacao.paraPedido();
        NivelClube nivel = NivelClube.porCodigo(solicitacao.nivelClube())
                .orElseThrow(erro(ErroCheckout.NIVEL_CLUBE_INVALIDO));
        Regiao regiao = Regiao.porCodigo(solicitacao.regiao())
                .orElseThrow(erro(ErroCheckout.REGIAO_INVALIDA));
        ModalidadeEntrega entrega = entregas.buscar(solicitacao.modalidadeEntrega())
                .orElseThrow(erro(ErroCheckout.MODALIDADE_INVALIDA));
        if (!entrega.atende(pedido)) {
            throw new CheckoutInvalidoException(ErroCheckout.MODALIDADE_INDISPONIVEL);
        }

        BigDecimal subtotalProdutos = pedido.subtotal();
        BigDecimal frete = nivel.freteCobrado(entrega.custo(pedido));
        BigDecimal descontoCupom = desconto(solicitacao.cupom(), new ContextoCupom(pedido, frete));

        FormaPagamento pagamento = FormaPagamento.porCodigo(solicitacao.formaPagamento())
                .orElseThrow(erro(ErroCheckout.FORMA_PAGAMENTO_INVALIDA));
        int parcelas = solicitacao.parcelasOuPadrao();
        if (!pagamento.aceitaParcelas(parcelas)) {
            throw new CheckoutInvalidoException(ErroCheckout.PARCELAMENTO_INVALIDO);
        }

        BigDecimal produtosComDesconto = subtotalProdutos.subtract(descontoCupom);
        BigDecimal totalSemImposto = Dinheiro.centavos(produtosComDesconto.add(frete));
        if (!pagamento.atende(new ContextoPagamento(totalSemImposto))) {
            throw new CheckoutInvalidoException(ErroCheckout.FORMA_PAGAMENTO_INDISPONIVEL);
        }

        BigDecimal imposto = regiao.imposto(produtosComDesconto);
        BigDecimal totalPedido = Dinheiro.centavos(totalSemImposto.add(imposto));
        ResultadoPagamento resultado = pagamento.aplicar(totalPedido, parcelas);

        return new ResumoCompra(
                subtotalProdutos,
                descontoCupom,
                Dinheiro.centavos(frete),
                entrega.prazoDias(),
                imposto,
                Dinheiro.centavos(resultado.totalFinal().subtract(totalPedido)),
                resultado.totalFinal(),
                parcelas,
                resultado.valorParcela(),
                nivel.creditoProximaCompra(subtotalProdutos),
                nivel.temBrinde(subtotalProdutos));
    }

    private BigDecimal desconto(String codigo, ContextoCupom contexto) {
        if (codigo == null) {
            return Dinheiro.ZERO;
        }
        Cupom cupom = cupons.buscar(codigo).orElseThrow(erro(ErroCheckout.CUPOM_INVALIDO));
        if (!cupom.aplicavel(contexto)) {
            throw new CheckoutInvalidoException(ErroCheckout.CUPOM_NAO_APLICAVEL);
        }
        return cupom.desconto(contexto);
    }

    private Supplier<CheckoutInvalidoException> erro(ErroCheckout erro) {
        return () -> new CheckoutInvalidoException(erro);
    }
}

package com.loja.checkout.dominio;

import com.loja.checkout.catalogo.Catalogo;
import com.loja.checkout.clube.NivelClube;
import com.loja.checkout.cupom.ContextoCupom;
import com.loja.checkout.cupom.Cupom;
import com.loja.checkout.cupom.SemCupom;
import com.loja.checkout.entrega.ModalidadeEntrega;
import com.loja.checkout.pagamento.FormaPagamento;
import com.loja.checkout.pagamento.ResultadoPagamento;
import java.math.BigDecimal;
import org.springframework.stereotype.Service;

/**
 * A sequencia do calculo, igual para todo pedido: produtos, desconto do
 * cupom, frete, seguro, total do pedido e ajuste do pagamento. O que muda de
 * caso para caso cada opcao resolve por conta propria.
 *
 * <p>As recusas sao conferidas na ordem combinada com o site: cada regra e
 * conferida assim que o valor de que ela depende fica pronto.
 */
@Service
public class CalculadoraResumo {

    private final Catalogo<NivelClube> niveisClube;
    private final Catalogo<Regiao> regioes;
    private final Catalogo<ModalidadeEntrega> modalidadesEntrega;
    private final Catalogo<Cupom> cupons;
    private final Catalogo<FormaPagamento> formasPagamento;

    public CalculadoraResumo(Catalogo<NivelClube> niveisClube,
                             Catalogo<Regiao> regioes,
                             Catalogo<ModalidadeEntrega> modalidadesEntrega,
                             Catalogo<Cupom> cupons,
                             Catalogo<FormaPagamento> formasPagamento) {
        this.niveisClube = niveisClube;
        this.regioes = regioes;
        this.modalidadesEntrega = modalidadesEntrega;
        this.cupons = cupons;
        this.formasPagamento = formasPagamento;
    }

    public ResumoCompra calcular(PedidoRecebido pedido) {
        Carrinho carrinho = pedido.carrinho();
        NivelClube nivelClube = niveisClube.exigir(pedido.nivelClube());
        Regiao regiao = regioes.exigir(pedido.regiao());
        ModalidadeEntrega entrega = modalidadesEntrega.exigir(pedido.modalidadeEntrega());

        BigDecimal pesoKg = carrinho.pesoKg();
        ErroPedido.MODALIDADE_INDISPONIVEL.exigir(entrega.atende(pesoKg));

        BigDecimal subtotalProdutos = carrinho.subtotalProdutos();
        BigDecimal frete = nivelClube.frete(Dinheiro.centavos(entrega.custo(pesoKg)));

        Cupom cupom = cupomEscolhido(pedido);
        ContextoCupom contexto = new ContextoCupom(carrinho.itens(), subtotalProdutos, frete);
        ErroPedido.CUPOM_NAO_APLICAVEL.exigir(cupom.aplicavel(contexto));
        BigDecimal descontoCupom = Dinheiro.centavos(cupom.desconto(contexto));

        BigDecimal seguro = regiao.seguro(subtotalProdutos);
        BigDecimal totalPedido = Dinheiro.centavos(
                subtotalProdutos.subtract(descontoCupom).add(frete).add(seguro));

        FormaPagamento pagamento = formasPagamento.exigir(pedido.formaPagamento());
        int parcelas = pedido.parcelasEscolhidas();
        ErroPedido.PARCELAMENTO_INVALIDO.exigir(pagamento.aceitaParcelas(parcelas));
        ErroPedido.FORMA_PAGAMENTO_INDISPONIVEL.exigir(pagamento.atende(totalPedido));
        ResultadoPagamento resultado = pagamento.calcular(totalPedido, parcelas);

        return new ResumoCompra(
                subtotalProdutos,
                descontoCupom,
                frete,
                entrega.prazoDias(),
                seguro,
                resultado.totalFinal().subtract(totalPedido),
                resultado.totalFinal(),
                parcelas,
                resultado.valorParcela(),
                nivelClube.credito(subtotalProdutos),
                nivelClube.brinde(subtotalProdutos));
    }

    private Cupom cupomEscolhido(PedidoRecebido pedido) {
        return pedido.cupom() == null ? SemCupom.INSTANCIA : cupons.exigir(pedido.cupom());
    }
}

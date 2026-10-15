package com.loja.checkout.dominio;

import com.loja.checkout.clube.NivelClube;
import com.loja.checkout.contrato.PedidoRequest;
import com.loja.checkout.contrato.ResumoResponse;
import com.loja.checkout.cupom.ContextoCupom;
import com.loja.checkout.cupom.Cupom;
import com.loja.checkout.entrega.ModalidadeEntrega;
import com.loja.checkout.pagamento.FormaPagamento;
import com.loja.checkout.pagamento.Pagamento;
import java.math.BigDecimal;
import org.springframework.stereotype.Service;

/**
 * A conta do resumo da compra. Esta classe tem a ordem das etapas, que e' igual
 * para todo pedido; o que muda de caso para caso mora na entrega, no cupom, no
 * nivel do clube e na forma de pagamento.
 */
@Service
public class CalculadoraResumo {

    private final Catalogo<NivelClube> clube;
    private final Catalogo<Regiao> regioes;
    private final Catalogo<ModalidadeEntrega> entregas;
    private final Catalogo<Cupom> cupons;
    private final Catalogo<FormaPagamento> pagamentos;

    public CalculadoraResumo(Catalogo<NivelClube> clube,
                             Catalogo<Regiao> regioes,
                             Catalogo<ModalidadeEntrega> entregas,
                             Catalogo<Cupom> cupons,
                             Catalogo<FormaPagamento> pagamentos) {
        this.clube = clube;
        this.regioes = regioes;
        this.entregas = entregas;
        this.cupons = cupons;
        this.pagamentos = pagamentos;
    }

    public ResumoResponse calcular(PedidoRequest requisicao) {
        Pedido pedido = Pedido.de(requisicao.itens());
        NivelClube nivel = clube.resolver(requisicao.nivelClube());
        Regiao regiao = regioes.resolver(requisicao.regiao());
        ModalidadeEntrega entrega = entregas.resolver(requisicao.modalidadeEntrega());
        recusarSe(!entrega.atende(pedido), ErroCheckout.MODALIDADE_INDISPONIVEL);

        BigDecimal subtotalProdutos = pedido.subtotalProdutos();
        BigDecimal frete = nivel.freteDevido(entrega.frete(pedido));
        BigDecimal descontoCupom = descontoDoCupom(requisicao.cupom(),
                new ContextoCupom(pedido, subtotalProdutos, frete));
        BigDecimal seguro = Dinheiro.percentual(subtotalProdutos, regiao.taxaSeguro());
        BigDecimal totalPedido = Dinheiro.arredonda(
                subtotalProdutos.subtract(descontoCupom).add(frete).add(seguro));

        FormaPagamento formaPagamento = pagamentos.resolver(requisicao.formaPagamento());
        int parcelas = requisicao.parcelas() == null ? 1 : requisicao.parcelas();
        recusarSe(!formaPagamento.aceitaParcelas(parcelas), ErroCheckout.PARCELAMENTO_INVALIDO);
        recusarSe(!formaPagamento.atende(totalPedido), ErroCheckout.FORMA_PAGAMENTO_INDISPONIVEL);
        Pagamento pagamento = formaPagamento.calcular(totalPedido, parcelas);

        return new ResumoResponse(
                subtotalProdutos,
                descontoCupom,
                frete,
                entrega.prazoDias(),
                seguro,
                Dinheiro.arredonda(pagamento.totalFinal().subtract(totalPedido)),
                pagamento.totalFinal(),
                parcelas,
                pagamento.valorParcela(),
                nivel.creditoProximaCompra(subtotalProdutos),
                nivel.brinde(subtotalProdutos));
    }

    private BigDecimal descontoDoCupom(String codigo, ContextoCupom contexto) {
        if (codigo == null) {
            return Dinheiro.ZERO;
        }
        Cupom cupom = cupons.resolver(codigo);
        recusarSe(!cupom.aplicavel(contexto), ErroCheckout.CUPOM_NAO_APLICAVEL);
        return Dinheiro.arredonda(cupom.desconto(contexto));
    }

    private static void recusarSe(boolean recusado, ErroCheckout erro) {
        if (recusado) {
            throw new PedidoRecusadoException(erro);
        }
    }
}

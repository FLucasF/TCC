package com.loja.checkout;

import com.loja.checkout.clube.NivelClube;
import com.loja.checkout.cupom.Cupom;
import com.loja.checkout.dominio.Catalogo;
import com.loja.checkout.dominio.Dinheiro;
import com.loja.checkout.dominio.ItemPedido;
import com.loja.checkout.dominio.Pedido;
import com.loja.checkout.dominio.Regiao;
import com.loja.checkout.entrega.Entrega;
import com.loja.checkout.entrega.ModalidadeEntrega;
import com.loja.checkout.pagamento.Cobranca;
import com.loja.checkout.pagamento.FormaPagamento;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.List;

/**
 * O roteiro do calculo, que e o mesmo para toda compra. O que muda de caso para
 * caso mora na modalidade de entrega, no cupom, no nivel do clube, na forma de
 * pagamento e na regiao; aqui so se pergunta a cada um deles.
 */
@Service
public class CalculadoraResumo {

    private static final int PARCELAS_PADRAO = 1;

    private final Catalogo<ModalidadeEntrega> modalidades;
    private final Catalogo<Cupom> cupons;
    private final Catalogo<NivelClube> niveisClube;
    private final Catalogo<FormaPagamento> formasPagamento;

    public CalculadoraResumo(Catalogo<ModalidadeEntrega> modalidades,
                             Catalogo<Cupom> cupons,
                             Catalogo<NivelClube> niveisClube,
                             Catalogo<FormaPagamento> formasPagamento) {
        this.modalidades = modalidades;
        this.cupons = cupons;
        this.niveisClube = niveisClube;
        this.formasPagamento = formasPagamento;
    }

    public ResumoCheckout calcular(RequisicaoResumo requisicao) {
        List<ItemPedido> itens = itensConferidos(requisicao);
        NivelClube nivelClube = niveisClube.porCodigo(codigo(requisicao, RequisicaoResumo::nivelClube))
                .orElseThrow(() -> new ErroCheckout("NIVEL_CLUBE_INVALIDO"));
        Regiao regiao = Regiao.porCodigo(codigo(requisicao, RequisicaoResumo::regiao))
                .orElseThrow(() -> new ErroCheckout("REGIAO_INVALIDA"));

        Pedido pedido = new Pedido(itens, regiao);

        ModalidadeEntrega modalidade = modalidades.porCodigo(codigo(requisicao, RequisicaoResumo::modalidadeEntrega))
                .orElseThrow(() -> new ErroCheckout("MODALIDADE_INVALIDA"));
        if (!modalidade.atende(pedido)) {
            throw new ErroCheckout("MODALIDADE_INDISPONIVEL");
        }
        Entrega entrega = modalidade.calcular(pedido);
        BigDecimal frete = Dinheiro.emCentavos(nivelClube.frete(pedido, entrega.frete()));

        BigDecimal descontoCupom = descontoCupom(requisicao, pedido, frete);

        FormaPagamento formaPagamento = formasPagamento.porCodigo(codigo(requisicao, RequisicaoResumo::formaPagamento))
                .orElseThrow(() -> new ErroCheckout("FORMA_PAGAMENTO_INVALIDA"));
        int parcelas = requisicao.parcelas() == null ? PARCELAS_PADRAO : requisicao.parcelas();
        if (!formaPagamento.permiteParcelas(parcelas)) {
            throw new ErroCheckout("PARCELAMENTO_INVALIDO");
        }

        BigDecimal subtotalProdutos = pedido.subtotalProdutos();
        BigDecimal seguro = regiao.seguro(pedido);
        BigDecimal totalPedido = Dinheiro.emCentavos(
                subtotalProdutos.subtract(descontoCupom).add(frete).add(seguro));

        if (!formaPagamento.atende(totalPedido)) {
            throw new ErroCheckout("FORMA_PAGAMENTO_INDISPONIVEL");
        }
        Cobranca cobranca = formaPagamento.cobrar(totalPedido, parcelas);

        return new ResumoCheckout(
                subtotalProdutos,
                descontoCupom,
                frete,
                entrega.prazoDias(),
                seguro,
                Dinheiro.emCentavos(cobranca.totalFinal().subtract(totalPedido)),
                Dinheiro.emCentavos(cobranca.totalFinal()),
                cobranca.parcelas(),
                Dinheiro.emCentavos(cobranca.valorParcela()),
                nivelClube.creditoProximaCompra(pedido),
                nivelClube.brinde(pedido));
    }

    private List<ItemPedido> itensConferidos(RequisicaoResumo requisicao) {
        if (requisicao == null || requisicao.itens() == null || requisicao.itens().isEmpty()) {
            throw new ErroCheckout("PEDIDO_INVALIDO");
        }
        return requisicao.itens().stream().map(this::itemConferido).toList();
    }

    private ItemPedido itemConferido(RequisicaoResumo.ItemRequisicao item) {
        if (item == null
                || !positivo(item.precoUnitario())
                || item.quantidade() == null || item.quantidade() <= 0
                || !positivo(item.pesoKg())) {
            throw new ErroCheckout("PEDIDO_INVALIDO");
        }
        return new ItemPedido(item.nome(), item.precoUnitario(), item.quantidade(), item.pesoKg());
    }

    private boolean positivo(BigDecimal valor) {
        return valor != null && valor.compareTo(BigDecimal.ZERO) > 0;
    }

    private BigDecimal descontoCupom(RequisicaoResumo requisicao, Pedido pedido, BigDecimal frete) {
        String codigo = codigo(requisicao, RequisicaoResumo::cupom);
        if (codigo == null) {
            return Dinheiro.ZERO;
        }
        Cupom cupom = cupons.porCodigo(codigo).orElseThrow(() -> new ErroCheckout("CUPOM_INVALIDO"));
        if (!cupom.aplicavel(pedido)) {
            throw new ErroCheckout("CUPOM_NAO_APLICAVEL");
        }
        return Dinheiro.emCentavos(cupom.desconto(pedido, frete));
    }

    /** Campo de texto ausente ou em branco conta como nao informado. */
    private String codigo(RequisicaoResumo requisicao,
                          java.util.function.Function<RequisicaoResumo, String> campo) {
        String valor = campo.apply(requisicao);
        return valor == null || valor.isBlank() ? null : valor;
    }
}

package br.com.loja.checkout.aplicacao;

import java.math.BigDecimal;
import java.util.List;

import org.springframework.stereotype.Service;

import br.com.loja.checkout.dominio.Dinheiro;
import br.com.loja.checkout.dominio.Item;
import br.com.loja.checkout.dominio.PedidoRecusadoException;
import br.com.loja.checkout.dominio.Regiao;
import br.com.loja.checkout.dominio.Registro;
import br.com.loja.checkout.dominio.clube.NivelClube;
import br.com.loja.checkout.dominio.cupom.ContextoCupom;
import br.com.loja.checkout.dominio.cupom.Cupom;
import br.com.loja.checkout.dominio.entrega.ModalidadeEntrega;
import br.com.loja.checkout.dominio.pagamento.Cobranca;
import br.com.loja.checkout.dominio.pagamento.FormaPagamento;

/**
 * Calcula o resumo da compra. Este e o unico lugar que conhece a sequencia das
 * etapas e a ordem das validacoes; nenhum caso concreto aparece aqui.
 */
@Service
public class ResumoService {

    private final Registro<ModalidadeEntrega> modalidadesEntrega;
    private final Registro<Cupom> cupons;
    private final Registro<NivelClube> niveisClube;
    private final Registro<FormaPagamento> formasPagamento;

    public ResumoService(Registro<ModalidadeEntrega> modalidadesEntrega,
                         Registro<Cupom> cupons,
                         Registro<NivelClube> niveisClube,
                         Registro<FormaPagamento> formasPagamento) {
        this.modalidadesEntrega = modalidadesEntrega;
        this.cupons = cupons;
        this.niveisClube = niveisClube;
        this.formasPagamento = formasPagamento;
    }

    public Resumo calcular(PedidoSolicitado pedido) {
        List<Item> itens = itensDoCarrinho(pedido);
        NivelClube nivel = niveisClube.buscar(pedido.nivelClube())
                .orElseThrow(() -> recusa("NIVEL_CLUBE_INVALIDO"));
        Regiao regiao = Regiao.porCodigo(pedido.regiao())
                .orElseThrow(() -> recusa("REGIAO_INVALIDA"));
        ModalidadeEntrega entrega = modalidadesEntrega.buscar(pedido.modalidadeEntrega())
                .orElseThrow(() -> recusa("MODALIDADE_INVALIDA"));

        BigDecimal pesoKg = pesoDoPedido(itens);
        if (!entrega.atende(pesoKg)) {
            throw recusa("MODALIDADE_INDISPONIVEL");
        }

        Dinheiro subtotalProdutos = subtotal(itens);
        Dinheiro frete = nivel.frete(entrega.frete(pesoKg));
        Dinheiro descontoCupom = descontoDoCupom(pedido, new ContextoCupom(itens, subtotalProdutos, frete));

        FormaPagamento pagamento = formasPagamento.buscar(pedido.formaPagamento())
                .orElseThrow(() -> recusa("FORMA_PAGAMENTO_INVALIDA"));
        int parcelas = pedido.parcelasOuUma();
        if (!pagamento.parcelasPermitidas(parcelas)) {
            throw recusa("PARCELAMENTO_INVALIDO");
        }

        Dinheiro seguro = regiao.seguro(subtotalProdutos);
        Dinheiro totalPedido = subtotalProdutos.menos(descontoCupom).mais(frete).mais(seguro);
        if (!pagamento.atende(totalPedido)) {
            throw recusa("FORMA_PAGAMENTO_INDISPONIVEL");
        }

        Cobranca cobranca = pagamento.cobrar(totalPedido, parcelas);
        return new Resumo(
                subtotalProdutos,
                descontoCupom,
                frete,
                entrega.prazoDias(),
                seguro,
                cobranca.totalFinal().menos(totalPedido),
                cobranca.totalFinal(),
                parcelas,
                cobranca.valorParcela(),
                nivel.credito(subtotalProdutos),
                nivel.temBrinde(subtotalProdutos));
    }

    private List<Item> itensDoCarrinho(PedidoSolicitado pedido) {
        List<ItemSolicitado> itens = pedido.itens();
        if (itens == null || itens.isEmpty()
                || itens.stream().anyMatch(item -> item == null || !item.valido())) {
            throw recusa("PEDIDO_INVALIDO");
        }
        return itens.stream().map(ItemSolicitado::paraItem).toList();
    }

    private Dinheiro descontoDoCupom(PedidoSolicitado pedido, ContextoCupom contexto) {
        if (!pedido.temCupom()) {
            return Dinheiro.ZERO;
        }
        Cupom cupom = cupons.buscar(pedido.cupom()).orElseThrow(() -> recusa("CUPOM_INVALIDO"));
        if (!cupom.aplicavel(contexto)) {
            throw recusa("CUPOM_NAO_APLICAVEL");
        }
        return cupom.desconto(contexto);
    }

    private BigDecimal pesoDoPedido(List<Item> itens) {
        return itens.stream().map(Item::pesoTotalKg).reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private Dinheiro subtotal(List<Item> itens) {
        return itens.stream().map(Item::total).reduce(Dinheiro.ZERO, Dinheiro::mais);
    }

    private PedidoRecusadoException recusa(String codigo) {
        return new PedidoRecusadoException(codigo);
    }
}

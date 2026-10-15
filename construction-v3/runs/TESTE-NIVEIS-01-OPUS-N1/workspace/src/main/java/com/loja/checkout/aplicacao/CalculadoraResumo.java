package com.loja.checkout.aplicacao;

import com.loja.checkout.clube.NivelClube;
import com.loja.checkout.cupom.BaseCupom;
import com.loja.checkout.cupom.Cupom;
import com.loja.checkout.dominio.Carrinho;
import com.loja.checkout.dominio.Catalogo;
import com.loja.checkout.dominio.Dinheiro;
import com.loja.checkout.dominio.Item;
import com.loja.checkout.dominio.Regiao;
import com.loja.checkout.entrega.Entrega;
import com.loja.checkout.entrega.ModalidadeEntrega;
import com.loja.checkout.pagamento.FormaPagamento;
import com.loja.checkout.pagamento.Pagamento;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.List;
import java.util.Objects;

/**
 * Monta o resumo da compra. A sequencia de etapas e a mesma para todo pedido:
 * o que muda de caso para caso mora na entrega, no cupom, no nivel do clube,
 * na regiao e na forma de pagamento escolhidos.
 */
@Service
public class CalculadoraResumo {

    private static final int PARCELA_UNICA = 1;

    private final Catalogo<ModalidadeEntrega> modalidades;
    private final Catalogo<Cupom> cupons;
    private final Catalogo<NivelClube> niveis;
    private final Catalogo<FormaPagamento> formasPagamento;

    CalculadoraResumo(Catalogo<ModalidadeEntrega> modalidades,
                      Catalogo<Cupom> cupons,
                      Catalogo<NivelClube> niveis,
                      Catalogo<FormaPagamento> formasPagamento) {
        this.modalidades = modalidades;
        this.cupons = cupons;
        this.niveis = niveis;
        this.formasPagamento = formasPagamento;
    }

    public ResumoCompra calcular(PedidoCheckout pedido) {
        Carrinho carrinho = carrinhoDe(pedido);
        NivelClube nivel = niveis.buscar(pedido.nivelClube())
                .orElseThrow(() -> recusa(CodigoErro.NIVEL_CLUBE_INVALIDO));
        Regiao regiao = Regiao.porNome(pedido.regiao())
                .orElseThrow(() -> recusa(CodigoErro.REGIAO_INVALIDA));

        ModalidadeEntrega modalidade = modalidades.buscar(pedido.modalidadeEntrega())
                .orElseThrow(() -> recusa(CodigoErro.MODALIDADE_INVALIDA));
        if (!modalidade.atende(carrinho)) {
            throw recusa(CodigoErro.MODALIDADE_INDISPONIVEL);
        }
        Entrega entrega = modalidade.calcular(carrinho);
        BigDecimal frete = nivel.frete(entrega.frete());

        BigDecimal descontoCupom = descontoDe(pedido.cupom(), new BaseCupom(carrinho, frete));

        FormaPagamento formaPagamento = formasPagamento.buscar(pedido.formaPagamento())
                .orElseThrow(() -> recusa(CodigoErro.FORMA_PAGAMENTO_INVALIDA));
        int parcelas = Objects.requireNonNullElse(pedido.parcelas(), PARCELA_UNICA);
        if (!formaPagamento.aceitaParcelas(parcelas)) {
            throw recusa(CodigoErro.PARCELAMENTO_INVALIDO);
        }

        BigDecimal subtotalProdutos = carrinho.subtotalProdutos();
        BigDecimal seguro = regiao.seguro(subtotalProdutos);
        BigDecimal totalPedido = Dinheiro.centavos(
                subtotalProdutos.subtract(descontoCupom).add(frete).add(seguro));

        if (!formaPagamento.atende(totalPedido)) {
            throw recusa(CodigoErro.FORMA_PAGAMENTO_INDISPONIVEL);
        }
        Pagamento pagamento = formaPagamento.calcular(totalPedido, parcelas);

        return new ResumoCompra(
                subtotalProdutos,
                descontoCupom,
                frete,
                entrega.prazoDias(),
                seguro,
                Dinheiro.centavos(pagamento.totalFinal().subtract(totalPedido)),
                pagamento.totalFinal(),
                pagamento.parcelas(),
                pagamento.valorParcela(),
                nivel.credito(subtotalProdutos),
                nivel.brinde(subtotalProdutos));
    }

    private BigDecimal descontoDe(String codigo, BaseCupom base) {
        if (codigo == null || codigo.isBlank()) {
            return Dinheiro.ZERO;
        }
        Cupom cupom = cupons.buscar(codigo)
                .orElseThrow(() -> recusa(CodigoErro.CUPOM_INVALIDO));
        if (!cupom.aplicavel(base)) {
            throw recusa(CodigoErro.CUPOM_NAO_APLICAVEL);
        }
        return cupom.desconto(base);
    }

    private Carrinho carrinhoDe(PedidoCheckout pedido) {
        List<PedidoCheckout.ItemPedido> itens = pedido.itens();
        if (itens == null || itens.isEmpty()) {
            throw recusa(CodigoErro.PEDIDO_INVALIDO);
        }
        return new Carrinho(itens.stream().map(this::itemDe).toList());
    }

    private Item itemDe(PedidoCheckout.ItemPedido item) {
        if (item == null
                || !positivo(item.precoUnitario())
                || item.quantidade() == null || item.quantidade() <= 0
                || !positivo(item.pesoKg())) {
            throw recusa(CodigoErro.PEDIDO_INVALIDO);
        }
        return new Item(item.nome(), item.precoUnitario(), item.quantidade(), item.pesoKg());
    }

    private boolean positivo(BigDecimal valor) {
        return valor != null && valor.compareTo(BigDecimal.ZERO) > 0;
    }

    private PedidoRecusadoException recusa(CodigoErro codigo) {
        return new PedidoRecusadoException(codigo);
    }
}

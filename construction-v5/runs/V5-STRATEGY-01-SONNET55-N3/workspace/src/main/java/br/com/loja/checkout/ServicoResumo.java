package br.com.loja.checkout;

import static br.com.loja.checkout.CodigoErro.*;

import br.com.loja.checkout.clube.NivelClube;
import br.com.loja.checkout.clube.Vantagens;
import br.com.loja.checkout.cupom.Cupom;
import br.com.loja.checkout.entrega.ModalidadeEntrega;
import br.com.loja.checkout.pagamento.Cobranca;
import br.com.loja.checkout.pagamento.FormaPagamento;
import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Service;

@Service
public class ServicoResumo {

    private final Catalogo<NivelClube> niveis;
    private final Catalogo<ModalidadeEntrega> modalidades;
    private final Catalogo<Cupom> cupons;
    private final Catalogo<FormaPagamento> formas;

    public ServicoResumo(
            List<NivelClube> niveis,
            List<ModalidadeEntrega> modalidades,
            List<Cupom> cupons,
            List<FormaPagamento> formas) {
        this.niveis = new Catalogo<>(niveis, NivelClube::codigo);
        this.modalidades = new Catalogo<>(modalidades, ModalidadeEntrega::codigo);
        this.cupons = new Catalogo<>(cupons, Cupom::codigo);
        this.formas = new Catalogo<>(formas, FormaPagamento::codigo);
    }

    public ResumoResponse calcular(ResumoRequest pedido) {
        Carrinho carrinho = carrinho(pedido.itens());
        NivelClube nivel = exigir(niveis.buscar(pedido.nivelClube()), NIVEL_CLUBE_INVALIDO);
        Regiao regiao = regiao(pedido.regiao());
        ModalidadeEntrega entrega = exigir(modalidades.buscar(pedido.modalidadeEntrega()), MODALIDADE_INVALIDA);
        if (!entrega.atende(carrinho)) {
            throw new PedidoRecusadoException(MODALIDADE_INDISPONIVEL);
        }
        Optional<Cupom> cupom = cupom(pedido.cupom(), carrinho);
        FormaPagamento forma = exigir(formas.buscar(pedido.formaPagamento()), FORMA_PAGAMENTO_INVALIDA);
        int parcelas = pedido.parcelas() == null ? 1 : pedido.parcelas();
        if (!forma.aceitaParcelas(parcelas)) {
            throw new PedidoRecusadoException(PARCELAMENTO_INVALIDO);
        }

        BigDecimal subtotal = carrinho.subtotal();
        Vantagens vantagens = nivel.vantagens(subtotal);
        BigDecimal frete = vantagens.freteGratis() ? Dinheiro.ZERO : entrega.frete(carrinho);
        BigDecimal desconto = cupom.map(c -> c.desconto(carrinho, frete)).orElse(Dinheiro.ZERO);
        BigDecimal seguro = regiao.seguro(subtotal);
        BigDecimal totalPedido = subtotal.subtract(desconto).add(frete).add(seguro);

        if (!forma.aceitaTotal(totalPedido)) {
            throw new PedidoRecusadoException(FORMA_PAGAMENTO_INDISPONIVEL);
        }
        Cobranca cobranca = forma.cobrar(totalPedido, parcelas);

        return new ResumoResponse(
                subtotal,
                desconto,
                frete,
                entrega.prazoDias(),
                seguro,
                cobranca.totalFinal().subtract(totalPedido),
                cobranca.totalFinal(),
                parcelas,
                cobranca.valorParcela(),
                vantagens.creditoProximaCompra(),
                vantagens.brinde());
    }

    private Carrinho carrinho(List<ItemRequest> itens) {
        if (itens == null || itens.isEmpty() || itens.stream().anyMatch(i -> i == null || !valido(i))) {
            throw new PedidoRecusadoException(PEDIDO_INVALIDO);
        }
        return new Carrinho(itens.stream()
                .map(i -> new Item(i.nome(), i.precoUnitario(), i.quantidade(), i.pesoKg()))
                .toList());
    }

    private boolean valido(ItemRequest item) {
        return item.precoUnitario() != null && item.precoUnitario().signum() > 0
                && item.quantidade() != null && item.quantidade() > 0
                && item.pesoKg() != null && item.pesoKg().signum() > 0;
    }

    private Regiao regiao(String codigo) {
        return exigir(Regiao.buscar(codigo), REGIAO_INVALIDA);
    }

    private Optional<Cupom> cupom(String codigo, Carrinho carrinho) {
        if (codigo == null) {
            return Optional.empty();
        }
        Cupom cupom = exigir(cupons.buscar(codigo), CUPOM_INVALIDO);
        if (!cupom.aplicavel(carrinho)) {
            throw new PedidoRecusadoException(CUPOM_NAO_APLICAVEL);
        }
        return Optional.of(cupom);
    }

    private static <T> T exigir(Optional<T> caso, CodigoErro erro) {
        return caso.orElseThrow(() -> new PedidoRecusadoException(erro));
    }
}

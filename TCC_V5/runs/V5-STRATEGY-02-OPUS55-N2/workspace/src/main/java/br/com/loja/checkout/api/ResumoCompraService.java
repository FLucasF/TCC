package br.com.loja.checkout.api;

import br.com.loja.checkout.clube.NivelClube;
import br.com.loja.checkout.cupom.Cupom;
import br.com.loja.checkout.dominio.Carrinho;
import br.com.loja.checkout.dominio.Catalogo;
import br.com.loja.checkout.dominio.CheckoutException;
import br.com.loja.checkout.dominio.Dinheiro;
import br.com.loja.checkout.dominio.Erro;
import br.com.loja.checkout.dominio.Item;
import br.com.loja.checkout.entrega.Cotacao;
import br.com.loja.checkout.entrega.ModalidadeEntrega;
import br.com.loja.checkout.pagamento.FormaPagamento;
import br.com.loja.checkout.pagamento.Pagamento;
import br.com.loja.checkout.seguro.Regiao;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Predicate;

@Service
public class ResumoCompraService {

    private final Catalogo<NivelClube> niveis;
    private final Catalogo<ModalidadeEntrega> modalidades;
    private final Catalogo<Cupom> cupons;
    private final Catalogo<FormaPagamento> formasPagamento;

    public ResumoCompraService(List<NivelClube> niveis, List<ModalidadeEntrega> modalidades,
                               List<Cupom> cupons, List<FormaPagamento> formasPagamento) {
        this.niveis = new Catalogo<>(niveis);
        this.modalidades = new Catalogo<>(modalidades);
        this.cupons = new Catalogo<>(cupons);
        this.formasPagamento = new Catalogo<>(formasPagamento);
    }

    public ResumoResponse resumir(PedidoRequest pedido) {
        Carrinho carrinho = carrinho(pedido.itens());
        NivelClube nivel = niveis.buscar(pedido.nivelClube())
                .orElseThrow(() -> new CheckoutException(Erro.NIVEL_CLUBE_INVALIDO));
        Regiao regiao = Regiao.buscar(pedido.regiao())
                .orElseThrow(() -> new CheckoutException(Erro.REGIAO_INVALIDA));
        ModalidadeEntrega modalidade = modalidades.buscar(pedido.modalidadeEntrega())
                .orElseThrow(() -> new CheckoutException(Erro.MODALIDADE_INVALIDA));
        exigir(modalidade.atende(carrinho), Erro.MODALIDADE_INDISPONIVEL);
        Optional<Cupom> cupom = cupom(pedido.cupom());
        exigir(cupom.map(c -> c.aplicavel(carrinho)).orElse(true), Erro.CUPOM_NAO_APLICAVEL);
        FormaPagamento formaPagamento = formasPagamento.buscar(pedido.formaPagamento())
                .orElseThrow(() -> new CheckoutException(Erro.FORMA_PAGAMENTO_INVALIDA));
        int parcelas = Objects.requireNonNullElse(pedido.parcelas(), 1);
        exigir(formaPagamento.aceitaParcelas(parcelas), Erro.PARCELAMENTO_INVALIDO);

        BigDecimal subtotal = carrinho.subtotal();
        Cotacao cotacao = modalidade.cotar(carrinho);
        BigDecimal frete = nivel.frete(cotacao.frete());
        BigDecimal desconto = cupom.map(c -> c.desconto(carrinho, frete)).orElse(Dinheiro.ZERO);
        BigDecimal seguro = regiao.seguro(subtotal);
        BigDecimal totalPedido = subtotal.subtract(desconto).add(frete).add(seguro);

        exigir(formaPagamento.atende(totalPedido), Erro.FORMA_PAGAMENTO_INDISPONIVEL);
        Pagamento pagamento = formaPagamento.pagar(totalPedido, parcelas);

        return new ResumoResponse(
                subtotal,
                desconto,
                frete,
                cotacao.prazoDias(),
                seguro,
                pagamento.totalFinal().subtract(totalPedido),
                pagamento.totalFinal(),
                pagamento.parcelas(),
                pagamento.valorParcela(),
                nivel.creditoProximaCompra(subtotal),
                nivel.ganhaBrinde(subtotal));
    }

    private Optional<Cupom> cupom(String codigo) {
        return Optional.ofNullable(codigo)
                .filter(Predicate.not(String::isBlank))
                .map(c -> cupons.buscar(c).orElseThrow(() -> new CheckoutException(Erro.CUPOM_INVALIDO)));
    }

    private static Carrinho carrinho(List<ItemRequest> itens) {
        exigir(itens != null && !itens.isEmpty(), Erro.PEDIDO_INVALIDO);
        return new Carrinho(itens.stream().map(ResumoCompraService::item).toList());
    }

    private static Item item(ItemRequest item) {
        exigir(item != null
                && positivo(item.precoUnitario())
                && item.quantidade() != null && item.quantidade() > 0
                && positivo(item.pesoKg()), Erro.PEDIDO_INVALIDO);
        return new Item(item.nome(), item.precoUnitario(), item.quantidade(), item.pesoKg());
    }

    private static boolean positivo(BigDecimal valor) {
        return valor != null && valor.signum() > 0;
    }

    private static void exigir(boolean condicao, Erro erro) {
        if (!condicao) {
            throw new CheckoutException(erro);
        }
    }
}

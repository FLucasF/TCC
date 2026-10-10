package br.com.loja.checkout;

import br.com.loja.checkout.api.ItemRequest;
import br.com.loja.checkout.api.ResumoRequest;
import br.com.loja.checkout.api.ResumoResponse;
import br.com.loja.checkout.clube.NivelClube;
import br.com.loja.checkout.cupom.Cupom;
import br.com.loja.checkout.entrega.ModalidadeEntrega;
import br.com.loja.checkout.pagamento.Cobranca;
import br.com.loja.checkout.pagamento.FormaPagamento;
import java.math.BigDecimal;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import org.springframework.stereotype.Service;

@Service
public class CheckoutService {

    private final Catalogo<ModalidadeEntrega> modalidades;
    private final Catalogo<Cupom> cupons;
    private final Catalogo<NivelClube> niveis;
    private final Catalogo<FormaPagamento> formasPagamento;

    public CheckoutService(List<ModalidadeEntrega> modalidades, List<Cupom> cupons,
            List<NivelClube> niveis, List<FormaPagamento> formasPagamento) {
        this.modalidades = new Catalogo<>(modalidades);
        this.cupons = new Catalogo<>(cupons);
        this.niveis = new Catalogo<>(niveis);
        this.formasPagamento = new Catalogo<>(formasPagamento);
    }

    public ResumoResponse resumir(ResumoRequest pedido) {
        Carrinho carrinho = carrinho(pedido.itens());
        NivelClube nivel = exigir(niveis.buscar(pedido.nivelClube()), "NIVEL_CLUBE_INVALIDO");
        Regiao regiao = exigir(Regiao.de(pedido.regiao()), "REGIAO_INVALIDA");

        ModalidadeEntrega modalidade = exigir(modalidades.buscar(pedido.modalidadeEntrega()), "MODALIDADE_INVALIDA");
        exigir(modalidade.atende(carrinho), "MODALIDADE_INDISPONIVEL");

        Optional<Cupom> cupom = cupom(pedido.cupom(), carrinho);

        FormaPagamento formaPagamento = exigir(formasPagamento.buscar(pedido.formaPagamento()), "FORMA_PAGAMENTO_INVALIDA");
        int parcelas = Objects.requireNonNullElse(pedido.parcelas(), 1);
        exigir(formaPagamento.aceitaParcelas(parcelas), "PARCELAMENTO_INVALIDO");

        BigDecimal subtotal = carrinho.subtotal();
        BigDecimal frete = nivel.freteCobrado(modalidade.frete(carrinho));
        BigDecimal desconto = cupom.map(c -> c.desconto(carrinho, frete)).orElse(Dinheiro.ZERO);
        BigDecimal seguro = regiao.seguro(subtotal);
        BigDecimal totalPedido = subtotal.subtract(desconto).add(frete).add(seguro);

        exigir(formaPagamento.atende(totalPedido), "FORMA_PAGAMENTO_INDISPONIVEL");
        Cobranca cobranca = formaPagamento.cobrar(totalPedido, parcelas);

        return new ResumoResponse(
                subtotal,
                desconto,
                frete,
                modalidade.prazoDias(),
                seguro,
                cobranca.totalFinal().subtract(totalPedido),
                cobranca.totalFinal(),
                parcelas,
                cobranca.valorParcela(),
                nivel.creditoProximaCompra(subtotal),
                nivel.brinde(subtotal));
    }

    private static Carrinho carrinho(List<ItemRequest> itens) {
        exigir(itens != null && !itens.isEmpty() && itens.stream().allMatch(CheckoutService::itemValido), "PEDIDO_INVALIDO");
        return new Carrinho(itens.stream()
                .map(i -> new Item(i.nome(), i.precoUnitario(), i.quantidade(), i.pesoKg()))
                .toList());
    }

    private static boolean itemValido(ItemRequest item) {
        return item != null
                && positivo(item.precoUnitario())
                && item.quantidade() != null && item.quantidade() > 0
                && positivo(item.pesoKg());
    }

    private static boolean positivo(BigDecimal valor) {
        return valor != null && valor.signum() > 0;
    }

    private Optional<Cupom> cupom(String codigo, Carrinho carrinho) {
        if (codigo == null) {
            return Optional.empty();
        }
        Cupom cupom = exigir(cupons.buscar(codigo), "CUPOM_INVALIDO");
        exigir(cupom.aplicavel(carrinho), "CUPOM_NAO_APLICAVEL");
        return Optional.of(cupom);
    }

    private static <T> T exigir(Optional<T> valor, String erro) {
        return valor.orElseThrow(() -> new PedidoRecusadoException(erro));
    }

    private static void exigir(boolean condicao, String erro) {
        if (!condicao) {
            throw new PedidoRecusadoException(erro);
        }
    }
}

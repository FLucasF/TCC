package br.com.loja.checkout.resumo;

import br.com.loja.checkout.clube.NivelClube;
import br.com.loja.checkout.cupom.Cupom;
import br.com.loja.checkout.entrega.ModalidadeEntrega;
import br.com.loja.checkout.pagamento.FormaPagamento;
import br.com.loja.checkout.pagamento.Pagamento;
import java.math.BigDecimal;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import org.springframework.stereotype.Service;

@Service
public class CalculadoraResumo {

    private final Catalogo<NivelClube> niveis;
    private final Catalogo<ModalidadeEntrega> modalidades;
    private final Catalogo<Cupom> cupons;
    private final Catalogo<FormaPagamento> formasPagamento;

    public CalculadoraResumo(List<NivelClube> niveis, List<ModalidadeEntrega> modalidades,
            List<Cupom> cupons, List<FormaPagamento> formasPagamento) {
        this.niveis = new Catalogo<>(niveis, NivelClube::codigo);
        this.modalidades = new Catalogo<>(modalidades, ModalidadeEntrega::codigo);
        this.cupons = new Catalogo<>(cupons, Cupom::codigo);
        this.formasPagamento = new Catalogo<>(formasPagamento, FormaPagamento::codigo);
    }

    public Resumo calcular(SolicitacaoResumo solicitacao) {
        Carrinho carrinho = carrinho(solicitacao.itens());
        NivelClube nivel = niveis.buscar(solicitacao.nivelClube())
                .orElseThrow(() -> new CheckoutException(ErroCheckout.NIVEL_CLUBE_INVALIDO));
        Regiao regiao = Regiao.buscar(solicitacao.regiao())
                .orElseThrow(() -> new CheckoutException(ErroCheckout.REGIAO_INVALIDA));
        ModalidadeEntrega modalidade = modalidade(solicitacao.modalidadeEntrega(), carrinho);
        Optional<Cupom> cupom = cupom(solicitacao.cupom(), carrinho);
        int parcelas = Objects.requireNonNullElse(solicitacao.parcelas(), 1);
        FormaPagamento formaPagamento = formaPagamento(solicitacao.formaPagamento(), parcelas);

        BigDecimal subtotal = carrinho.subtotal();
        BigDecimal frete = nivel.frete(modalidade.frete(carrinho));
        BigDecimal desconto = cupom.map(c -> c.desconto(carrinho, frete)).orElse(Dinheiro.ZERO);
        BigDecimal seguro = regiao.seguro(subtotal);
        BigDecimal totalPedido = subtotal.subtract(desconto).add(frete).add(seguro);

        if (!formaPagamento.atende(totalPedido)) {
            throw new CheckoutException(ErroCheckout.FORMA_PAGAMENTO_INDISPONIVEL);
        }
        Pagamento pagamento = formaPagamento.pagar(totalPedido, parcelas);

        return new Resumo(
                subtotal,
                desconto,
                frete,
                modalidade.prazoDias(),
                seguro,
                pagamento.totalFinal().subtract(totalPedido),
                pagamento.totalFinal(),
                pagamento.parcelas(),
                pagamento.valorParcela(),
                nivel.credito(subtotal),
                nivel.brinde(subtotal));
    }

    private static Carrinho carrinho(List<Item> itens) {
        if (itens == null || itens.isEmpty() || !itens.stream().allMatch(item -> item != null && item.valido())) {
            throw new CheckoutException(ErroCheckout.PEDIDO_INVALIDO);
        }
        return new Carrinho(itens);
    }

    private ModalidadeEntrega modalidade(String codigo, Carrinho carrinho) {
        ModalidadeEntrega modalidade = modalidades.buscar(codigo)
                .orElseThrow(() -> new CheckoutException(ErroCheckout.MODALIDADE_INVALIDA));
        if (!modalidade.atende(carrinho)) {
            throw new CheckoutException(ErroCheckout.MODALIDADE_INDISPONIVEL);
        }
        return modalidade;
    }

    private Optional<Cupom> cupom(String codigo, Carrinho carrinho) {
        if (codigo == null) {
            return Optional.empty();
        }
        Cupom cupom = cupons.buscar(codigo)
                .orElseThrow(() -> new CheckoutException(ErroCheckout.CUPOM_INVALIDO));
        if (!cupom.aplicavel(carrinho)) {
            throw new CheckoutException(ErroCheckout.CUPOM_NAO_APLICAVEL);
        }
        return Optional.of(cupom);
    }

    private FormaPagamento formaPagamento(String codigo, int parcelas) {
        FormaPagamento formaPagamento = formasPagamento.buscar(codigo)
                .orElseThrow(() -> new CheckoutException(ErroCheckout.FORMA_PAGAMENTO_INVALIDA));
        if (!formaPagamento.aceitaParcelas(parcelas)) {
            throw new CheckoutException(ErroCheckout.PARCELAMENTO_INVALIDO);
        }
        return formaPagamento;
    }
}

package com.loja.checkout.dominio;

import com.loja.checkout.dominio.clube.NivelClube;
import com.loja.checkout.dominio.cupom.ContextoCupom;
import com.loja.checkout.dominio.cupom.Cupom;
import com.loja.checkout.dominio.cupom.SemCupom;
import com.loja.checkout.dominio.entrega.ModalidadeEntrega;
import com.loja.checkout.dominio.pagamento.FormaPagamento;
import com.loja.checkout.dominio.pagamento.ResultadoPagamento;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * Calcula o resumo da compra. Esta classe só conhece a ordem do cálculo e a ordem das
 * conferências: quanto custa o frete, quanto desconta o cupom, quanto ajusta o pagamento
 * e o que o clube dá é responsabilidade de cada caso.
 */
@Service
public class CalculadoraResumo {

    private static final int PARCELA_UNICA = 1;

    private final Catalogo<ModalidadeEntrega> modalidades;
    private final Catalogo<Cupom> cupons;
    private final Catalogo<FormaPagamento> formasPagamento;

    public CalculadoraResumo(List<ModalidadeEntrega> modalidades,
                             List<Cupom> cupons,
                             List<FormaPagamento> formasPagamento) {
        this.modalidades = new Catalogo<>(modalidades);
        this.cupons = new Catalogo<>(cupons);
        this.formasPagamento = new Catalogo<>(formasPagamento);
    }

    public ResumoCompra calcular(PedidoInformado pedido) {
        Carrinho carrinho = carrinhoDe(pedido.itens());
        NivelClube nivel = Codigos.buscar(NivelClube.class, pedido.nivelClube())
                .orElseThrow(() -> recusar(ErroCheckout.NIVEL_CLUBE_INVALIDO));
        Regiao regiao = Codigos.buscar(Regiao.class, pedido.regiao())
                .orElseThrow(() -> recusar(ErroCheckout.REGIAO_INVALIDA));

        ModalidadeEntrega modalidade = modalidades.buscar(pedido.modalidadeEntrega())
                .orElseThrow(() -> recusar(ErroCheckout.MODALIDADE_INVALIDA));
        BigDecimal pesoKg = carrinho.pesoKg();
        exigir(modalidade.atende(pesoKg), ErroCheckout.MODALIDADE_INDISPONIVEL);

        BigDecimal subtotalProdutos = carrinho.subtotal();
        BigDecimal frete = nivel.freteCobrado(modalidade.frete(pesoKg));

        Cupom cupom = cupomDe(pedido.cupom());
        ContextoCupom contexto = new ContextoCupom(carrinho, frete);
        exigir(cupom.aplicavel(contexto), ErroCheckout.CUPOM_NAO_APLICAVEL);
        BigDecimal descontoCupom = cupom.desconto(contexto);

        FormaPagamento formaPagamento = formasPagamento.buscar(pedido.formaPagamento())
                .orElseThrow(() -> recusar(ErroCheckout.FORMA_PAGAMENTO_INVALIDA));
        int parcelas = Objects.requireNonNullElse(pedido.parcelas(), PARCELA_UNICA);
        exigir(formaPagamento.parcelasPermitidas(parcelas), ErroCheckout.PARCELAMENTO_INVALIDO);

        BigDecimal seguro = regiao.seguro(subtotalProdutos);
        BigDecimal totalPedido = Dinheiro.arredonda(
                subtotalProdutos.subtract(descontoCupom).add(frete).add(seguro));
        exigir(formaPagamento.atende(totalPedido), ErroCheckout.FORMA_PAGAMENTO_INDISPONIVEL);

        ResultadoPagamento pagamento = formaPagamento.calcular(totalPedido, parcelas);

        return new ResumoCompra(
                subtotalProdutos,
                descontoCupom,
                frete,
                modalidade.prazoDias(),
                seguro,
                pagamento.totalFinal().subtract(totalPedido),
                pagamento.totalFinal(),
                parcelas,
                pagamento.valorParcela(),
                nivel.credito(subtotalProdutos),
                nivel.temBrinde(subtotalProdutos));
    }

    private Carrinho carrinhoDe(List<ItemInformado> itens) {
        exigir(itens != null && !itens.isEmpty(), ErroCheckout.PEDIDO_INVALIDO);
        return new Carrinho(itens.stream().map(this::itemDe).toList());
    }

    private ItemPedido itemDe(ItemInformado item) {
        exigir(item != null, ErroCheckout.PEDIDO_INVALIDO);
        exigir(positivo(item.precoUnitario()), ErroCheckout.PEDIDO_INVALIDO);
        exigir(item.quantidade() != null && item.quantidade() > 0, ErroCheckout.PEDIDO_INVALIDO);
        exigir(positivo(item.pesoKg()), ErroCheckout.PEDIDO_INVALIDO);
        return new ItemPedido(item.nome(), item.precoUnitario(), item.quantidade(), item.pesoKg());
    }

    private boolean positivo(BigDecimal valor) {
        return valor != null && valor.compareTo(BigDecimal.ZERO) > 0;
    }

    /** Cupom não informado (ausente ou em branco) é o caso "sem cupom". */
    private Cupom cupomDe(String codigo) {
        return Optional.ofNullable(codigo)
                .filter(informado -> !informado.isBlank())
                .map(informado -> cupons.buscar(informado)
                        .orElseThrow(() -> recusar(ErroCheckout.CUPOM_INVALIDO)))
                .orElse(SemCupom.INSTANCIA);
    }

    private void exigir(boolean condicao, ErroCheckout erro) {
        if (!condicao) {
            throw recusar(erro);
        }
    }

    private ErroCheckoutException recusar(ErroCheckout erro) {
        return new ErroCheckoutException(erro);
    }
}

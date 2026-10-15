package br.com.loja.checkout;

import static br.com.loja.checkout.pedido.ErroCheckout.CUPOM_INVALIDO;
import static br.com.loja.checkout.pedido.ErroCheckout.CUPOM_NAO_APLICAVEL;
import static br.com.loja.checkout.pedido.ErroCheckout.FORMA_PAGAMENTO_INDISPONIVEL;
import static br.com.loja.checkout.pedido.ErroCheckout.FORMA_PAGAMENTO_INVALIDA;
import static br.com.loja.checkout.pedido.ErroCheckout.MODALIDADE_INDISPONIVEL;
import static br.com.loja.checkout.pedido.ErroCheckout.MODALIDADE_INVALIDA;
import static br.com.loja.checkout.pedido.ErroCheckout.NIVEL_CLUBE_INVALIDO;
import static br.com.loja.checkout.pedido.ErroCheckout.PARCELAMENTO_INVALIDO;
import static br.com.loja.checkout.pedido.ErroCheckout.PEDIDO_INVALIDO;
import static br.com.loja.checkout.pedido.ErroCheckout.REGIAO_INVALIDA;

import br.com.loja.checkout.clube.NivelClube;
import br.com.loja.checkout.cupom.Cupom;
import br.com.loja.checkout.cupom.SemCupom;
import br.com.loja.checkout.entrega.ModalidadeEntrega;
import br.com.loja.checkout.pagamento.FormaPagamento;
import br.com.loja.checkout.pagamento.Pagamento;
import br.com.loja.checkout.pedido.Carrinho;
import br.com.loja.checkout.pedido.Catalogo;
import br.com.loja.checkout.pedido.CheckoutRecusadoException;
import br.com.loja.checkout.pedido.ErroCheckout;
import br.com.loja.checkout.pedido.ItemPedido;
import br.com.loja.checkout.seguro.Regiao;
import java.math.BigDecimal;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Supplier;
import org.springframework.stereotype.Service;

@Service
public class CheckoutService {

    private static final int PARCELAS_PADRAO = 1;

    private final Catalogo<ModalidadeEntrega> modalidades;
    private final Catalogo<Cupom> cupons;
    private final Catalogo<NivelClube> niveis;
    private final Catalogo<FormaPagamento> formasPagamento;
    private final Catalogo<Regiao> regioes = new Catalogo<>(Arrays.asList(Regiao.values()));

    public CheckoutService(List<ModalidadeEntrega> modalidades, List<Cupom> cupons,
            List<NivelClube> niveis, List<FormaPagamento> formasPagamento) {
        this.modalidades = new Catalogo<>(modalidades);
        this.cupons = new Catalogo<>(cupons);
        this.niveis = new Catalogo<>(niveis);
        this.formasPagamento = new Catalogo<>(formasPagamento);
    }

    public ResumoCompra resumir(PedidoCheckout pedido) {
        Carrinho carrinho = carrinho(pedido.itens());
        NivelClube nivel = niveis.buscar(pedido.nivelClube()).orElseThrow(recusa(NIVEL_CLUBE_INVALIDO));
        Regiao regiao = regioes.buscar(pedido.regiao()).orElseThrow(recusa(REGIAO_INVALIDA));

        ModalidadeEntrega modalidade = modalidades.buscar(pedido.modalidadeEntrega())
                .orElseThrow(recusa(MODALIDADE_INVALIDA));
        exigir(modalidade.atende(carrinho), MODALIDADE_INDISPONIVEL);

        Cupom cupom = Optional.ofNullable(pedido.cupom())
                .map(codigo -> cupons.buscar(codigo).orElseThrow(recusa(CUPOM_INVALIDO)))
                .orElse(SemCupom.INSTANCIA);
        exigir(cupom.aplicavel(carrinho), CUPOM_NAO_APLICAVEL);

        FormaPagamento formaPagamento = formasPagamento.buscar(pedido.formaPagamento())
                .orElseThrow(recusa(FORMA_PAGAMENTO_INVALIDA));
        int parcelas = Objects.requireNonNullElse(pedido.parcelas(), PARCELAS_PADRAO);
        exigir(formaPagamento.aceitaParcelas(parcelas), PARCELAMENTO_INVALIDO);

        BigDecimal subtotal = carrinho.subtotal();
        BigDecimal frete = nivel.frete(modalidade.frete(carrinho));
        BigDecimal desconto = cupom.desconto(carrinho, frete);
        BigDecimal seguro = regiao.seguro(subtotal);
        BigDecimal totalPedido = subtotal.subtract(desconto).add(frete).add(seguro);

        exigir(formaPagamento.atende(totalPedido), FORMA_PAGAMENTO_INDISPONIVEL);
        Pagamento pagamento = formaPagamento.pagar(totalPedido, parcelas);

        return new ResumoCompra(
                subtotal,
                desconto,
                frete,
                modalidade.prazoDias(),
                seguro,
                pagamento.totalFinal().subtract(totalPedido),
                pagamento.totalFinal(),
                pagamento.parcelas(),
                pagamento.valorParcela(),
                nivel.creditoProximaCompra(subtotal),
                nivel.brinde(subtotal));
    }

    private static Carrinho carrinho(List<PedidoCheckout.Item> itens) {
        exigir(itens != null && !itens.isEmpty() && itens.stream().allMatch(CheckoutService::itemValido),
                PEDIDO_INVALIDO);
        return new Carrinho(itens.stream()
                .map(item -> new ItemPedido(item.nome(), item.precoUnitario(), item.quantidade(), item.pesoKg()))
                .toList());
    }

    private static boolean itemValido(PedidoCheckout.Item item) {
        return item != null
                && positivo(item.precoUnitario())
                && item.quantidade() != null && item.quantidade() > 0
                && positivo(item.pesoKg());
    }

    private static boolean positivo(BigDecimal valor) {
        return valor != null && valor.signum() > 0;
    }

    private static void exigir(boolean condicao, ErroCheckout erro) {
        if (!condicao) {
            throw new CheckoutRecusadoException(erro);
        }
    }

    private static Supplier<CheckoutRecusadoException> recusa(ErroCheckout erro) {
        return () -> new CheckoutRecusadoException(erro);
    }
}

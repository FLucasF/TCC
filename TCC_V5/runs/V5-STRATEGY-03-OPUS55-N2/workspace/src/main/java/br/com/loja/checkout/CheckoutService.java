package br.com.loja.checkout;

import static br.com.loja.checkout.dominio.CodigoErro.CUPOM_INVALIDO;
import static br.com.loja.checkout.dominio.CodigoErro.CUPOM_NAO_APLICAVEL;
import static br.com.loja.checkout.dominio.CodigoErro.FORMA_PAGAMENTO_INDISPONIVEL;
import static br.com.loja.checkout.dominio.CodigoErro.FORMA_PAGAMENTO_INVALIDA;
import static br.com.loja.checkout.dominio.CodigoErro.MODALIDADE_INDISPONIVEL;
import static br.com.loja.checkout.dominio.CodigoErro.MODALIDADE_INVALIDA;
import static br.com.loja.checkout.dominio.CodigoErro.NIVEL_CLUBE_INVALIDO;
import static br.com.loja.checkout.dominio.CodigoErro.PARCELAMENTO_INVALIDO;
import static br.com.loja.checkout.dominio.CodigoErro.PEDIDO_INVALIDO;
import static br.com.loja.checkout.dominio.CodigoErro.REGIAO_INVALIDA;

import br.com.loja.checkout.api.PedidoRequest;
import br.com.loja.checkout.api.ResumoResponse;
import br.com.loja.checkout.clube.NivelClube;
import br.com.loja.checkout.cupom.ContextoCupom;
import br.com.loja.checkout.cupom.Cupom;
import br.com.loja.checkout.dominio.Carrinho;
import br.com.loja.checkout.dominio.Catalogo;
import br.com.loja.checkout.dominio.CodigoErro;
import br.com.loja.checkout.dominio.Dinheiro;
import br.com.loja.checkout.dominio.Item;
import br.com.loja.checkout.dominio.PedidoRecusadoException;
import br.com.loja.checkout.dominio.Regiao;
import br.com.loja.checkout.entrega.ModalidadeEntrega;
import br.com.loja.checkout.pagamento.Cobranca;
import br.com.loja.checkout.pagamento.FormaPagamento;
import java.math.BigDecimal;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import org.springframework.stereotype.Service;

@Service
public class CheckoutService {

    private final Catalogo<NivelClube> niveis;
    private final Catalogo<Regiao> regioes;
    private final Catalogo<ModalidadeEntrega> modalidades;
    private final Catalogo<Cupom> cupons;
    private final Catalogo<FormaPagamento> formasPagamento;

    public CheckoutService(List<NivelClube> niveis, List<ModalidadeEntrega> modalidades, List<Cupom> cupons,
            List<FormaPagamento> formasPagamento) {
        this.niveis = new Catalogo<>(niveis, NivelClube::codigo);
        this.regioes = new Catalogo<>(Arrays.asList(Regiao.values()), Regiao::name);
        this.modalidades = new Catalogo<>(modalidades, ModalidadeEntrega::codigo);
        this.cupons = new Catalogo<>(cupons, Cupom::codigo);
        this.formasPagamento = new Catalogo<>(formasPagamento, FormaPagamento::codigo);
    }

    public ResumoResponse resumir(PedidoRequest pedido) {
        Carrinho carrinho = carrinho(pedido);
        NivelClube nivel = niveis.exigir(pedido.nivelClube(), NIVEL_CLUBE_INVALIDO);
        Regiao regiao = regioes.exigir(pedido.regiao(), REGIAO_INVALIDA);
        ModalidadeEntrega modalidade = modalidades.exigir(pedido.modalidadeEntrega(), MODALIDADE_INVALIDA);
        exigir(modalidade.atende(carrinho), MODALIDADE_INDISPONIVEL);

        BigDecimal subtotal = carrinho.subtotalProdutos();
        BigDecimal frete = nivel.freteCobrado(modalidade.frete(carrinho));
        ContextoCupom contextoCupom = new ContextoCupom(carrinho, subtotal, frete);

        Optional<Cupom> cupom = Optional.ofNullable(pedido.cupom()).map(codigo -> cupons.exigir(codigo, CUPOM_INVALIDO));
        cupom.ifPresent(c -> exigir(c.aplicavel(contextoCupom), CUPOM_NAO_APLICAVEL));
        BigDecimal descontoCupom = cupom.map(c -> c.desconto(contextoCupom)).orElse(Dinheiro.ZERO);

        BigDecimal seguro = regiao.seguro(subtotal);
        BigDecimal totalPedido = subtotal.subtract(descontoCupom).add(frete).add(seguro);

        FormaPagamento formaPagamento = formasPagamento.exigir(pedido.formaPagamento(), FORMA_PAGAMENTO_INVALIDA);
        int parcelas = Objects.requireNonNullElse(pedido.parcelas(), 1);
        exigir(formaPagamento.aceitaParcelas(parcelas), PARCELAMENTO_INVALIDO);
        exigir(formaPagamento.atende(totalPedido), FORMA_PAGAMENTO_INDISPONIVEL);
        Cobranca cobranca = formaPagamento.cobrar(totalPedido, parcelas);

        return new ResumoResponse(
                subtotal,
                descontoCupom,
                frete,
                modalidade.prazoDias(),
                seguro,
                cobranca.totalFinal().subtract(totalPedido),
                cobranca.totalFinal(),
                parcelas,
                cobranca.valorParcela(),
                nivel.creditoProximaCompra(subtotal),
                nivel.ganhaBrinde(subtotal));
    }

    private static Carrinho carrinho(PedidoRequest pedido) {
        List<PedidoRequest.ItemRequest> itens = pedido.itens();
        exigir(itens != null && !itens.isEmpty() && itens.stream().allMatch(CheckoutService::itemValido), PEDIDO_INVALIDO);
        return new Carrinho(itens.stream()
                .map(i -> new Item(i.nome(), i.precoUnitario(), i.quantidade(), i.pesoKg()))
                .toList());
    }

    private static boolean itemValido(PedidoRequest.ItemRequest item) {
        return item != null
                && positivo(item.precoUnitario())
                && item.quantidade() != null && item.quantidade() > 0
                && positivo(item.pesoKg());
    }

    private static boolean positivo(BigDecimal valor) {
        return valor != null && valor.signum() > 0;
    }

    private static void exigir(boolean condicao, CodigoErro erro) {
        if (!condicao) {
            throw new PedidoRecusadoException(erro);
        }
    }
}

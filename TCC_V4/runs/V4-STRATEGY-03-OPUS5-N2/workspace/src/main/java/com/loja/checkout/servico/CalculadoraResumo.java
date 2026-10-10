package com.loja.checkout.servico;

import com.loja.checkout.api.CodigoErro;
import com.loja.checkout.api.ItemRequest;
import com.loja.checkout.api.PedidoRecusadoException;
import com.loja.checkout.api.ResumoRequest;
import com.loja.checkout.api.ResumoResponse;
import com.loja.checkout.dominio.Carrinho;
import com.loja.checkout.dominio.Catalogo;
import com.loja.checkout.dominio.Cupom;
import com.loja.checkout.dominio.Dinheiro;
import com.loja.checkout.dominio.Entrega;
import com.loja.checkout.dominio.FormaPagamento;
import com.loja.checkout.dominio.Item;
import com.loja.checkout.dominio.ModalidadeEntrega;
import com.loja.checkout.dominio.NivelClube;
import com.loja.checkout.dominio.Pagamento;
import com.loja.checkout.dominio.Regiao;
import java.math.BigDecimal;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import org.springframework.stereotype.Service;

/**
 * Monta o resumo da compra. A sequencia do calculo e a mesma para todo pedido;
 * o que muda de caso para caso (entrega, cupom, clube, regiao, pagamento) e
 * respondido pela propria opcao escolhida.
 */
@Service
public class CalculadoraResumo {

    private static final int PARCELA_UNICA = 1;

    public ResumoResponse calcular(ResumoRequest pedido) {
        Carrinho carrinho = montarCarrinho(pedido.itens());
        NivelClube nivel = exigir(NivelClube.class, pedido.nivelClube(), CodigoErro.NIVEL_CLUBE_INVALIDO);
        Regiao regiao = exigir(Regiao.class, pedido.regiao(), CodigoErro.REGIAO_INVALIDA);

        Entrega entrega = cotar(carrinho, pedido.modalidadeEntrega());
        BigDecimal frete = nivel.freteACobrar(entrega.frete());

        BigDecimal subtotalProdutos = carrinho.subtotalProdutos();
        BigDecimal descontoCupom = descontar(carrinho, frete, pedido.cupom());

        FormaPagamento formaPagamento =
                exigir(FormaPagamento.class, pedido.formaPagamento(), CodigoErro.FORMA_PAGAMENTO_INVALIDA);
        int parcelas = Objects.requireNonNullElse(pedido.parcelas(), PARCELA_UNICA);
        recusarSe(!formaPagamento.aceitaParcelas(parcelas), CodigoErro.PARCELAMENTO_INVALIDO);

        BigDecimal seguro = regiao.seguro(subtotalProdutos);
        BigDecimal totalPedido = Dinheiro.arredondar(
                subtotalProdutos.subtract(descontoCupom).add(frete).add(seguro));
        recusarSe(!formaPagamento.atende(totalPedido), CodigoErro.FORMA_PAGAMENTO_INDISPONIVEL);

        Pagamento pagamento = formaPagamento.cobrar(totalPedido, parcelas);

        return new ResumoResponse(
                subtotalProdutos,
                descontoCupom,
                frete,
                entrega.prazoDias(),
                seguro,
                pagamento.totalFinal().subtract(totalPedido),
                pagamento.totalFinal(),
                parcelas,
                pagamento.valorParcela(),
                nivel.credito(subtotalProdutos),
                nivel.ganhaBrinde(subtotalProdutos));
    }

    private Carrinho montarCarrinho(List<ItemRequest> itens) {
        recusarSe(itens == null || itens.isEmpty(), CodigoErro.PEDIDO_INVALIDO);
        return new Carrinho(itens.stream().map(this::validarItem).toList());
    }

    private Item validarItem(ItemRequest item) {
        recusarSe(item == null, CodigoErro.PEDIDO_INVALIDO);
        recusarSe(naoEPositivo(item.precoUnitario()), CodigoErro.PEDIDO_INVALIDO);
        recusarSe(naoEPositivo(item.pesoKg()), CodigoErro.PEDIDO_INVALIDO);
        recusarSe(item.quantidade() == null || item.quantidade() <= 0, CodigoErro.PEDIDO_INVALIDO);
        return new Item(item.nome(), item.precoUnitario(), item.quantidade(), item.pesoKg());
    }

    private boolean naoEPositivo(BigDecimal valor) {
        return valor == null || valor.compareTo(BigDecimal.ZERO) <= 0;
    }

    private Entrega cotar(Carrinho carrinho, String codigoModalidade) {
        ModalidadeEntrega modalidade =
                exigir(ModalidadeEntrega.class, codigoModalidade, CodigoErro.MODALIDADE_INVALIDA);
        BigDecimal pesoKg = carrinho.pesoKg();
        recusarSe(!modalidade.atende(pesoKg), CodigoErro.MODALIDADE_INDISPONIVEL);
        return modalidade.cotar(pesoKg);
    }

    private BigDecimal descontar(Carrinho carrinho, BigDecimal frete, String codigoCupom) {
        if (codigoCupom == null || codigoCupom.isBlank()) {
            return Dinheiro.ZERO;
        }
        Cupom cupom = exigir(Cupom.class, codigoCupom, CodigoErro.CUPOM_INVALIDO);
        recusarSe(!cupom.aplicavel(carrinho, frete), CodigoErro.CUPOM_NAO_APLICAVEL);
        return cupom.desconto(carrinho, frete);
    }

    private <E extends Enum<E>> E exigir(Class<E> opcoes, String codigo, CodigoErro erro) {
        return Optional.ofNullable(codigo)
                .flatMap(informado -> Catalogo.buscar(opcoes, informado))
                .orElseThrow(() -> new PedidoRecusadoException(erro));
    }

    private void recusarSe(boolean problema, CodigoErro erro) {
        if (problema) {
            throw new PedidoRecusadoException(erro);
        }
    }
}

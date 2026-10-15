package com.loja.checkout.calculo;

import com.loja.checkout.clube.CatalogoNiveisClube;
import com.loja.checkout.clube.NivelClube;
import com.loja.checkout.cupom.CatalogoCupons;
import com.loja.checkout.cupom.Cupom;
import com.loja.checkout.dominio.Dinheiro;
import com.loja.checkout.dominio.Item;
import com.loja.checkout.dominio.Pedido;
import com.loja.checkout.entrega.CatalogoEntregas;
import com.loja.checkout.entrega.ModalidadeEntrega;
import com.loja.checkout.pagamento.CatalogoFormasPagamento;
import com.loja.checkout.pagamento.FormaPagamento;
import com.loja.checkout.pagamento.ResultadoPagamento;
import com.loja.checkout.regiao.CatalogoRegioes;
import com.loja.checkout.regiao.Regiao;
import com.loja.checkout.web.ItemRequest;
import com.loja.checkout.web.ResumoRequest;
import com.loja.checkout.web.ResumoResponse;
import java.math.BigDecimal;
import java.util.List;
import java.util.Objects;
import org.springframework.stereotype.Service;

/**
 * Calcula o resumo da compra: confere o pedido e monta cada parte do valor final,
 * na ordem produtos -> cupom -> frete -> seguro -> total -> ajuste do pagamento.
 */
@Service
public class CalculadoraResumo {

    private final CatalogoNiveisClube niveisClube;
    private final CatalogoRegioes regioes;
    private final CatalogoEntregas entregas;
    private final CatalogoCupons cupons;
    private final CatalogoFormasPagamento formasPagamento;

    public CalculadoraResumo(CatalogoNiveisClube niveisClube,
                             CatalogoRegioes regioes,
                             CatalogoEntregas entregas,
                             CatalogoCupons cupons,
                             CatalogoFormasPagamento formasPagamento) {
        this.niveisClube = niveisClube;
        this.regioes = regioes;
        this.entregas = entregas;
        this.cupons = cupons;
        this.formasPagamento = formasPagamento;
    }

    public ResumoResponse calcular(ResumoRequest requisicao) {
        ResumoRequest pedidoRecebido = requisicao == null
                ? new ResumoRequest(null, null, null, null, null, null, null)
                : requisicao;

        // 1 a 4: conferir o que o site mandou, na ordem combinada.
        List<Item> itens = itensConferidos(pedidoRecebido.itens());
        NivelClube nivelClube = niveisClube.buscar(pedidoRecebido.nivelClube())
                .orElseThrow(() -> new PedidoRecusadoException("NIVEL_CLUBE_INVALIDO"));
        Regiao regiao = regioes.buscar(pedidoRecebido.regiao())
                .orElseThrow(() -> new PedidoRecusadoException("REGIAO_INVALIDA"));
        ModalidadeEntrega modalidade = entregas.buscar(pedidoRecebido.modalidadeEntrega())
                .orElseThrow(() -> new PedidoRecusadoException("MODALIDADE_INVALIDA"));

        Pedido pedido = new Pedido(itens, nivelClube, regiao);

        // 5: a modalidade existe, mas precisa atender este pedido.
        if (!modalidade.atende(pedido)) {
            throw new PedidoRecusadoException("MODALIDADE_INDISPONIVEL");
        }

        BigDecimal subtotalProdutos = pedido.subtotalProdutos();
        BigDecimal frete = nivelClube.freteGratis() ? Dinheiro.ZERO : modalidade.frete(pedido);

        // 6 e 7: o cupom, se o cliente usou um.
        BigDecimal descontoCupom = descontoDoCupom(pedidoRecebido.cupom(), pedido, frete);

        BigDecimal seguro = modalidade.temSeguro()
                ? Dinheiro.percentual(subtotalProdutos, regiao.percentualSeguro())
                : Dinheiro.ZERO;

        BigDecimal totalPedido = Dinheiro.centavos(subtotalProdutos
                .subtract(descontoCupom)
                .add(frete)
                .add(seguro));

        // 8 a 10: a forma de pagamento.
        FormaPagamento formaPagamento = formasPagamento.buscar(pedidoRecebido.formaPagamento())
                .orElseThrow(() -> new PedidoRecusadoException("FORMA_PAGAMENTO_INVALIDA"));
        int parcelas = pedidoRecebido.parcelas() == null ? 1 : pedidoRecebido.parcelas();
        if (!formaPagamento.aceitaParcelas(parcelas)) {
            throw new PedidoRecusadoException("PARCELAMENTO_INVALIDO");
        }
        if (!formaPagamento.atende(totalPedido)) {
            throw new PedidoRecusadoException("FORMA_PAGAMENTO_INDISPONIVEL");
        }

        ResultadoPagamento pagamento = formaPagamento.calcular(totalPedido, parcelas, nivelClube);
        BigDecimal ajustePagamento = Dinheiro.centavos(pagamento.totalFinal().subtract(totalPedido));

        return new ResumoResponse(
                subtotalProdutos,
                descontoCupom,
                frete,
                modalidade.prazoDias() + formaPagamento.diasAdicionais(),
                seguro,
                ajustePagamento,
                pagamento.totalFinal(),
                parcelas,
                pagamento.valorParcela(),
                Dinheiro.percentual(subtotalProdutos, nivelClube.percentualCredito()),
                nivelClube.temBrinde(subtotalProdutos));
    }

    /** O carrinho precisa ter itens, e todo item precisa de preco, quantidade e peso positivos. */
    private List<Item> itensConferidos(List<ItemRequest> itens) {
        if (itens == null || itens.isEmpty() || itens.stream().anyMatch(Objects::isNull)) {
            throw new PedidoRecusadoException("PEDIDO_INVALIDO");
        }
        return itens.stream().map(this::itemConferido).toList();
    }

    private Item itemConferido(ItemRequest item) {
        if (!positivo(item.precoUnitario()) || !positivo(item.pesoKg())
                || item.quantidade() == null || item.quantidade() <= 0) {
            throw new PedidoRecusadoException("PEDIDO_INVALIDO");
        }
        return new Item(item.nome(), item.precoUnitario(), item.quantidade(), item.pesoKg());
    }

    private boolean positivo(BigDecimal valor) {
        return valor != null && valor.compareTo(BigDecimal.ZERO) > 0;
    }

    private BigDecimal descontoDoCupom(String codigo, Pedido pedido, BigDecimal frete) {
        if (codigo == null || codigo.isBlank()) {
            return Dinheiro.ZERO;
        }
        Cupom cupom = cupons.buscar(codigo)
                .orElseThrow(() -> new PedidoRecusadoException("CUPOM_INVALIDO"));
        if (!cupom.aplicavel(pedido, frete)) {
            throw new PedidoRecusadoException("CUPOM_NAO_APLICAVEL");
        }
        return Dinheiro.centavos(cupom.desconto(pedido, frete));
    }
}

package com.loja.checkout.aplicacao;

import com.loja.checkout.clube.CatalogoDoClube;
import com.loja.checkout.clube.NivelClube;
import com.loja.checkout.cupom.CatalogoDeCupons;
import com.loja.checkout.cupom.ContextoCupom;
import com.loja.checkout.cupom.Cupom;
import com.loja.checkout.cupom.SemCupom;
import com.loja.checkout.dominio.CodigoErro;
import com.loja.checkout.dominio.Dinheiro;
import com.loja.checkout.dominio.ItemPedido;
import com.loja.checkout.dominio.PedidoRecusadoException;
import com.loja.checkout.dominio.Regiao;
import com.loja.checkout.entrega.CatalogoDeEntregas;
import com.loja.checkout.entrega.Frete;
import com.loja.checkout.entrega.ModalidadeEntrega;
import com.loja.checkout.pagamento.CatalogoDePagamentos;
import com.loja.checkout.pagamento.FormaPagamento;
import com.loja.checkout.pagamento.ResultadoPagamento;
import java.math.BigDecimal;
import java.util.List;
import java.util.Objects;
import java.util.function.Function;
import org.springframework.stereotype.Service;

/**
 * Sabe a ordem do cálculo e a ordem das validações. Não sabe nada de nenhuma
 * modalidade, cupom, nível ou forma de pagamento em particular.
 */
@Service
public class CalculadoraResumo {

    private static final int PARCELAS_PADRAO = 1;

    private final CatalogoDeEntregas entregas;
    private final CatalogoDeCupons cupons;
    private final CatalogoDoClube clube;
    private final CatalogoDePagamentos pagamentos;

    public CalculadoraResumo(CatalogoDeEntregas entregas, CatalogoDeCupons cupons, CatalogoDoClube clube,
            CatalogoDePagamentos pagamentos) {
        this.entregas = entregas;
        this.cupons = cupons;
        this.clube = clube;
        this.pagamentos = pagamentos;
    }

    public ResumoResponse calcular(PedidoRequest pedido) {
        List<ItemPedido> itens = itensValidos(pedido);
        BigDecimal subtotalProdutos = Dinheiro.centavos(somar(itens, ItemPedido::total));
        BigDecimal pesoKg = somar(itens, ItemPedido::peso);

        NivelClube nivel = clube.buscar(pedido.nivelClube())
                .orElseThrow(() -> recusar(CodigoErro.NIVEL_CLUBE_INVALIDO));
        Regiao regiao = Regiao.porNome(pedido.regiao())
                .orElseThrow(() -> recusar(CodigoErro.REGIAO_INVALIDA));

        ModalidadeEntrega modalidade = entregas.buscar(pedido.modalidadeEntrega())
                .orElseThrow(() -> recusar(CodigoErro.MODALIDADE_INVALIDA));
        if (!modalidade.atende(pesoKg)) {
            throw recusar(CodigoErro.MODALIDADE_INDISPONIVEL);
        }
        Frete frete = modalidade.calcular(pesoKg);
        BigDecimal valorFrete = nivel.freteGratis() ? Dinheiro.ZERO : Dinheiro.centavos(frete.valor());

        Cupom cupom = escolherCupom(pedido.cupom());
        ContextoCupom contexto = new ContextoCupom(itens, subtotalProdutos, valorFrete);
        if (!cupom.aplicavel(contexto)) {
            throw recusar(CodigoErro.CUPOM_NAO_APLICAVEL);
        }
        BigDecimal descontoCupom = Dinheiro.centavos(cupom.desconto(contexto));

        BigDecimal seguro = regiao.seguro(subtotalProdutos);
        BigDecimal totalPedido = Dinheiro.centavos(
                subtotalProdutos.subtract(descontoCupom).add(valorFrete).add(seguro));

        FormaPagamento forma = pagamentos.buscar(pedido.formaPagamento())
                .orElseThrow(() -> recusar(CodigoErro.FORMA_PAGAMENTO_INVALIDA));
        int parcelas = Objects.requireNonNullElse(pedido.parcelas(), PARCELAS_PADRAO);
        if (!forma.aceitaParcelas(parcelas)) {
            throw recusar(CodigoErro.PARCELAMENTO_INVALIDO);
        }
        if (!forma.atende(totalPedido)) {
            throw recusar(CodigoErro.FORMA_PAGAMENTO_INDISPONIVEL);
        }
        ResultadoPagamento pagamento = forma.calcular(totalPedido, parcelas);

        return new ResumoResponse(
                subtotalProdutos,
                descontoCupom,
                valorFrete,
                frete.prazoDias(),
                seguro,
                Dinheiro.centavos(pagamento.totalFinal().subtract(totalPedido)),
                pagamento.totalFinal(),
                parcelas,
                pagamento.valorParcela(),
                nivel.credito(subtotalProdutos),
                nivel.brinde(subtotalProdutos));
    }

    private List<ItemPedido> itensValidos(PedidoRequest pedido) {
        if (pedido == null || pedido.itens() == null || pedido.itens().isEmpty()) {
            throw recusar(CodigoErro.PEDIDO_INVALIDO);
        }
        return pedido.itens().stream().map(this::itemValido).toList();
    }

    private ItemPedido itemValido(PedidoRequest.ItemRequest item) {
        if (item == null || !positivo(item.precoUnitario()) || !positivo(item.pesoKg())
                || item.quantidade() == null || item.quantidade() <= 0) {
            throw recusar(CodigoErro.PEDIDO_INVALIDO);
        }
        return new ItemPedido(item.nome(), item.precoUnitario(), item.quantidade(), item.pesoKg());
    }

    private boolean positivo(BigDecimal valor) {
        return valor != null && valor.compareTo(BigDecimal.ZERO) > 0;
    }

    private Cupom escolherCupom(String codigo) {
        if (codigo == null || codigo.isBlank()) {
            return SemCupom.INSTANCIA;
        }
        return cupons.buscar(codigo).orElseThrow(() -> recusar(CodigoErro.CUPOM_INVALIDO));
    }

    private BigDecimal somar(List<ItemPedido> itens, Function<ItemPedido, BigDecimal> medida) {
        return itens.stream().map(medida).reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private PedidoRecusadoException recusar(CodigoErro codigo) {
        return new PedidoRecusadoException(codigo);
    }
}

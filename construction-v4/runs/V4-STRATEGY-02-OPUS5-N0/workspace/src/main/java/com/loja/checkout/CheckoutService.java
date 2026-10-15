package com.loja.checkout;

import com.loja.checkout.clube.NivelClube;
import com.loja.checkout.cupom.ContextoCupom;
import com.loja.checkout.cupom.Cupom;
import com.loja.checkout.dominio.Carrinho;
import com.loja.checkout.dominio.Dinheiro;
import com.loja.checkout.dominio.ErroPedido;
import com.loja.checkout.dominio.ItemPedido;
import com.loja.checkout.dominio.PedidoRecusadoException;
import com.loja.checkout.dominio.Regiao;
import com.loja.checkout.dominio.ResumoCompra;
import com.loja.checkout.entrega.ContextoEntrega;
import com.loja.checkout.entrega.ModalidadeEntrega;
import com.loja.checkout.pagamento.FormaPagamento;
import com.loja.checkout.pagamento.ResultadoPagamento;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;

/**
 * Calcula o resumo da compra. As opções de entrega, cupons, níveis do clube e
 * formas de pagamento são descobertos pelo Spring: o cálculo abaixo não precisa
 * saber quais existem.
 */
@Service
public class CheckoutService {

    private final Map<String, ModalidadeEntrega> modalidades;
    private final Map<String, Cupom> cupons;
    private final Map<String, NivelClube> niveisClube;
    private final Map<String, FormaPagamento> formasPagamento;

    public CheckoutService(List<ModalidadeEntrega> modalidades,
                           List<Cupom> cupons,
                           List<NivelClube> niveisClube,
                           List<FormaPagamento> formasPagamento) {
        this.modalidades = indexar(modalidades, ModalidadeEntrega::codigo);
        this.cupons = indexar(cupons, Cupom::codigo);
        this.niveisClube = indexar(niveisClube, NivelClube::codigo);
        this.formasPagamento = indexar(formasPagamento, FormaPagamento::codigo);
    }

    private static <T> Map<String, T> indexar(List<T> itens, Function<T, String> codigo) {
        return itens.stream().collect(Collectors.toUnmodifiableMap(codigo, Function.identity()));
    }

    public ResumoCompra calcular(PedidoCheckout pedido) {
        Carrinho carrinho = validarCarrinho(pedido.itens());
        NivelClube nivelClube = buscar(niveisClube, pedido.nivelClube(), ErroPedido.NIVEL_CLUBE_INVALIDO);
        Regiao regiao = Regiao.porCodigo(pedido.regiao())
                .orElseThrow(() -> new PedidoRecusadoException(ErroPedido.REGIAO_INVALIDA));

        ModalidadeEntrega modalidade =
                buscar(modalidades, pedido.modalidadeEntrega(), ErroPedido.MODALIDADE_INVALIDA);
        ContextoEntrega contextoEntrega = new ContextoEntrega(carrinho, regiao, nivelClube);
        if (!modalidade.atende(contextoEntrega)) {
            throw new PedidoRecusadoException(ErroPedido.MODALIDADE_INDISPONIVEL);
        }

        BigDecimal subtotalProdutos = carrinho.subtotalProdutos();
        BigDecimal frete = nivelClube.freteGratis()
                ? Dinheiro.ZERO
                : Dinheiro.arredondar(modalidade.frete(contextoEntrega));

        BigDecimal descontoCupom = Dinheiro.ZERO;
        if (temCupom(pedido.cupom())) {
            Cupom cupom = buscar(cupons, pedido.cupom(), ErroPedido.CUPOM_INVALIDO);
            ContextoCupom contextoCupom = new ContextoCupom(carrinho, frete);
            if (!cupom.aplicavel(contextoCupom)) {
                throw new PedidoRecusadoException(ErroPedido.CUPOM_NAO_APLICAVEL);
            }
            descontoCupom = Dinheiro.arredondar(cupom.desconto(contextoCupom));
        }

        FormaPagamento formaPagamento =
                buscar(formasPagamento, pedido.formaPagamento(), ErroPedido.FORMA_PAGAMENTO_INVALIDA);
        int parcelas = pedido.parcelas() == null ? 1 : pedido.parcelas();
        if (!formaPagamento.aceitaParcelas(parcelas)) {
            throw new PedidoRecusadoException(ErroPedido.PARCELAMENTO_INVALIDO);
        }

        BigDecimal seguro = Dinheiro.percentual(subtotalProdutos, regiao.percentualSeguro());
        BigDecimal totalPedido = Dinheiro.arredondar(
                subtotalProdutos.subtract(descontoCupom).add(frete).add(seguro));

        if (!formaPagamento.atende(totalPedido)) {
            throw new PedidoRecusadoException(ErroPedido.FORMA_PAGAMENTO_INDISPONIVEL);
        }

        ResultadoPagamento resultado = formaPagamento.calcular(totalPedido, parcelas);
        BigDecimal totalFinal = Dinheiro.arredondar(resultado.totalFinal());
        BigDecimal ajustePagamento = Dinheiro.arredondar(totalFinal.subtract(totalPedido));

        return new ResumoCompra(
                subtotalProdutos,
                descontoCupom,
                frete,
                modalidade.prazoDias(),
                seguro,
                ajustePagamento,
                totalFinal,
                parcelas,
                Dinheiro.arredondar(resultado.valorParcela()),
                nivelClube.creditoProximaCompra(subtotalProdutos),
                nivelClube.brinde(subtotalProdutos));
    }

    private Carrinho validarCarrinho(List<ItemPedidoCheckout> itens) {
        if (itens == null || itens.isEmpty()) {
            throw new PedidoRecusadoException(ErroPedido.PEDIDO_INVALIDO);
        }
        List<ItemPedido> validados = itens.stream().map(CheckoutService::validarItem).toList();
        return new Carrinho(validados);
    }

    private static ItemPedido validarItem(ItemPedidoCheckout item) {
        if (item == null
                || !positivo(item.precoUnitario())
                || item.quantidade() == null || item.quantidade() <= 0
                || !positivo(item.pesoKg())) {
            throw new PedidoRecusadoException(ErroPedido.PEDIDO_INVALIDO);
        }
        return new ItemPedido(item.nome(), item.precoUnitario(), item.quantidade(), item.pesoKg());
    }

    private static boolean positivo(BigDecimal valor) {
        return valor != null && valor.compareTo(BigDecimal.ZERO) > 0;
    }

    private static boolean temCupom(String codigo) {
        return codigo != null && !codigo.isBlank();
    }

    private static <T> T buscar(Map<String, T> disponiveis, String codigo, ErroPedido erro) {
        T encontrado = codigo == null ? null : disponiveis.get(codigo);
        if (encontrado == null) {
            throw new PedidoRecusadoException(erro);
        }
        return encontrado;
    }
}

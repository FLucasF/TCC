package br.com.loja.checkout.dominio;

import static br.com.loja.checkout.dominio.CodigoErro.*;

import br.com.loja.checkout.api.PedidoRequest;
import br.com.loja.checkout.api.PedidoRequest.ItemRequest;
import br.com.loja.checkout.api.ResumoResponse;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;

@Service
public class CheckoutService {

    private final Map<String, Entrega> entregas;
    private final Map<String, Cupom> cupons;
    private final Map<String, NivelClube> niveis;
    private final Map<String, FormaPagamento> pagamentos;

    public CheckoutService(List<Entrega> entregas, List<Cupom> cupons, List<NivelClube> niveis,
                           List<FormaPagamento> pagamentos) {
        this.entregas = porCodigo(entregas, Entrega::codigo);
        this.cupons = porCodigo(cupons, Cupom::codigo);
        this.niveis = porCodigo(niveis, NivelClube::codigo);
        this.pagamentos = porCodigo(pagamentos, FormaPagamento::codigo);
    }

    public ResumoResponse resumir(PedidoRequest pedido) {
        List<Item> itens = validarItens(pedido.itens());
        NivelClube nivel = buscar(niveis, pedido.nivelClube(), NIVEL_CLUBE_INVALIDO);
        Regiao regiao = buscarRegiao(pedido.regiao());
        Entrega entrega = buscar(entregas, pedido.modalidadeEntrega(), MODALIDADE_INVALIDA);

        BigDecimal subtotal = itens.stream().map(Item::total).reduce(Dinheiro.ZERO, BigDecimal::add);
        BigDecimal peso = itens.stream().map(Item::pesoTotal).reduce(BigDecimal.ZERO, BigDecimal::add);
        if (!entrega.atende(peso)) {
            throw new PedidoRecusadoException(MODALIDADE_INDISPONIVEL);
        }
        BigDecimal frete = nivel.freteGratis() ? Dinheiro.ZERO : entrega.frete(peso);

        BigDecimal desconto = Dinheiro.ZERO;
        if (pedido.cupom() != null) {
            Cupom cupom = buscar(cupons, pedido.cupom(), CUPOM_INVALIDO);
            Cupom.Contexto contexto = new Cupom.Contexto(itens, subtotal, frete);
            if (!cupom.aplicavel(contexto)) {
                throw new PedidoRecusadoException(CUPOM_NAO_APLICAVEL);
            }
            desconto = cupom.desconto(contexto);
        }

        FormaPagamento pagamento = buscar(pagamentos, pedido.formaPagamento(), FORMA_PAGAMENTO_INVALIDA);
        int parcelas = pedido.parcelas() == null ? 1 : pedido.parcelas();
        if (!pagamento.aceitaParcelas(parcelas)) {
            throw new PedidoRecusadoException(PARCELAMENTO_INVALIDO);
        }

        BigDecimal seguro = regiao.seguro(subtotal);
        BigDecimal totalPedido = subtotal.subtract(desconto).add(frete).add(seguro);
        if (!pagamento.disponivel(totalPedido)) {
            throw new PedidoRecusadoException(FORMA_PAGAMENTO_INDISPONIVEL);
        }
        FormaPagamento.Cobranca cobranca = pagamento.cobrar(totalPedido, parcelas);

        return new ResumoResponse(
                subtotal,
                desconto,
                frete,
                entrega.prazoDias(),
                seguro,
                cobranca.totalFinal().subtract(totalPedido),
                cobranca.totalFinal(),
                parcelas,
                cobranca.valorParcela(),
                nivel.credito(subtotal),
                nivel.ganhaBrinde(subtotal));
    }

    private static List<Item> validarItens(List<ItemRequest> itens) {
        if (itens == null || itens.isEmpty()) {
            throw new PedidoRecusadoException(PEDIDO_INVALIDO);
        }
        return itens.stream().map(CheckoutService::validarItem).toList();
    }

    private static Item validarItem(ItemRequest i) {
        if (i == null || i.precoUnitario() == null || i.quantidade() == null || i.pesoKg() == null
                || i.precoUnitario().signum() <= 0 || i.quantidade() <= 0 || i.pesoKg().signum() <= 0) {
            throw new PedidoRecusadoException(PEDIDO_INVALIDO);
        }
        return new Item(i.nome(), i.precoUnitario(), i.quantidade(), i.pesoKg());
    }

    private static Regiao buscarRegiao(String codigo) {
        try {
            return Regiao.valueOf(codigo);
        } catch (IllegalArgumentException | NullPointerException e) {
            throw new PedidoRecusadoException(REGIAO_INVALIDA);
        }
    }

    private static <T> T buscar(Map<String, T> mapa, String codigo, CodigoErro erro) {
        T valor = codigo == null ? null : mapa.get(codigo);
        if (valor == null) {
            throw new PedidoRecusadoException(erro);
        }
        return valor;
    }

    private static <T> Map<String, T> porCodigo(List<T> lista, Function<T, String> codigo) {
        return lista.stream().collect(Collectors.toUnmodifiableMap(codigo, Function.identity()));
    }
}

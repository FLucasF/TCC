package br.com.loja.checkout;

import br.com.loja.checkout.api.ItemRequest;
import br.com.loja.checkout.api.ResumoRequest;
import br.com.loja.checkout.api.ResumoResponse;
import br.com.loja.checkout.clube.NivelClube;
import br.com.loja.checkout.cupom.ContextoCupom;
import br.com.loja.checkout.cupom.Cupom;
import br.com.loja.checkout.dominio.CheckoutException;
import br.com.loja.checkout.dominio.CodigoErro;
import br.com.loja.checkout.dominio.Dinheiro;
import br.com.loja.checkout.dominio.Item;
import br.com.loja.checkout.dominio.Pedido;
import br.com.loja.checkout.dominio.Regiao;
import br.com.loja.checkout.entrega.ModalidadeEntrega;
import br.com.loja.checkout.pagamento.FormaPagamento;
import br.com.loja.checkout.pagamento.Pagamento;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Calcula o resumo da compra. As opções de entrega, cupons, níveis do clube e formas de
 * pagamento são descobertas automaticamente entre as classes anotadas com {@code @Component}.
 */
@Service
public class CalculadoraResumo {

    private final Map<String, ModalidadeEntrega> modalidades;
    private final Map<String, Cupom> cupons;
    private final Map<String, NivelClube> niveis;
    private final Map<String, FormaPagamento> formasPagamento;

    public CalculadoraResumo(List<ModalidadeEntrega> modalidades, List<Cupom> cupons,
                             List<NivelClube> niveis, List<FormaPagamento> formasPagamento) {
        this.modalidades = porCodigo(modalidades, ModalidadeEntrega::codigo);
        this.cupons = porCodigo(cupons, Cupom::codigo);
        this.niveis = porCodigo(niveis, NivelClube::codigo);
        this.formasPagamento = porCodigo(formasPagamento, FormaPagamento::codigo);
    }

    public ResumoResponse calcular(ResumoRequest request) {
        Pedido pedido = lerPedido(request.itens());
        NivelClube nivel = buscar(niveis, request.nivelClube(), CodigoErro.NIVEL_CLUBE_INVALIDO);
        Regiao regiao = Regiao.porCodigo(request.regiao())
                .orElseThrow(() -> new CheckoutException(CodigoErro.REGIAO_INVALIDA));

        ModalidadeEntrega modalidade = buscar(modalidades, request.modalidadeEntrega(), CodigoErro.MODALIDADE_INVALIDA);
        exigir(modalidade.atende(pedido), CodigoErro.MODALIDADE_INDISPONIVEL);

        BigDecimal subtotal = pedido.subtotalProdutos();
        BigDecimal frete = nivel.isentoDeFrete() ? Dinheiro.ZERO : modalidade.frete(pedido);

        BigDecimal descontoCupom = Dinheiro.ZERO;
        if (request.cupom() != null) {
            Cupom cupom = buscar(cupons, request.cupom(), CodigoErro.CUPOM_INVALIDO);
            ContextoCupom contexto = new ContextoCupom(pedido, frete);
            exigir(cupom.aplicavel(contexto), CodigoErro.CUPOM_NAO_APLICAVEL);
            descontoCupom = cupom.desconto(contexto);
        }

        FormaPagamento formaPagamento = buscar(formasPagamento, request.formaPagamento(),
                CodigoErro.FORMA_PAGAMENTO_INVALIDA);
        int parcelas = lerParcelas(request.parcelas());
        exigir(formaPagamento.permiteParcelas(parcelas), CodigoErro.PARCELAMENTO_INVALIDO);

        BigDecimal seguro = regiao.seguro(pedido);
        BigDecimal totalPedido = subtotal.subtract(descontoCupom).add(frete).add(seguro);
        exigir(formaPagamento.atende(totalPedido), CodigoErro.FORMA_PAGAMENTO_INDISPONIVEL);

        Pagamento pagamento = formaPagamento.calcular(totalPedido, parcelas);

        return new ResumoResponse(
                subtotal,
                descontoCupom,
                frete,
                modalidade.prazoDias(),
                seguro,
                pagamento.totalFinal().subtract(totalPedido),
                pagamento.totalFinal(),
                pagamento.parcelas(),
                pagamento.valorParcela(),
                nivel.credito(pedido),
                nivel.ganhaBrinde(pedido));
    }

    private static Pedido lerPedido(List<ItemRequest> itens) {
        exigir(itens != null && !itens.isEmpty(), CodigoErro.PEDIDO_INVALIDO);
        List<Item> lidos = new ArrayList<>();
        for (ItemRequest item : itens) {
            exigir(item != null
                    && positivo(item.precoUnitario())
                    && positivo(item.quantidade())
                    && positivo(item.pesoKg()), CodigoErro.PEDIDO_INVALIDO);
            int quantidade = inteiro(item.quantidade())
                    .orElseThrow(() -> new CheckoutException(CodigoErro.PEDIDO_INVALIDO));
            lidos.add(new Item(item.nome(), item.precoUnitario(), quantidade, item.pesoKg()));
        }
        return new Pedido(lidos);
    }

    /** Parcelas não informadas valem 1; valores não inteiros nunca são permitidos. */
    private static int lerParcelas(BigDecimal parcelas) {
        if (parcelas == null) {
            return 1;
        }
        return inteiro(parcelas).orElseThrow(() -> new CheckoutException(CodigoErro.PARCELAMENTO_INVALIDO));
    }

    private static boolean positivo(BigDecimal valor) {
        return valor != null && valor.signum() > 0;
    }

    private static Optional<Integer> inteiro(BigDecimal valor) {
        try {
            return Optional.of(valor.intValueExact());
        } catch (ArithmeticException e) {
            return Optional.empty();
        }
    }

    private static <T> T buscar(Map<String, T> opcoes, String codigo, CodigoErro erro) {
        T opcao = codigo == null ? null : opcoes.get(codigo);
        exigir(opcao != null, erro);
        return opcao;
    }

    private static void exigir(boolean condicao, CodigoErro erro) {
        if (!condicao) {
            throw new CheckoutException(erro);
        }
    }

    private static <T> Map<String, T> porCodigo(List<T> opcoes, Function<T, String> codigo) {
        return opcoes.stream().collect(Collectors.toUnmodifiableMap(codigo, Function.identity()));
    }
}

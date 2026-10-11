package br.com.loja.checkout.aplicacao;

import static br.com.loja.checkout.dominio.Dinheiro.centavos;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;

import org.springframework.stereotype.Service;

import br.com.loja.checkout.api.ResumoCompraRequest;
import br.com.loja.checkout.api.ResumoCompraRequest.ItemRequest;
import br.com.loja.checkout.api.ResumoCompraResponse;
import br.com.loja.checkout.cupom.Cupom;
import br.com.loja.checkout.dominio.CalculadoraPagamento;
import br.com.loja.checkout.dominio.CodigoErro;
import br.com.loja.checkout.dominio.Dinheiro;
import br.com.loja.checkout.dominio.ErroPedidoException;
import br.com.loja.checkout.dominio.FormaPagamento;
import br.com.loja.checkout.dominio.ItemCompra;
import br.com.loja.checkout.dominio.NivelClube;
import br.com.loja.checkout.dominio.Pagamento;
import br.com.loja.checkout.dominio.Regiao;
import br.com.loja.checkout.entrega.Entrega;

@Service
public class ResumoCompraService {

    private final List<Entrega> entregas;
    private final List<Cupom> cupons;

    public ResumoCompraService(List<Entrega> entregas, List<Cupom> cupons) {
        this.entregas = entregas;
        this.cupons = cupons;
    }

    public ResumoCompraResponse calcular(ResumoCompraRequest pedido) {
        List<ItemCompra> itens = itens(pedido.itens());
        BigDecimal subtotal = itens.stream().map(ItemCompra::subtotal).reduce(Dinheiro.ZERO, BigDecimal::add);
        BigDecimal peso = itens.stream().map(ItemCompra::peso).reduce(BigDecimal.ZERO, BigDecimal::add);

        NivelClube nivel = buscarEnum(NivelClube.class, pedido.nivelClube(), CodigoErro.NIVEL_CLUBE_INVALIDO);
        Regiao regiao = buscarEnum(Regiao.class, pedido.regiao(), CodigoErro.REGIAO_INVALIDA);
        Entrega entrega = entregas.stream()
                .filter(e -> e.codigo().equals(pedido.modalidadeEntrega()))
                .findFirst()
                .orElseThrow(() -> new ErroPedidoException(CodigoErro.MODALIDADE_INVALIDA));
        if (!entrega.atende(peso)) {
            throw new ErroPedidoException(CodigoErro.MODALIDADE_INDISPONIVEL);
        }
        Cupom cupom = cupomInformado(pedido.cupom());
        if (cupom != null && !cupom.aplicavel(subtotal)) {
            throw new ErroPedidoException(CodigoErro.CUPOM_NAO_APLICAVEL);
        }
        FormaPagamento forma = buscarEnum(FormaPagamento.class, pedido.formaPagamento(), CodigoErro.FORMA_PAGAMENTO_INVALIDA);
        int parcelas = pedido.parcelas() == null ? 1 : pedido.parcelas();

        BigDecimal frete = nivel.temFreteGratis() ? Dinheiro.ZERO : centavos(entrega.frete(peso));
        BigDecimal desconto = cupom == null ? Dinheiro.ZERO : centavos(cupom.desconto(subtotal, itens, frete));
        BigDecimal seguro = centavos(subtotal.multiply(regiao.percentualSeguro()));
        BigDecimal totalPedido = subtotal.subtract(desconto).add(frete).add(seguro);

        Pagamento pagamento = CalculadoraPagamento.calcular(forma, totalPedido, parcelas);
        BigDecimal credito = centavos(subtotal.multiply(nivel.percentualCredito()));

        return new ResumoCompraResponse(
                subtotal,
                desconto,
                frete,
                entrega.prazoDias(),
                seguro,
                pagamento.ajuste(),
                pagamento.totalFinal(),
                pagamento.parcelas(),
                pagamento.valorParcela(),
                credito,
                nivel.ganhaBrinde(subtotal));
    }

    private static List<ItemCompra> itens(List<ItemRequest> pedidos) {
        if (pedidos == null || pedidos.isEmpty() || pedidos.stream().anyMatch(Objects::isNull)) {
            throw new ErroPedidoException(CodigoErro.PEDIDO_INVALIDO);
        }
        return pedidos.stream()
                .map(i -> new ItemCompra(i.nome(), i.precoUnitario(), i.quantidade(), i.pesoKg()))
                .toList();
    }

    private Cupom cupomInformado(String codigo) {
        if (codigo == null || codigo.isBlank()) {
            return null;
        }
        return cupons.stream()
                .filter(c -> c.codigo().equals(codigo))
                .findFirst()
                .orElseThrow(() -> new ErroPedidoException(CodigoErro.CUPOM_INVALIDO));
    }

    private static <E extends Enum<E>> E buscarEnum(Class<E> tipo, String codigo, CodigoErro erro) {
        return Arrays.stream(tipo.getEnumConstants())
                .filter(e -> e.name().equals(codigo))
                .findFirst()
                .orElseThrow(() -> new ErroPedidoException(erro));
    }
}

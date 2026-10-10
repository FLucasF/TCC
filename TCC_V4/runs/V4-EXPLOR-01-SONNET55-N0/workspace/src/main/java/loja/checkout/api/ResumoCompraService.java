package loja.checkout.api;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import loja.checkout.clube.NivelClube;
import loja.checkout.cupom.Cupom;
import loja.checkout.dominio.Dinheiro;
import loja.checkout.dominio.ErroNegocio;
import loja.checkout.dominio.Item;
import loja.checkout.dominio.Pedido;
import loja.checkout.dominio.Regiao;
import loja.checkout.entrega.OpcaoEntrega;
import loja.checkout.pagamento.FormaPagamento;
import loja.checkout.pagamento.Liquidacao;
import org.springframework.stereotype.Service;

@Service
public class ResumoCompraService {
    private final Map<String, OpcaoEntrega> entregas;
    private final Map<String, Cupom> cupons;
    private final Map<String, FormaPagamento> pagamentos;
    private final Map<String, NivelClube> niveis;

    public ResumoCompraService(
            List<OpcaoEntrega> entregas,
            List<Cupom> cupons,
            List<FormaPagamento> pagamentos,
            List<NivelClube> niveis) {
        this.entregas = indexar(entregas, OpcaoEntrega::codigo);
        this.cupons = indexar(cupons, Cupom::codigo);
        this.pagamentos = indexar(pagamentos, FormaPagamento::codigo);
        this.niveis = indexar(niveis, NivelClube::codigo);
    }

    private static <T> Map<String, T> indexar(List<T> lista, Function<T, String> chave) {
        return lista.stream().collect(Collectors.toMap(chave, Function.identity()));
    }

    public ResumoResponse calcular(ResumoRequest req) {
        if (req == null) {
            throw new ErroNegocio("PEDIDO_INVALIDO");
        }
        Pedido pedido = montarPedido(req.itens());

        NivelClube nivel = buscar(niveis, req.nivelClube(), "NIVEL_CLUBE_INVALIDO");
        Regiao regiao = buscarRegiao(req.regiao());

        OpcaoEntrega entrega = buscar(entregas, req.modalidadeEntrega(), "MODALIDADE_INVALIDA");
        if (!entrega.atende(pedido)) {
            throw new ErroNegocio("MODALIDADE_INDISPONIVEL");
        }

        Cupom cupom = null;
        if (req.cupom() != null) {
            cupom = buscar(cupons, req.cupom(), "CUPOM_INVALIDO");
            if (!cupom.aplicavel(pedido)) {
                throw new ErroNegocio("CUPOM_NAO_APLICAVEL");
            }
        }

        FormaPagamento pagamento = buscar(pagamentos, req.formaPagamento(), "FORMA_PAGAMENTO_INVALIDA");
        int parcelas = req.parcelas() == null ? 1 : req.parcelas();
        if (!pagamento.parcelasPermitidas(parcelas)) {
            throw new ErroNegocio("PARCELAMENTO_INVALIDO");
        }

        BigDecimal subtotal = pedido.subtotal();
        BigDecimal frete = nivel.isentoDeFrete() ? Dinheiro.ZERO : entrega.frete(pedido);
        BigDecimal desconto = cupom == null ? Dinheiro.ZERO : cupom.desconto(pedido, frete);
        BigDecimal seguro = regiao.seguro(subtotal);
        BigDecimal totalPedido = subtotal.subtract(desconto).add(frete).add(seguro);

        if (!pagamento.disponivel(totalPedido)) {
            throw new ErroNegocio("FORMA_PAGAMENTO_INDISPONIVEL");
        }
        Liquidacao liquidacao = pagamento.liquidar(totalPedido, parcelas);

        return new ResumoResponse(
                subtotal,
                desconto,
                frete,
                entrega.prazoDias(),
                seguro,
                liquidacao.totalFinal().subtract(totalPedido),
                liquidacao.totalFinal(),
                parcelas,
                liquidacao.valorParcela(),
                nivel.credito(subtotal),
                nivel.brinde(subtotal));
    }

    private static Pedido montarPedido(List<ResumoRequest.ItemRequest> itens) {
        if (itens == null || itens.isEmpty()) {
            throw new ErroNegocio("PEDIDO_INVALIDO");
        }
        List<Item> lista = itens.stream().map(ResumoCompraService::montarItem).toList();
        return new Pedido(lista);
    }

    private static Item montarItem(ResumoRequest.ItemRequest i) {
        if (i == null
                || i.precoUnitario() == null || i.precoUnitario().signum() <= 0
                || i.quantidade() == null || i.quantidade() <= 0
                || i.pesoKg() == null || i.pesoKg().signum() <= 0) {
            throw new ErroNegocio("PEDIDO_INVALIDO");
        }
        return new Item(i.nome(), i.precoUnitario(), i.quantidade(), i.pesoKg());
    }

    private static Regiao buscarRegiao(String valor) {
        if (valor != null) {
            for (Regiao r : Regiao.values()) {
                if (r.name().equals(valor)) {
                    return r;
                }
            }
        }
        throw new ErroNegocio("REGIAO_INVALIDA");
    }

    private static <T> T buscar(Map<String, T> mapa, String chave, String codigoErro) {
        T encontrado = chave == null ? null : mapa.get(chave);
        if (encontrado == null) {
            throw new ErroNegocio(codigoErro);
        }
        return encontrado;
    }
}

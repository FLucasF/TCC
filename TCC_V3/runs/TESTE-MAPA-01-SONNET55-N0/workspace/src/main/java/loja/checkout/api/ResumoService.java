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
import loja.checkout.entrega.ModalidadeEntrega;
import loja.checkout.pagamento.Cobranca;
import loja.checkout.pagamento.FormaPagamento;
import org.springframework.stereotype.Service;

@Service
public class ResumoService {
    private final Map<String, ModalidadeEntrega> modalidades;
    private final Map<String, Cupom> cupons;
    private final Map<String, NivelClube> niveis;
    private final Map<String, FormaPagamento> formas;

    public ResumoService(List<ModalidadeEntrega> modalidades, List<Cupom> cupons,
                         List<NivelClube> niveis, List<FormaPagamento> formas) {
        this.modalidades = indexar(modalidades, ModalidadeEntrega::codigo);
        this.cupons = indexar(cupons, Cupom::codigo);
        this.niveis = indexar(niveis, NivelClube::codigo);
        this.formas = indexar(formas, FormaPagamento::codigo);
    }

    private static <T> Map<String, T> indexar(List<T> lista, Function<T, String> codigo) {
        return lista.stream().collect(Collectors.toMap(codigo, Function.identity()));
    }

    public ResumoResponse calcular(ResumoRequest req) {
        Pedido pedido = montarPedido(req.itens());

        NivelClube nivel = obter(niveis, req.nivelClube(), "NIVEL_CLUBE_INVALIDO");
        Regiao regiao = Regiao.de(req.regiao()).orElseThrow(() -> new ErroNegocio("REGIAO_INVALIDA"));
        ModalidadeEntrega modalidade = obter(modalidades, req.modalidadeEntrega(), "MODALIDADE_INVALIDA");
        if (!modalidade.atende(pedido)) {
            throw new ErroNegocio("MODALIDADE_INDISPONIVEL");
        }

        Cupom cupom = null;
        if (req.cupom() != null) {
            cupom = obter(cupons, req.cupom(), "CUPOM_INVALIDO");
            if (!cupom.aplicavel(pedido)) {
                throw new ErroNegocio("CUPOM_NAO_APLICAVEL");
            }
        }

        FormaPagamento forma = obter(formas, req.formaPagamento(), "FORMA_PAGAMENTO_INVALIDA");
        int parcelas = req.parcelas() == null ? 1 : req.parcelas();
        if (!forma.parcelasPermitidas(parcelas)) {
            throw new ErroNegocio("PARCELAMENTO_INVALIDO");
        }

        BigDecimal subtotal = Dinheiro.arredondar(pedido.subtotal());
        BigDecimal frete = nivel.freteGratis() ? Dinheiro.ZERO : modalidade.frete(pedido);
        BigDecimal desconto = cupom == null ? Dinheiro.ZERO : cupom.desconto(pedido, frete);
        BigDecimal seguro = regiao.seguro(pedido);
        BigDecimal totalPedido = subtotal.subtract(desconto).add(frete).add(seguro);

        if (!forma.disponivel(totalPedido)) {
            throw new ErroNegocio("FORMA_PAGAMENTO_INDISPONIVEL");
        }
        Cobranca cobranca = forma.cobrar(totalPedido, parcelas);

        return new ResumoResponse(
                subtotal,
                desconto,
                frete,
                modalidade.prazoDias(),
                seguro,
                cobranca.totalFinal().subtract(totalPedido),
                cobranca.totalFinal(),
                parcelas,
                cobranca.valorParcela(),
                nivel.credito(pedido),
                nivel.brinde(pedido));
    }

    private static <T> T obter(Map<String, T> mapa, String codigo, String erro) {
        T valor = codigo == null ? null : mapa.get(codigo);
        if (valor == null) {
            throw new ErroNegocio(erro);
        }
        return valor;
    }

    private static Pedido montarPedido(List<ItemRequest> itens) {
        if (itens == null || itens.isEmpty()) {
            throw new ErroNegocio("PEDIDO_INVALIDO");
        }
        List<Item> validos = itens.stream().map(i -> {
            if (i == null
                    || i.precoUnitario() == null || i.precoUnitario().signum() <= 0
                    || i.quantidade() == null || i.quantidade() <= 0
                    || i.pesoKg() == null || i.pesoKg().signum() <= 0) {
                throw new ErroNegocio("PEDIDO_INVALIDO");
            }
            return new Item(i.nome(), i.precoUnitario(), i.quantidade(), i.pesoKg());
        }).toList();
        return new Pedido(validos);
    }
}

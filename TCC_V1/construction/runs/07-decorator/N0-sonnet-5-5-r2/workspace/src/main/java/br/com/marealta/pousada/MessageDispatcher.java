package br.com.marealta.pousada;

import br.com.marealta.pousada.Domain.Message;
import br.com.marealta.pousada.Domain.MessageStatus;

import java.time.Clock;
import java.time.Instant;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Aplica as regras de envio a cada mensagem e registra o resultado.
 *
 * Fluxo: (1) regras de bloqueio; se alguma bloqueia, a mensagem fica BLOCKED com o endereço original e
 * nada mais acontece; (2) regras de expansão (cópia para o arquivo); (3) regras de transformação
 * (redirecionamento do modo de teste), aplicadas a todas as mensagens que vão sair, inclusive cópias.
 * As regras que valem são escolhidas pela configuração da instalação.
 */
public class MessageDispatcher {
    public static final String ARCHIVE_ADDRESS = "arquivo@pousadamarealta.com.br";
    public static final String TEST_ADDRESS = "testes@pousadamarealta.com.br";

    /** Mensagem ainda sem id. */
    public record Draft(String channel, String to, String subject, String body) {}

    /** Onde as mensagens são guardadas. */
    public interface Store {
        /** Guarda e devolve a mensagem com id. guestKey identifica o hóspede (null para cópias). */
        Message save(Draft draft, MessageStatus status, String guestKey);

        int countSent(String guestKey, Instant from, Instant to);
    }

    interface BlockRule { boolean blocks(Draft d, String guestKey, Store store); }
    interface ExpandRule { List<Draft> extra(Draft d); }
    interface TransformRule { Draft apply(Draft d); }

    private final Config config;
    private final Clock clock;
    private final List<BlockRule> blockRules = new ArrayList<>();
    private final List<ExpandRule> expandRules = new ArrayList<>();
    private final List<TransformRule> transformRules = new ArrayList<>();

    public MessageDispatcher(Config config, Clock clock) {
        this.config = config;
        this.clock = clock;
        boolean test = config.testMode();

        blockRules.add((d, key, store) -> !isValidAddress(d.to()));
        if (config.quietHours() != null && config.quietHoursScope().appliesIn(test)) {
            blockRules.add((d, key, store) ->
                    config.quietHours().contains(ZonedDateTime.now(clock.withZone(config.zone())).toLocalTime()));
        }
        if (config.dailyLimit() != null && config.dailyLimitScope().appliesIn(test)) {
            blockRules.add((d, key, store) -> {
                ZonedDateTime now = ZonedDateTime.now(clock.withZone(config.zone()));
                Instant start = now.toLocalDate().atStartOfDay(config.zone()).toInstant();
                Instant end = now.toLocalDate().plusDays(1).atStartOfDay(config.zone()).toInstant();
                return store.countSent(key, start, end) >= config.dailyLimit();
            });
        }
        if (config.archiveCopy().appliesIn(test)) {
            expandRules.add(d -> List.of(new Draft(d.channel(), ARCHIVE_ADDRESS, "[Cópia] " + d.subject(), d.body())));
        }
        if (test) {
            // Sempre vale no modo de teste: não é configurável, para nada sair para endereço real.
            transformRules.add(d -> new Draft(d.channel(), TEST_ADDRESS, "[TESTE] " + d.subject(),
                    "Destinatário original: " + d.to() + "\n" + d.body()));
        }
    }

    /** Um único @, algo antes dele e, depois, um nome com pelo menos um ponto no meio. */
    public static boolean isValidAddress(String address) {
        if (address == null || address.chars().anyMatch(Character::isWhitespace)) return false;
        int at = address.indexOf('@');
        if (at <= 0 || at != address.lastIndexOf('@')) return false;
        String domain = address.substring(at + 1);
        return domain.length() > 2 && domain.substring(1, domain.length() - 1).contains(".");
    }

    public List<Message> dispatch(Store store, Draft original) {
        String guestKey = original.to() == null ? "" : original.to().trim().toLowerCase();
        for (BlockRule r : blockRules) {
            if (r.blocks(original, guestKey, store)) {
                return List.of(store.save(original, MessageStatus.BLOCKED, guestKey));
            }
        }
        List<Draft> out = new ArrayList<>();
        out.add(original);
        for (ExpandRule r : expandRules) out.addAll(r.extra(original));

        List<Message> saved = new ArrayList<>();
        for (int i = 0; i < out.size(); i++) {
            Draft d = out.get(i);
            for (TransformRule t : transformRules) d = t.apply(d);
            saved.add(store.save(d, MessageStatus.SENT, i == 0 ? guestKey : null));
        }
        return saved;
    }
}

package hotel.messages;

import java.util.List;

/**
 * Applies a sequence of filters to decide what happens to each message: it can go through
 * (unchanged or transformed), be blocked, or generate extra messages.
 */
public final class FilterChain {

    private final List<EmailFilter> filters;

    public FilterChain(List<EmailFilter> filters) {
        this.filters = List.copyOf(filters);
    }

    public FilterResult apply(OutgoingMessage message) {
        FilterResult result = FilterResult.allow(message);
        for (EmailFilter filter : filters) {
            if (result.isBlocked()) {
                break;
            }
            result = filter.apply(result.message());
        }
        return result;
    }
}

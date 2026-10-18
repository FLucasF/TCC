package shop.persistence;

import java.util.function.Function;

public interface Store {
    /** Runs the work in one transaction: commits when it returns, rolls back when it throws. */
    <T> T inTransaction(Function<Tx, T> work);
}

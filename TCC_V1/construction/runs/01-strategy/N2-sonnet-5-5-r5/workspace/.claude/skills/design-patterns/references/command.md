# Command

Turn a request into an object, so it can be stored, queued, logged, undone or replayed.

## Use it when

- actions must be undone and redone;
- actions must be queued, scheduled, retried or sent elsewhere to run;
- a history of what was done must be kept and replayed.

## Do not use it when

- the action runs immediately and nothing else happens to it: call the method;
- the "command" would be a wrapper around a single method call with no undo, queue or log.

## Shape

```java
interface EditCommand {
    void apply(TextBuffer buffer);
    void revert(TextBuffer buffer);
}

final class InsertText implements EditCommand { /* insert; revert removes it */ }
final class DeleteRange implements EditCommand { /* remembers what it removed, to put it back */ }

final class Editor {
    private final Deque<EditCommand> done = new ArrayDeque<>();
    private final Deque<EditCommand> undone = new ArrayDeque<>();

    void run(EditCommand command) { command.apply(buffer); done.push(command); undone.clear(); }
    void undo() { /* pop from done, revert, push to undone */ }
    void redo() { /* the reverse */ }
}
```

## Done right when

- each command carries everything it needs to run and to revert;
- the invoker (the editor, the queue) knows only the command interface.

## Usual mistakes

- Commands that cannot be undone in a system whose reason for them was undo.
- Commands that reach into global state instead of receiving what they act on.
- Command objects for every method call, with no queue, undo or history behind them.

## Neighbors

- **Strategy**: a rule run now; Command is a request kept for later.
- **Observer**: reactions to an event; a command is the request itself.

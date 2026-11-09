# Template Method

Write the fixed sequence of steps once, in a base class, and let each case fill in only the steps that differ.

## Use it when

- several cases follow the same sequence of steps, and only some steps change from case to case;
- a change to the sequence itself (a new first step, a new check at the end) should apply to every case at once;
- new cases following the same sequence are expected.

## Do not use it when

- there is no shared sequence: each case is a different algorithm (that is Strategy);
- there are two cases and the shared part is a couple of lines;
- the only shared thing is a formatting helper: a utility function is enough, and it is not a Template Method.

## Shape

```java
abstract class BoardGame {
    // the template method: the sequence lives here and nowhere else
    public final Result play(List<Player> players) {
        Board board = setUp(players);
        while (!isOver(board)) {
            playTurn(board, nextPlayer(board, players));
        }
        return decideWinner(board);
    }

    protected abstract Board setUp(List<Player> players);
    protected abstract boolean isOver(Board board);
    protected abstract void playTurn(Board board, Player player);
    protected abstract Result decideWinner(Board board);

    protected Player nextPlayer(Board board, List<Player> players) { /* default rotation; a hook */ }
}

final class Checkers extends BoardGame { /* only the four steps */ }
final class Reversi extends BoardGame { /* only the four steps */ }
```

The same idea works with composition: one generic runner that receives the varying steps as objects or functions
(`new GameRunner(setUp, isOver, playTurn, decideWinner)`), as long as every case goes through that runner.

## Done right when

- the sequence is written once, in the template method (marked `final` when the language allows);
- each case implements only the steps that vary, and none of them repeats the sequence;
- every case uses the skeleton, including the cases that existed before it was introduced;
- a change to the sequence is one edit, in the template method.

## Usual mistakes

- A common interface with one method (`run()`), where each implementation still writes the whole sequence: the
  cases look uniform from outside, but the skeleton is copied inside each one.
- A shared helper that formats the output, while each case still calls the steps in order by itself.
- The new cases use the skeleton and the old ones stay as they were.
- Steps so coarse that one "step" contains almost the whole sequence.

## Neighbors

- **Strategy**: the whole rule varies, there is no common skeleton.
- **Factory Method**: often appears as one of the steps of a template method ("create the piece").

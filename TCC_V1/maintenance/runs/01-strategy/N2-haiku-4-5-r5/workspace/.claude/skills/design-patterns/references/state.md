# State

Represent each state of an object as its own class, so the object behaves differently as its state changes, without
`switch` statements on the state in every method.

## Use it when

- one object moves through states over time, and several of its operations behave differently in each state;
- the transitions themselves carry rules (what is allowed from where, what happens on entering a state);
- adding a state today would mean editing many methods that switch on the current state.

## Do not use it when

- the "state" is just a status field that is read, with one or two simple checks: an enum and a guard clause are
  clearer;
- the allowed transitions fit in a small table (from, event, to) and the behavior does not differ per state;
- the variant is chosen once and never changes: that is Strategy.

## Shape

```java
interface MachineState {
    MachineState insertCoin(VendingMachine m, int cents);
    MachineState selectItem(VendingMachine m, String code);
}

final class Idle implements MachineState { /* insertCoin -> HasCredit; selectItem -> refuse */ }
final class HasCredit implements MachineState { /* selectItem -> Dispensing or refuse */ }
final class Dispensing implements MachineState { /* everything refused until done */ }

final class VendingMachine {
    private MachineState state = new Idle();
    void insertCoin(int cents) { state = state.insertCoin(this, cents); }
    void selectItem(String code) { state = state.selectItem(this, code); }
}
```

## Done right when

- each state class decides what its operations do and which state comes next;
- the context object only delegates to its current state;
- adding a state means one new class and the transitions that lead to it.

## Usual mistakes

- State classes exist, but the context still checks `if (state instanceof …)` before delegating.
- Most methods in most states only throw "not allowed": a transition table would have been simpler.
- Using State for something that never changes during the object's life.

## Neighbors

- **Strategy**: chosen from outside, stays fixed. State is changed by the object itself.
- **A transition table**: enough when states differ only in which transitions are allowed.

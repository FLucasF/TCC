---
name: design-patterns
description: Decide whether a part of the code needs an object-oriented design pattern (GoF), pick the right one among similar patterns, and apply it completely. Use it while designing or changing code whenever a feature has several variants that differ in behavior and more variants are expected (new types, rules, steps, listeners, formats), or when choosing between Strategy, State, Template Method, Observer, Decorator, Chain of Responsibility, Factory Method, Abstract Factory, Builder, Adapter, Facade, Composite or Command. Also use it to decide that no pattern is needed.
---

# Design patterns: when, which, and how

This skill is reference material for the task you are already doing. Read it, decide, write the code, and keep
going with the task. It does not answer questions: there is no one on the other side of it, so do not stop to
report what you read. If the task is to build or change a system, the result of using this skill is code.

## Step 1: is there a variation that deserves structure?

A pattern pays off only where the code has an **axis of variation**: several cases that differ in behavior (not just
in a value), with signs that more cases will come. Look for both:

- **Concrete variation now**: three or more cases with different rules, or two places that would each need an
  `if`/`switch` on the same case, or behaviors that combine in different ways.
- **Expected growth**: the requirements say new cases will keep arriving ("we keep adding…", "there will be more").

If you only have one of the two, prefer the simple solution. A single value, text or field that might change is a
constant or a configuration, not a pattern. A vague "maybe one day" is not expected growth. Structure added for cases
nobody asked for makes the code harder to read and still has to be maintained.

When the answer is yes, the goal is that **the next case enters in one place, without editing code that already
works**. Ask: "to add the next case, how many places would change?" If the honest answer is "several", the code needs
the structure.

## Step 2: name what varies, then pick the pattern

Pick by **what varies**, not by the names in the requirements.

| What varies | Pattern | Reference |
|---|---|---|
| A whole algorithm or rule, chosen per case (route planning, scoring, compression) | Strategy | [strategy.md](references/strategy.md) |
| The behavior of one object as it moves between states over time | State | [state.md](references/state.md) |
| Some steps inside a fixed sequence that every case follows | Template Method | [template-method.md](references/template-method.md) |
| Who must react when something happens | Observer | [observer.md](references/observer.md) |
| Extra behavior wrapped around the same operation, combined differently per configuration | Decorator | [decorator.md](references/decorator.md) |
| Which handler, of several in sequence, deals with a request (and whether it goes on) | Chain of Responsibility | [chain-of-responsibility.md](references/chain-of-responsibility.md) |
| Which concrete object to create, decided in one place for every caller | Factory Method | [factory-method.md](references/factory-method.md) |
| Whole families of related objects that must match each other | Abstract Factory | [abstract-factory.md](references/abstract-factory.md) |
| The construction of one complex object with many optional parts | Builder | [builder.md](references/builder.md) |
| An external interface that does not match the one your code expects | Adapter | [adapter.md](references/adapter.md) |
| Many subsystem calls that clients repeat together | Facade | [facade.md](references/facade.md) |
| Parts and wholes that must be treated the same way (trees) | Composite | [composite.md](references/composite.md) |
| Requests that must be stored, queued, undone or replayed | Command | [command.md](references/command.md) |

Before writing code, open the reference of the pattern you picked. Each one has when to use it, when not to, a short
Java sketch, how to tell it is done, the usual mistakes, and the neighbors it is confused with.

## Step 3: tell the neighbors apart

These pairs are where the choice usually goes wrong. Decide with the question in the second column.

| Confusion | The question that decides |
|---|---|
| Strategy or Template Method | Do the cases share a **fixed sequence of steps** and differ only in some of them? Template Method. Is the **whole rule** different per case, with no common skeleton? Strategy. Several board games that all go "set up, play turns, decide the winner" are a skeleton with varying steps, not interchangeable algorithms. |
| Strategy or State | Is the case **chosen from outside** and stays fixed? Strategy. Does the object **change its own case** as events happen? State. |
| Decorator or Chain of Responsibility | Do **all** the extra behaviors run around the same operation, each one adding something? Decorator. Does **one** handler take the request and usually stop it? Chain. Both are fine when the combination is decided where the objects are assembled. |
| Decorator or Adapter | Same interface, more behavior: Decorator. Different interface, same behavior: Adapter. |
| Observer or direct calls | Does the source need to know who reacts? If more reactions are expected and the source should not change for each one, Observer. With one fixed reaction, a direct call is simpler. |
| Factory Method or a constructor | Do several callers need to **choose** among concrete types by the same rule? Factory Method, so the choice lives in one place. If every caller knows the exact type, call the constructor. |
| Factory Method or Abstract Factory | One product varies: Factory Method. Several products must vary **together** as a family: Abstract Factory. |
| Builder or Factory Method | Builder is about **how** to assemble one complex object step by step; Factory Method is about **which** class to create. |
| Command or Strategy | Command packages a request to run later (queue, undo, log); Strategy packages a rule to run now. |

## Step 4: apply it completely

A pattern that covers only part of the cases is often worse than none, because the code now has two ways of doing
the same thing. When you apply one:

- **Move every case into the structure**, including the cases that already existed before the new ones. Leaving the
  old cases in an `if` and putting only the new ones in classes keeps the very branching the pattern was meant to
  remove.
- **Keep one place that chooses or assembles the cases** (a map, a registry, the code that wires the application).
  Code that uses the structure should not ask which concrete case it holds: no `instanceof`, no `switch` on the type,
  no special path for one case.
- **Give each case all of its own rules.** If one case needs extra input, make room for it in the shared interface or
  in what the case receives, instead of handling it outside the structure.
- **Decide combinations where objects are assembled** (the code that wires the application), so each piece only does
  its own job and the assembly says which pieces are used.
- **Check the result with the question from Step 1**: adding the next case should mean one new class (or entry) and
  one registration, and nothing else.

## When not to use a pattern

Use the simple solution, and say nothing more about patterns, when:

- there is only one case, or the cases differ only in data (a percentage, a label, a date range);
- the variation is real but nothing suggests it will grow, and two cases fit comfortably in one clear method;
- the "pattern" would wrap a single implementation that nothing replaces (an interface with one class, a factory
  that always builds the same thing, a layer that only forwards calls);
- the structure would serve a hypothetical future that the requirements mention only as "maybe".

Apply patterns where the variation is; keep the rest of the system plain. A system with one well-placed pattern and
simple code around it is the goal, not a pattern in every package.

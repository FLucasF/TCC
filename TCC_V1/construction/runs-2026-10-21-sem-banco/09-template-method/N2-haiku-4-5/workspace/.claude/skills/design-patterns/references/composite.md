# Composite

Treat single objects and groups of objects the same way, by giving them one interface, so trees can be built and
used without checking what each node is.

## Use it when

- the data is naturally a tree (shapes inside groups, folders inside folders, parts inside assemblies);
- clients should run the same operation on a leaf or on a whole group;
- groups can contain groups.

## Do not use it when

- the structure is a flat list;
- leaves and groups support very different operations.

## Shape

```java
interface Shape {
    double area();
    void moveBy(double dx, double dy);
}

final class Circle implements Shape { /* a leaf */ }
final class Rectangle implements Shape { /* a leaf */ }

final class Group implements Shape {
    private final List<Shape> children = new ArrayList<>();
    void add(Shape shape) { children.add(shape); }
    public double area() { return children.stream().mapToDouble(Shape::area).sum(); }
    public void moveBy(double dx, double dy) { children.forEach(s -> s.moveBy(dx, dy)); }
}
```

## Done right when

- clients call operations on a `Shape` without asking whether it is a group;
- a group's operation is defined by its children.

## Usual mistakes

- Clients check `instanceof Group` before calling the operation.
- Leaves forced to implement child-management methods that make no sense for them.

## Neighbors

- **Decorator**: also wraps objects with the same interface, but to add behavior, not to group.
- **Iterator** or plain recursion: often all that is needed to walk the tree.

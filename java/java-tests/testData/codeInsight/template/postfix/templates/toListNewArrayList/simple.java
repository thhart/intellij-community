import java.util.Set;

public class Foo {
    void m(Set<String> o) {
        o.toList<caret>
    }
}
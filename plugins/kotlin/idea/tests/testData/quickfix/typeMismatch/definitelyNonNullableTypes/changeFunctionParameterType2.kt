// "Change parameter 'x' type of function 'bar' to 'List<T & Any>'" "false"
// ERROR: Type mismatch: inferred type is List<T> but List<T & Any> was expected
// ACTION: Add 'Any' as upper bound for T to make it non-nullable
// ACTION: Add 'x =' to argument
// ACTION: Cast expression 'x' to 'List<T & Any>'
// ACTION: Change parameter 'x' type of function 'foo' to 'List<T>'
// ACTION: Create function 'foo'
// LANGUAGE_VERSION: 1.8
// K2_AFTER_ERROR: Argument type mismatch: actual type is 'List<T#1 (of fun <T> bar)>', but 'List<uninferred T (of fun <T> foo)>' was expected.
// K2_AFTER_ERROR: Cannot infer type for type parameter 'T'. Specify it explicitly.
package a

fun <T> foo(x: List<T & Any>) {}

fun <T> bar(x: List<T>) {
    foo(x<caret>)
}

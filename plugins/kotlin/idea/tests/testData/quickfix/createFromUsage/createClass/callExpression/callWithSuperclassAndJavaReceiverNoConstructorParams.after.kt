// "Create class 'Foo'" "true"
// ERROR: Unresolved reference: Foo
// IGNORE_K2
open class A

fun test(): A = J.Foo(2, "2")
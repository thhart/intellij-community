// "Change to constructor invocation" "true"
// PRIORITY: HIGH
fun bar() {
    abstract class Foo {}
    val foo: Foo = object : <caret>Foo {}
}
// FUS_QUICKFIX_NAME: org.jetbrains.kotlin.idea.quickfix.SuperClassNotInitialized$AddParenthesisFix

// FUS_K2_QUICKFIX_NAME: org.jetbrains.kotlin.idea.k2.codeinsight.fixes.SuperClassNotInitializedFactories$AddParenthesisFix

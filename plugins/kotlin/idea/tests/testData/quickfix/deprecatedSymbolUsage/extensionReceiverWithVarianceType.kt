// "Replace with 'bar(f)'" "true"
// WITH_STDLIB

fun test(list: List<Int>) {
    list.foo<caret> { it }
}

@Deprecated("", ReplaceWith("bar(f)"))
fun List<out Int>.foo(f: (Int) -> Unit) {}

fun List<Int>.bar(f: (Int) -> Unit) {}

// FUS_QUICKFIX_NAME: org.jetbrains.kotlin.idea.quickfix.replaceWith.DeprecatedSymbolUsageFix
// FUS_K2_QUICKFIX_NAME: org.jetbrains.kotlin.idea.k2.codeinsight.fixes.replaceWith.DeprecatedSymbolUsageFix
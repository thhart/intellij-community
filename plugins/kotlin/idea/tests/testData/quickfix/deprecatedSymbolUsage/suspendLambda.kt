// "Replace with 'runSuspend(block)'" "true"

fun runSuspend(block: suspend () -> Unit) {}
@Deprecated("Use new function", ReplaceWith("runSuspend(block)"))
fun runSuspendOld(block: suspend () -> Unit) = runSuspend(block)

fun println() {}

fun usage() {
    <caret>runSuspendOld { println() }
}
// FUS_QUICKFIX_NAME: org.jetbrains.kotlin.idea.quickfix.replaceWith.DeprecatedSymbolUsageFix
// FUS_K2_QUICKFIX_NAME: org.jetbrains.kotlin.idea.k2.codeinsight.fixes.replaceWith.DeprecatedSymbolUsageFix
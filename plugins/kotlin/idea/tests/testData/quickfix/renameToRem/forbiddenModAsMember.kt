// "Rename to 'rem'" "true"
// DISABLE-ERRORS
// COMPILER_ARGUMENTS: -XXLanguage:+ProhibitOperatorMod

object Rem {
    operator<caret> fun mod(x: Int) {}
    operator fun modAssign(x: Int) {}
}

fun test() {
    Rem % 1
    Rem.mod(1)
    Rem.modAssign(1)
}
// FUS_QUICKFIX_NAME: org.jetbrains.kotlin.idea.quickfix.RenameModToRemFix
// FUS_K2_QUICKFIX_NAME: org.jetbrains.kotlin.idea.quickfix.RenameModToRemFix
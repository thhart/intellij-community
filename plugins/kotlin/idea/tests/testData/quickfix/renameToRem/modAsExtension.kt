// "Rename to 'rem'" "true"
// DISABLE-ERRORS

object A
operator<caret> fun A.mod(x: Int) {}

fun test() {
    A.mod(3)
    A % 2
}
// FUS_QUICKFIX_NAME: org.jetbrains.kotlin.idea.quickfix.RenameModToRemFix
// FUS_K2_QUICKFIX_NAME: org.jetbrains.kotlin.idea.quickfix.RenameModToRemFix
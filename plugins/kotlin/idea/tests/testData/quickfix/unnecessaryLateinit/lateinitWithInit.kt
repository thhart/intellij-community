// "Remove 'lateinit' modifier" "true"

class Foo {
    <caret>lateinit var bar: String

    init {
        bar = ""
    }
}
// FUS_QUICKFIX_NAME: org.jetbrains.kotlin.idea.quickfix.RemoveModifierFixBase
// FUS_K2_QUICKFIX_NAME: org.jetbrains.kotlin.idea.quickfix.RemoveModifierFixBase
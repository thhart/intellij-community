// "Replace 'if' expression with safe access expression" "false"
// ACTION: Add non-null asserted (x!!) call
// ACTION: Introduce local variable
// DISABLE_ERRORS
class Test {
    var x: String? = ""

    fun test() {
        if (x != null) {
            bar()
            <caret>x.length
        }
    }

    fun bar() {}
}
// "Import" "false"
// ACTION: Rename reference
// ERROR: Unresolved reference: ext
// K2_AFTER_ERROR: Unresolved reference 'ext'.
package p

open class A {
    fun <T : CharSequence> T.ext() {}
}

object AObject : A()

fun usage() {
    // 20 is not a CharSequence, so no import should be suggested
    20.<caret>ext()
}

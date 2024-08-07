// "Change function signature to 'fun f(a: Int): Int'" "true"
// ERROR: Type mismatch: inferred type is String but Int was expected
open class A {
    open fun f(a: Int): Int = 0
}

class B : A(){
    // Note that when parameter types match, RETURN_TYPE_MISMATCH_ON_OVERRIDE error is reported
    // and "Change function signature" quickfix is not present.
    <caret>override fun f(a : String): String = "FOO"
}

// FUS_QUICKFIX_NAME: org.jetbrains.kotlin.idea.quickfix.ChangeMemberFunctionSignatureFix
// FUS_K2_QUICKFIX_NAME: org.jetbrains.kotlin.idea.k2.codeinsight.fixes.ChangeMemberFunctionSignatureFixFactory$ChangeMemberFunctionSignatureFix
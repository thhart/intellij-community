// "Add initializer" "true"
// ERROR: Val cannot be reassigned
// K2_AFTER_ERROR: 'val' cannot be reassigned.
// COMPILER_ARGUMENTS: -XXLanguage:-ProhibitOpenValDeferredInitialization
open class Foo {
    open <caret>val foo: Int
        get() = field

    init {
        foo = 2
    }
}

// FUS_QUICKFIX_NAME: org.jetbrains.kotlin.idea.quickfix.InitializePropertyQuickFixFactory$AddInitializerFix
// FUS_K2_QUICKFIX_NAME: org.jetbrains.kotlin.idea.k2.codeinsight.fixes.InitializePropertyQuickFixFactories$InitializePropertyModCommandAction
// "Propagate 'SubclassOptInRequired(A::class)' opt-in requirement to 'SomeImplementation'" "true"
// ERROR: This class or interface requires opt-in to be implemented. Its usage must be marked with '@B', '@OptIn(B::class)' or '@SubclassOptInRequired(B::class)'
// K2_AFTER_ERROR: This class or interface requires opt-in to be implemented. Its usage must be marked with '@B', '@OptIn(B::class)' or '@SubclassOptInRequired(B::class)'
@file:OptIn(ExperimentalSubclassOptIn::class)

@RequiresOptIn
annotation class A

@RequiresOptIn
annotation class B

@SubclassOptInRequired(A::class)
interface LibraryA

@SubclassOptInRequired(B::class)
interface LibraryB

interface SomeImplementation : LibraryA<caret>, LibraryB
// FUS_QUICKFIX_NAME: org.jetbrains.kotlin.idea.quickfix.OptInFixes$PropagateOptInAnnotationFix
// FUS_K2_QUICKFIX_NAME: org.jetbrains.kotlin.idea.quickfix.OptInFixes$PropagateOptInAnnotationFix
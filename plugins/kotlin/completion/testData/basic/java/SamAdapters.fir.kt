// FIR_COMPARISON
fun interface Fun<R> {
    operator fun invoke(): R
}

fun <R> runBlock(`fun`: Fun<R>): R = `fun`()

fun foo() {
    runBloc<caret>
}

// WITH_ORDER
// EXIST: { lookupString: "runBlock", itemText: "runBlock", tailText: " { `fun`: () -> R } (<root>)", typeText: "R", icon: "Function"}
// EXIST: { lookupString: "runBlock", itemText: "runBlock", tailText: "(`fun`: Fun<R>) (<root>)", typeText: "R", icon: "Function"}
// NOTHING_ELSE

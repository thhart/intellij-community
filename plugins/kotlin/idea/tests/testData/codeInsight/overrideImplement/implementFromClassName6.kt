// FIR_IDENTICAL
interface A {
    fun a1()
    fun a2()
    fun a3()
    fun a4()
    fun a5()
    fun a6()
    fun a7()
    fun a8()
}

interface B {
    fun b3()
    fun b2()
    fun b1()
}

class <caret>Test : A, B {
    override fun a3() {
    }

    override fun b2() {
    }

    override fun a6() {
    }
}

// MEMBER: "a1(): Unit"
// MEMBER: "a2(): Unit"
// MEMBER: "a4(): Unit"
// MEMBER: "a5(): Unit"
// MEMBER: "a7(): Unit"
// MEMBER: "a8(): Unit"
// MEMBER: "b3(): Unit"
// MEMBER: "b1(): Unit"
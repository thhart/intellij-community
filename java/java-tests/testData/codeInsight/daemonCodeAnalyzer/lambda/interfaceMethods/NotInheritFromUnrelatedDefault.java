interface FirstParent {

    default int doSomething() {
        return 1;
    }
}

interface SecondParent {

    int doSomething();
}

<error descr="Class 'FirstSon' must either be declared abstract or implement abstract method 'doSomething()' in 'SecondParent'">class FirstSon implements FirstParent, SecondParent</error> {}

<error descr="Class 'SecondSon' must either be declared abstract or implement abstract method 'doSomething()' in 'SecondParent'">class SecondSon implements SecondParent, FirstParent</error> {}

interface A {
  default int foo() {
    return 1;
  }
}

interface B {
  abstract int foo();
}

interface <error descr="C inherits abstract and default for foo() from types A and B">C</error> extends A, B {
}

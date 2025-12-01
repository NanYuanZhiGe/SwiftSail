package com.nyzg.common;

public class Pair <A,B>{
    A a;
    B b;

    public Pair(A a, B b) {
        this.a = a;
        this.b = b;
    }
    public Pair(){}

    public A getA() {
        return a;
    }

    public void setA(A a) {
        this.a = a;
    }

    public B getB() {
        return b;
    }

    public void setB(B b) {
        this.b = b;
    }
}

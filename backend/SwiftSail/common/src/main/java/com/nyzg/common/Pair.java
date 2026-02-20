package com.nyzg.common;

import lombok.Getter;

@Getter
public class Pair <A,B>{
    A a;
    B b;

    public Pair(A a, B b) {
        this.a = a;
        this.b = b;
    }
    public Pair(){}

    public void setA(A a) {
        this.a = a;
    }

    public void setB(B b) {
        this.b = b;
    }
}

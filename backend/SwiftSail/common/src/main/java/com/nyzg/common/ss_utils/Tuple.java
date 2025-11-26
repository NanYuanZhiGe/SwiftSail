package com.nyzg.common.ss_utils;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;

@Data
@NoArgsConstructor
@ToString
public class Tuple<A,B,C> {
    A a;
    B b;
    C c;

    public Tuple(A a, B b, C c) {
        this.a = a;
        this.b = b;
        this.c = c;
    }
}
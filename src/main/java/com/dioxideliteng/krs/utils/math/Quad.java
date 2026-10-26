package com.dioxideliteng.krs.utils.math;

public class Quad<A, B, C, D> {
    private final A first;
    private final B second;
    private final C third;
    private final D fourth;

    public Quad(A first, B second, C third, D fourth) {
        this.first = first;
        this.second = second;
        this.third = third;
        this.fourth = fourth;
    }

    public A getFirst() { return first; }
    public B getSecond() { return second; }
    public C getThird() { return third; }
    public D getFourth() { return fourth; }
}
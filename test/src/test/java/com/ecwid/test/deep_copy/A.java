package com.ecwid.test.deep_copy;

public abstract class A {

    protected int factor;
    private final int const_ = 999;

    public A(int factor) {
        this.factor = factor;
    }

    public int getConst() {
        return const_;
    }

    @Override
    public String toString() {
        return "A{" +
                "factor=" + factor +
                '}';
    }
}

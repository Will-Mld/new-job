package com.ecwid.test.deep_copy;

public class B extends A {

    private final int multiplier;

    public B(int factor, int multiplier) {
        super(factor);
        this.multiplier = multiplier;
    }

    @Override
    public String toString() {
        return "B{" +
                "multiplier=" + multiplier +
                ", factor=" + factor +
                '}';
    }
}

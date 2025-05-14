package com.ecwid.ipcounter;

import java.util.Arrays;
import java.util.BitSet;
import java.util.stream.Stream;

public class BitSetContainer implements IntContainer {

    private final BitSet[] storage;
    private final int mask;
    private final int shift;

    public BitSetContainer(int level){
        mask = 0xFFFF_FFFF >>> level;
        shift = Integer.SIZE - level;
        storage = Stream.generate(BitSet::new).limit(1L << level).toArray(BitSet[]::new);
    }

    @Override
    public void add(int number) {
        storage[number>>>shift].set(number&mask);
    }

    @Override
    public long countUnique() {
        return Arrays.stream(storage).mapToLong(BitSet::cardinality).sum();
    }
}

package com.ecwid.ipcounter;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.function.Supplier;
import java.util.function.ToIntFunction;

/**
 * If we run the ‘naive’ algorithm, we get the message ‘java.lang.OutOfMemoryError: Java heap space’.
 * This is because IPv4 has a string size of 7 to 15 characters. A string object will need 24 to 32 bytes.
 * If there are a billion unique IPv4's in the file, our programme will need about 32Gb just to store this unique set.
 * So first we convert each IPv4 to int.  For this purpose I implemented IpConverter.
 * Since we need to calculate the number of unique addresses, we do not need to store the addresses
 * and can use BitSet. For this purpose I implemented BitSetContainer.
 * */
public class IpCounter {
    ToIntFunction<CharSequence> converter = new IpConverter();
    Supplier<IntContainer> supplier = () -> new BitSetContainer(Byte.SIZE);

    public long countUniqueIp (String fileName) {
        Path path = Path.of(fileName);
        try (var lines = Files.lines(path)){
            System.out.println("start work with file - "+fileName);
            return lines.mapToInt(converter)
                    .collect(supplier, IntContainer::add, IntContainer::addAll)
                    .countUnique();
        } catch (IOException e) {
            System.out.println(e.getMessage());
            throw new RuntimeException(e);
        }
    }
}

package com.ecwid.test.deep_copy;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.concurrent.atomic.AtomicReference;

/**
 * --add-opens=java.base/java.lang=ALL-UNNAMED
 * --add-opens=java.base/java.util=ALL-UNNAMED
 * --add-opens=java.base/java.util.concurrent.atomic=ALL-UNNAMED
 *
 */
public class Main {

    public static void main(String[] args)
    {
        copyMan();
        copyHashSet();
        copyLinkedList();

        System.out.println("That's all, folks");
    }

    static void copyMan()
    {
        Man man = new Man("Dan", 29, Arrays.asList("Dublin", "New York"));
        Man copyMan = CopyUtils.deepCopy(man);
        System.out.println(copyMan);

        ArrayList<String> books = new ArrayList<>(); // Arrays.asList produces different class
        books.add("Dodge in Hell");
        books.add("The Rise and Fall of D.O.D.O");
        man = new Man("Tommy", 43, books);
        copyMan = CopyUtils.deepCopy(man);
        System.out.println(copyMan);
    }

    static void copyHashSet()
    {
        HashSet<AtomicReference<Double>> set = new HashSet<>();
        AtomicReference<Double> ref = new AtomicReference<>(100.1);
        set.add(ref);
        ref = new AtomicReference<>(200.1);
        set.add(ref);
        ref = new AtomicReference<>(300.1);
        set.add(ref);
        HashSet<AtomicReference<Double>> copy = CopyUtils.deepCopy(set);
        System.out.println(copy);
        ref = new AtomicReference<>(200.1);
        copy.add(ref);
        System.out.println(copy);
        System.out.println(set);
    }

    static void copyLinkedList()
    {
        LinkedList<Integer> list = new LinkedList<>();
        for (int k = 10; k < 10000; k++) // to test where recursion fails
            list.add(k);
        LinkedList<Integer> listCopy = CopyUtils.deepCopy(list);
        System.out.println(listCopy);
    }

}

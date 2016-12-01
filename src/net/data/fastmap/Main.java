package net.data.fastmap;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;

public class Main {

    public static void main(String[] args) {
        Random r = new Random(42);
        ArrayList<Integer> values = new ArrayList<>();
        for (int i = 0; i < 2000000; i++) {
            values.add(r.nextInt(90000));
        }

        Map<Integer, Integer> verifyMap = new HashMap<>(100000);
        Map<Integer, Integer> testMap = new FastMap<>(100000);

        long start = System.currentTimeMillis();
        for (int i : values) {
            verifyMap.put(i, i + 1);
        }
        long verifyTime = System.currentTimeMillis() - start;
        System.out.println("Old: " + verifyTime);

        start = System.currentTimeMillis();
        for (int i : values) {
            testMap.put(i, i + 1);
        }
        verifyTime = System.currentTimeMillis() - start;
        System.out.println("New: " + verifyTime);

        System.out.println("Size: " + verifyMap.size() + " / " + testMap.size());
    }
}

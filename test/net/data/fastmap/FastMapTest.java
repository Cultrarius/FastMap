package net.data.fastmap;

import org.junit.Test;

import java.util.HashMap;
import java.util.Map;
import java.util.Random;
import java.util.Set;

import static org.junit.Assert.*;


public class FastMapTest {

    @Test
    public void testSimpleInsertion() {
        Map<String, String> map = new FastMap<>();

        String oldValue = map.put("123", "test");
        assertEquals(null, oldValue);

        oldValue = map.put("123", "testX");
        assertEquals("test", oldValue);

        oldValue = map.put("234", "new");
        assertEquals(null, oldValue);

        oldValue = map.put("456", "test");
        assertEquals(null, oldValue);

        assertEquals("testX", map.get("123"));
        assertEquals("new", map.get("234"));
        assertEquals("test", map.get("456"));
        assertEquals(null, map.get("test"));
    }

    @Test
    public void testRandomInsertion() {
        Map<Integer, Integer> verifyMap = new HashMap<>();
        Map<Integer, Integer> testMap = new FastMap<>();

        Random r = new Random();
        for (int i = 0; i < 2000; i++) {
            int key = r.nextInt(5000);
            int value = r.nextInt();
            Integer oldVal1 = testMap.put(key, value);
            Integer oldVal2 = verifyMap.put(key, value);
            assertEquals(oldVal2, oldVal1);
        }

        verifyMap.entrySet().forEach(entry -> assertEquals(entry.getValue(), testMap.get(entry.getKey())));

        Set<Integer> keySet = testMap.keySet();
        verifyMap.keySet().forEach(key -> assertTrue(testMap.containsKey(key) && keySet.contains(key)));
    }

    @Test
    public void testInsertionTime() {
        Map<Integer, Integer> verifyMap = new HashMap<>(100000);
        Map<Integer, Integer> testMap = new FastMap<>(100000);

        Random r = new Random(42);
        long start = System.currentTimeMillis();
        for (int i = 0; i < 1000000; i++) {
            int key = r.nextInt(90000);
            int value = r.nextInt();
            verifyMap.put(key, value);
        }
        long verifyTime = System.currentTimeMillis() - start;
        System.out.println("Old: " + verifyTime);

        r = new Random(42);
        start = System.currentTimeMillis();
        for (int i = 0; i < 1000000; i++) {
            int key = r.nextInt(90000);
            int value = r.nextInt();
            testMap.put(key, value);
        }
        verifyTime = System.currentTimeMillis() - start;
        System.out.println("New: " + verifyTime);

        System.out.println("Size: " + verifyMap.size() + " / " + testMap.size());
    }
}
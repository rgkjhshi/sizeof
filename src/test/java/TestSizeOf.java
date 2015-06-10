import cn.loveshisong.sizeof.SizeOf;
import org.junit.Test;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.concurrent.ConcurrentSkipListMap;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import static junit.framework.Assert.assertEquals;

/**
 * @author song.shi
 * @date 15/6/7
 */

/**
 * JDK7
 * JVM参数 -XX:+UseCompressedOops
 */
public class TestSizeOf {

    private static class Parent {
        private int i;
    }

    private static class Child extends Parent {
        private int j;
    }

    private static class Recursive {
        int i;
        Recursive child = null;
    }

    @Test
    public void testPrimitives() {

        // Object
        assertEquals(16, SizeOf.sizeOf(new Object()));
        assertEquals(16, SizeOf.fullSizeOf(new Object()));

        // Integer
        assertEquals(16, SizeOf.sizeOf(new Integer(0)));
        assertEquals(16, SizeOf.fullSizeOf(new Integer(0)));


        // String
        assertEquals(24, SizeOf.sizeOf(""));
        assertEquals(40, SizeOf.fullSizeOf(""));
        assertEquals(24, SizeOf.sizeOf("a"));
        assertEquals(48, SizeOf.fullSizeOf("a"));


        // array
        assertEquals(16, SizeOf.sizeOf(new Object[0]));
        Object[] objects = new Object[10];
        assertEquals(16 + 4 * 10, SizeOf.sizeOf(objects));
        assertEquals(16 + 4 * 10, SizeOf.fullSizeOf(objects));

        for(int i = 0; i < objects.length; i++) {
            objects[i] = new Object();
        }
        assertEquals(16 + 4 * 10, SizeOf.sizeOf(objects));
        assertEquals(16 + 4 * 10 + 16 * 10, SizeOf.fullSizeOf(objects));
    }

    @Test
    public void testCycle() {
        Recursive dummy = new Recursive();
        assertEquals(24, SizeOf.sizeOf(dummy));
        assertEquals(24, SizeOf.fullSizeOf(dummy));
        dummy.child = dummy;
        assertEquals(24, SizeOf.fullSizeOf(dummy));
    }

    @Test
    public void testInheritance() {
        assertEquals(24, SizeOf.sizeOf(new Parent()));
        assertEquals(24, SizeOf.fullSizeOf(new Parent()));
        assertEquals(32, SizeOf.sizeOf(new Child()));
        assertEquals(32, SizeOf.fullSizeOf(new Child()));
    }

    @Test
    public void testCollections() {
        System.out.println(SizeOf.fullSizeOf(new ArrayList()));
        System.out.println(SizeOf.fullSizeOf(new HashMap()));
        System.out.println(SizeOf.fullSizeOf(new LinkedHashMap()));
        System.out.println(SizeOf.fullSizeOf(new ReentrantReadWriteLock()));
        System.out.println(SizeOf.fullSizeOf(new ConcurrentSkipListMap()));
    }

    @Test
    public void testDeep() {
        Recursive root = new Recursive();
        Recursive recursive = root;
        for (int i = 0; i < 10; i++) {
            recursive.child = new Recursive();
            recursive = recursive.child;
        }
        assertEquals(24 * 11, SizeOf.fullSizeOf(root));
    }

}

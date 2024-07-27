package ysoserial.test.payloads;

import org.junit.Test;

import java.io.*;
import java.lang.invoke.SerializedLambda;
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
}

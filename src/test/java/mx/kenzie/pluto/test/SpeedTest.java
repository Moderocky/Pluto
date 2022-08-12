package mx.kenzie.pluto.test;

import mx.kenzie.pluto.Pluto;

import java.io.*;

public class SpeedTest {
    
    
    private static final Pluto pluto = new Pluto();
    private static final Thing thing = new Thing();
    
    public static void main(String... args) throws Throwable {
        thing.name = "Test";
        thing.age = 37;
        thing.hands = 3;
        for (int i = 0; i < 10000; i++) {
            java();
        }
        {
            final long start = System.currentTimeMillis();
            for (int i = 0; i < 1000000; i++) {
                java();
            }
            final long end = System.currentTimeMillis();
            System.out.println("Java's serializer took " + (end - start) + " millis to serialise 1000000 objects.");
        }
        pluto.writeSerialiser(Thing.class, true);
        for (int i = 0; i < 10000; i++) {
            pluto();
        }
        {
            final long start = System.currentTimeMillis();
            for (int i = 0; i < 1000000; i++) {
                pluto();
            }
            final long end = System.currentTimeMillis();
            System.out.println("Pluto took " + (end - start) + " millis to serialise 1000000 objects.");
        }
        pluto.writeUnsafeSerialiser(Thing.class, null);
        for (int i = 0; i < 10000; i++) {
            unsafe();
        }
        {
            final long start = System.currentTimeMillis();
            for (int i = 0; i < 1000000; i++) {
                unsafe();
            }
            final long end = System.currentTimeMillis();
            System.out.println("Pluto's unsafe serialiser took " + (end - start) + " millis to serialise 1000000 objects.");
        }
        // speed test
    }
    
    private static void java() throws Throwable {
        final ByteArrayOutputStream stream = new ByteArrayOutputStream();
        final ObjectOutputStream output = new ObjectOutputStream(stream);
        output.writeObject(thing);
        final byte[] bytes = stream.toByteArray();
        final ObjectInputStream input = new ObjectInputStream(new ByteArrayInputStream(bytes));
        final Thing result = (Thing) input.readObject();
        if (result == null) throw new Error();
        input.close();
        output.close();
    }
    
    private static void pluto() throws Throwable {
        final Thing result = new Thing();
        final ByteArrayOutputStream stream = new ByteArrayOutputStream();
        pluto.serialise(thing, stream);
        pluto.deserialise(result, new DataInputStream(new ByteArrayInputStream(stream.toByteArray())));
    }
    
    private static void unsafe() throws Throwable {
        final Thing result = new Thing();
        final ByteArrayOutputStream stream = new ByteArrayOutputStream();
        pluto.serialise(thing, stream);
        pluto.deserialise(result, new DataInputStream(new ByteArrayInputStream(stream.toByteArray())));
    }
    
    public static class Thing implements Serializable {
        public String name;
        public int age;
        protected short hands;
    }
    
}

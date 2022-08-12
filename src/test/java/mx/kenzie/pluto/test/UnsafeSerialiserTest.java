package mx.kenzie.pluto.test;

import mx.kenzie.pluto.Pluto;
import org.junit.Test;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.Serializable;

public class UnsafeSerialiserTest {
    
    @Test
    public void internal() {
        final Person person = new Person();
        person.name = "Test";
        person.age = 37;
        person.hands = 3;
        final Pluto pluto = new Pluto();
        pluto.writeUnsafeSerialiser(Person.class, null);
        final ByteArrayOutputStream stream = new ByteArrayOutputStream();
        pluto.serialise(person, stream);
        final byte[] bytes = stream.toByteArray();
        final DataInputStream input = new DataInputStream(new ByteArrayInputStream(bytes));
        final Person result = new Person();
        assert result.age == 0;
        assert result.hands == 0;
        pluto.deserialise(result, input);
        assert result.age == 37;
        assert result.hands == 3;
    }
    
    public static class Person implements Serializable {
        public String name;
        public int age;
        protected short hands;
    }
}

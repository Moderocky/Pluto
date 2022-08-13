package mx.kenzie.pluto.test;

import mx.kenzie.pluto.Pluto;
import mx.kenzie.pluto.Stored;
import org.junit.Test;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.Serializable;

public class SimpleSerialiserTest {
    
    @Test
    public void simple() throws Throwable {
        final Person person = new Person();
        person.name = "Test";
        person.age = 37;
        person.hands = 3;
        final Pluto pluto = new Pluto();
        final byte[] code = pluto.writeSerialiser(Person.class, false);
        assert code.length > 0;
        final short clash = pluto.clashCode(Person.class);
        final ByteArrayOutputStream stream = new ByteArrayOutputStream();
        pluto.serialise(person, stream);
        final byte[] bytes = stream.toByteArray();
        final DataInputStream input = new DataInputStream(new ByteArrayInputStream(bytes));
        assert input.readShort() == clash;
        final Stored.Deserialiser deserialiser = pluto.getDeserialiser(clash);
        final Person result = new Person();
        assert result.age == 0;
        assert result.hands == 0;
        deserialiser.run(result, input, pluto);
        assert result.age == 37;
        assert result.hands == 0;
        input.close();
    }
    
    @Test
    public void internal() throws Throwable {
        final Person person = new Person();
        person.name = "Test";
        person.age = 37;
        person.hands = 3;
        final Pluto pluto = new Pluto();
        pluto.writeSerialiser(Person.class, true);
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
        input.close();
    }
    
    public static class Person implements Serializable {
        public String name;
        public int age;
        protected short hands;
    }
}

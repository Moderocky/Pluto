package mx.kenzie.pluto;

import sun.misc.Unsafe;

import java.io.*;
import java.util.HashMap;
import java.util.Map;

public class Pluto {
    
    protected final Map<Short, Class<?>> map;
    protected final Map<Class<?>, Short> reverse;
    protected final Map<Class<?>, Stored.Serialiser> serialisers;
    protected final Map<Class<?>, Stored.Deserialiser> deserialisers;
    
    public Pluto() {
        this.map = new HashMap<>();
        this.reverse = new HashMap<>();
        this.serialisers = new HashMap<>();
        this.deserialisers = new HashMap<>();
    }
    
    public static void main(String[] args) {
    
    }
    
    public static void transform(File file) {
    
    }
    
    public void serialise(Object object, OutputStream output) {
        this.serialise(object, new DataOutputStream(output));
    }
    
    public void serialise(Object object, DataOutputStream stream) {
        final Stored.Serialiser serialiser = this.getSerialiser(object.getClass());
        if (serialiser == null) return; // todo error
        serialiser.run(object, stream, this);
    }
    
    public Stored.Serialiser getSerialiser(Class<?> type) {
        return serialisers.get(type);
    }
    
    public void deserialise(Object object, InputStream input) {
        this.deserialise(object, new DataInputStream(input));
    }
    
    public void deserialise(Object object, DataInputStream stream) {
        final Stored.Deserialiser deserialiser;
        try {
            final short clash = stream.readShort();
            deserialiser = this.getDeserialiser(clash);
        } catch (IOException ex) {
            throw new RuntimeException("Failed to verify type.");
        }
        if (deserialiser == null) return; // todo error
        deserialiser.run(object, stream, this);
    }
    
    public Stored.Deserialiser getDeserialiser(short s) {
        final Class<?> type = map.get(s);
        return deserialisers.get(type);
    }
    
    public void deserialise(Object object, byte[] bytes) {
        this.deserialise(object, new DataInputStream(new ByteArrayInputStream(bytes)));
    }
    
    public Stored.Deserialiser getDeserialiser(Class<?> type) {
        return deserialisers.get(type);
    }
    
    protected void register(Class<?> type, short index, Stored.Serialiser serialiser, Stored.Deserialiser deserialiser) {
        this.serialisers.put(type, serialiser);
        this.deserialisers.put(type, deserialiser);
        this.map.put(index, type);
        this.reverse.put(type, index);
    }
    
    public <Type> short register(Class<Type> type, Stored.Serialiser serialiser, Stored.Deserialiser deserialiser) {
        this.serialisers.put(type, serialiser);
        this.deserialisers.put(type, deserialiser);
        short code = this.code(type);
        while (map.containsKey(code)) code++;
        this.map.put(code, type);
        this.reverse.put(type, code);
        return code;
    }
    
    protected short code(Class<?> type) {
        short s = (short) (type.hashCode() * 17);
        s ^= type.hashCode() >> 16;
        return s;
    }
    
    @SuppressWarnings("unchecked")
    public <Type, Thing extends Stored.Serialiser & Stored.Deserialiser>
    short writeSerialiser(Class<Type> type, boolean readPrivate) {
        final short code = this.clashCode(type);
        final AutomaticSerialiser serialiser = new AutomaticSerialiser(type, this);
        serialiser.prepareFields(readPrivate);
        serialiser.writeConstructor();
        serialiser.writeSerialiser();
        serialiser.writeDeserialiser();
        final Thing thing = (Thing) serialiser.create();
        this.register0(type, thing, thing);
        return code;
    }
    
    public short clashCode(Class<?> type) {
        final Short value = reverse.get(type);
        if (value == null) {
            short code = this.code(type);
            while (map.containsKey(code)) code++;
            this.map.put(code, type);
            this.reverse.put(type, code);
            return code;
        } else return value;
    }
    
    protected void register0(Class<?> type, Stored.Serialiser serialiser, Stored.Deserialiser deserialiser) {
        this.serialisers.put(type, serialiser);
        this.deserialisers.put(type, deserialiser);
    }
    
    @SuppressWarnings("unchecked")
    public <Type, Thing extends Stored.Serialiser & Stored.Deserialiser>
    short writeUnsafeSerialiser(Class<Type> type, Unsafe unsafe) {
        final short code = this.clashCode(type);
        final UnsafeSerialiser serialiser = new UnsafeSerialiser(type, this, unsafe);
        serialiser.prepareFields(true);
        serialiser.writeConstructor();
        serialiser.writeSerialiser();
        serialiser.writeDeserialiser();
        final Thing thing = (Thing) serialiser.create();
        this.register0(type, thing, thing);
        return code;
    }
    
}

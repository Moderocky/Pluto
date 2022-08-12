package mx.kenzie.pluto;

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
        final DataOutputStream stream = (output instanceof DataOutputStream data) ? data : new DataOutputStream(output);
        final Stored.Serialiser serialiser = this.getSerialiser(object.getClass());
        if (serialiser == null) return; // todo error
        serialiser.run(object, stream, this);
    }
    
    public Stored.Serialiser getSerialiser(Class<?> type) {
        return serialisers.get(type);
    }
    
    public void deserialise(Object object, InputStream input) {
        final DataInputStream stream = (input instanceof DataInputStream data) ? data : new DataInputStream(input);
        final Stored.Deserialiser deserialiser;
        try {
            final short clash = stream.readShort();
            assert clash == this.clashCode(object.getClass());
            deserialiser = this.getDeserialiser(clash);
        } catch (IOException ex) {
            throw new RuntimeException("Failed to verify type.");
        }
        if (deserialiser == null) return; // todo error
        deserialiser.run(object, stream, this);
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
    
    public Stored.Deserialiser getDeserialiser(short s) {
        final Class<?> type = map.get(s);
        return deserialisers.get(type);
    }
    
    protected short code(Class<?> type) {
        short s = (short) (type.hashCode() * 17);
        s ^= type.hashCode() >> 16;
        return s;
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
    
    protected void register0(Class<?> type, Stored.Serialiser serialiser, Stored.Deserialiser deserialiser) {
        this.serialisers.put(type, serialiser);
        this.deserialisers.put(type, deserialiser);
    }
    
}

package mx.kenzie.pluto;

import java.io.File;
import java.lang.reflect.Type;
import java.util.HashMap;
import java.util.Map;

public class Pluto {
    
    protected final Map<Short, Class<?>> map;
    protected final Map<Class<?>, Short> reverse;
    protected final Map<Class<?>, Stored.Serialiser<?>> serialisers;
    protected final Map<Class<?>, Stored.Deserialiser<?>> deserialisers;
    public Pluto() {
        this.map = new HashMap<>();
        this.reverse = new HashMap<>();
        this.serialisers = new HashMap<>();
        this.deserialisers = new HashMap<>();
    }
    
    public Stored.Serialiser<?> getSerialiser(Class<?> type) {
        return serialisers.get(type);
    }
    
    protected void register(Class<?> type, short index, Stored.Serialiser<?> serialiser, Stored.Deserialiser<?> deserialiser) {
        this.serialisers.put(type, serialiser);
        this.deserialisers.put(type, deserialiser);
        this.map.put(index, type);
        this.reverse.put(type, index);
    }
    
    protected void register0(Class<?> type, Stored.Serialiser<?> serialiser, Stored.Deserialiser<?> deserialiser) {
        this.serialisers.put(type, serialiser);
        this.deserialisers.put(type, deserialiser);
    }
    
    public <Type> short register(Class<Type> type, Stored.Serialiser<Type> serialiser, Stored.Deserialiser<Type> deserialiser) {
        this.serialisers.put(type, serialiser);
        this.deserialisers.put(type, deserialiser);
        short code = this.code(type);
        while (map.containsKey(code)) code++;
        this.map.put(code, type);
        this.reverse.put(type, code);
        return code;
    }
    
    @SuppressWarnings("unchecked")
    public <Type, Thing extends Stored.Serialiser<Type> & Stored.Deserialiser<Type>>
    short writeSerialiser(Class<Type> type, boolean readPrivate) {
        final short code = this.clashCode(type);
        final AutomaticSerialiser serialiser = new AutomaticSerialiser(type, this);
        serialiser.prepareFields(readPrivate);
        serialiser.writeConstructor();
        serialiser.writeSerialiser();
//        serialiser.writeDeserialiser(); todo
        final Thing thing = (Thing) serialiser.create();
        this.register(type, thing, thing);
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
    
    protected short code(Class<?> type) {
        short s = (short) (type.hashCode() * 17);
        s ^= type.hashCode() >> 16;
        return s;
    }
    
    public static void main(String[] args) {
    
    }
    
    public static void transform(File file) {
    
    }
    
}

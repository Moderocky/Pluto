package mx.kenzie.pluto;

import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.Type;

import java.lang.reflect.Field;
import java.util.LinkedHashSet;
import java.util.Set;

public abstract class Marshalling {
    
    protected final Pluto pluto;
    protected final Class<?> type;
    protected final ClassWriter writer;
    protected final String location;
    protected final Set<Field> fields = new LinkedHashSet<>();
    protected String internal;
    protected String key;
    protected byte[] bytecode;
    
    protected Marshalling(Pluto pluto, Class<?> type) {
        this.pluto = pluto;
        this.type = type;
        this.writer = new ClassWriter(0);
        this.location = Type.getInternalName(type);
    }
    
    public abstract void prepareFields(boolean all);
    
    public abstract void writeConstructor();
    
    public abstract void writeSerialiser();
    
    public abstract void writeDeserialiser();
    
    public abstract Object create();
}

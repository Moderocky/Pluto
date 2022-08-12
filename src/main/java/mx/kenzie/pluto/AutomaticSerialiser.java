package mx.kenzie.pluto;

import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Type;

import java.lang.invoke.MethodHandles;
import java.lang.reflect.Field;
import java.util.LinkedHashSet;
import java.util.Set;

class AutomaticSerialiser {
    
    private static final String[] SERIALISERS = {Type.getInternalName(Stored.Serialiser.class), Type.getInternalName(Stored.Deserialiser.class)};
    
    protected final Pluto pluto;
    protected final Class<?> type;
    protected final ClassWriter writer;
    protected final String internal, location;
    protected final Set<Field> fields = new LinkedHashSet<>();
    
    protected AutomaticSerialiser(Class<?> type, Pluto pluto) {
        this.type = type;
        this.pluto = pluto;
        this.writer = new ClassWriter(0);
        this.location = Type.getInternalName(type);
        this.internal = location + "$Serialiser";
        this.writer.visit(61, 0x0001 | 0x0010 | 0x1000 | 0x0020, internal, null, "java/lang/Object", SERIALISERS);
        this.writer.visitNestHost(location);
        this.writer.visitInnerClass(internal, location, "Serialiser", 0x0001 | 0x0008);
    }
    
    public void prepareFields(boolean all) {
        for (final Field field : type.getFields()) {
            final int modifiers = field.getModifiers();
            if ((modifiers & 0x00000008) != 0) continue; // static
            if ((modifiers & 0x00000080) != 0) continue; // transient
            this.fields.add(field);
        }
        if (!all) return;
        Class<?> found = type;
        do {
            for (final Field field : found.getDeclaredFields()) {
                final int modifiers = field.getModifiers();
                if ((modifiers & 0x00000008) != 0) continue; // static
                if ((modifiers & 0x00000080) != 0) continue; // transient
                this.fields.add(field);
            }
        } while ((found = type.getSuperclass()) != null && found != Object.class);
    }
    
    public void writeConstructor() {
        final MethodVisitor visitor = writer.visitMethod(0x00000001, "<init>", "()V", null, null);
        visitor.visitCode();
        visitor.visitVarInsn(25, 0);
        visitor.visitMethodInsn(183, "java/lang/Object", "<init>", "()V", false);
        visitor.visitInsn(177);
        visitor.visitMaxs(1, 1);
        visitor.visitEnd();
    }
    
    public void writeSerialiser() {
        boolean wide = false;
        final MethodVisitor visitor = writer.visitMethod(0x00000001, "run", "(Ljava/lang/Object;Ljava/io/DataOutputStream;Lmx/kenzie/pluto/Pluto;)V", null, null);
        final String output = "java/io/DataOutputStream";
        final short clash = pluto.clashCode(type);
        visitor.visitVarInsn(25, 2);
        visitor.visitIntInsn(17, clash);
        visitor.visitMethodInsn(182, output, "writeShort", "(I)V", false);
        for (final Field field : fields) {
            final Class<?> type = field.getType();
            if (type == long.class || type == double.class) wide = true;
            if (!type.isPrimitive() && type != String.class) {
                visitor.visitVarInsn(25, 3);
                visitor.visitLdcInsn(Type.getObjectType(Type.getInternalName(type)));
                visitor.visitMethodInsn(182, "mx/kenzie/pluto/Pluto", "getSerialiser", "(Ljava/lang/Class;)L" + SERIALISERS[0] + ";", false);
                visitor.visitFieldInsn(180, location, field.getName(), Type.getDescriptor(type));
                visitor.visitVarInsn(25, 2);
                visitor.visitVarInsn(25, 3);
                visitor.visitMethodInsn(185, SERIALISERS[0], "run", "(Ljava/lang/Object;Ljava/io/DataOutputStream;Lmx/kenzie/pluto/Pluto;)V", true);
            } else {
                visitor.visitVarInsn(25, 2);
                visitor.visitVarInsn(25, 1);
                visitor.visitTypeInsn(192, location);
                visitor.visitFieldInsn(180, location, field.getName(), Type.getDescriptor(type));
                if (type == String.class)
                    visitor.visitMethodInsn(182, output, "writeUTF", "(Ljava/lang/String;)V", false);
                else if (type == byte.class) visitor.visitMethodInsn(182, output, "writeByte", "(I)V", false);
                else if (type == short.class) visitor.visitMethodInsn(182, output, "writeShort", "(I)V", false);
                else if (type == int.class) visitor.visitMethodInsn(182, output, "writeInt", "(I)V", false);
                else if (type == long.class) visitor.visitMethodInsn(182, output, "writeLong", "(J)V", false);
                else if (type == float.class) visitor.visitMethodInsn(182, output, "writeFloat", "(F)V", false);
                else if (type == double.class) visitor.visitMethodInsn(182, output, "writeDouble", "(D)V", false);
                else if (type == char.class) visitor.visitMethodInsn(182, output, "writeChar", "(C)V", false);
                else if (type == boolean.class) visitor.visitMethodInsn(182, output, "writeBoolean", "(Z)V", false);
            }
        }
        visitor.visitInsn(177);
        visitor.visitMaxs(wide ? 5 : 4, 4);
        visitor.visitEnd();
    }
    
    public void writeDeserialiser() {
        boolean wide = false;
        final MethodVisitor visitor = writer.visitMethod(0x00000001, "run", "(Ljava/lang/Object;Ljava/io/DataInputStream;Lmx/kenzie/pluto/Pluto;)V", null, null);
        final String input = "java/io/DataInputStream"; // clash code already came off the stack
        for (final Field field : fields) {
            final Class<?> type = field.getType();
            if (type == long.class || type == double.class) wide = true;
            if (!type.isPrimitive() && type != String.class) {
                visitor.visitVarInsn(25, 3);
                visitor.visitMethodInsn(182, input, "readShort", "()S", false); // use short code to find serialiser
                visitor.visitInsn(87); // pop - we know what this is already
                visitor.visitLdcInsn(Type.getObjectType(Type.getInternalName(type)));
                visitor.visitMethodInsn(182, "mx/kenzie/pluto/Pluto", "getDeserialiser", "(Ljava/lang/Class;)L" + SERIALISERS[1] + ";", false);
                visitor.visitFieldInsn(180, location, field.getName(), Type.getDescriptor(type)); // assume non-primitive has a value already
                visitor.visitVarInsn(25, 2);
                visitor.visitVarInsn(25, 3);
                visitor.visitMethodInsn(185, SERIALISERS[0], "run", "(Ljava/lang/Object;Ljava/io/DataInputStream;Lmx/kenzie/pluto/Pluto;)V", true);
            } else {
                visitor.visitVarInsn(25, 1);
                visitor.visitTypeInsn(192, location);
                visitor.visitVarInsn(25, 2);
                if (type == String.class) visitor.visitMethodInsn(182, input, "readUTF", "()Ljava/lang/String;", false);
                else if (type == byte.class) visitor.visitMethodInsn(182, input, "readByte", "()B", false);
                else if (type == short.class) visitor.visitMethodInsn(182, input, "readShort", "()S", false);
                else if (type == int.class) visitor.visitMethodInsn(182, input, "readInt", "()I", false);
                else if (type == long.class) visitor.visitMethodInsn(182, input, "readLong", "()J", false);
                else if (type == float.class) visitor.visitMethodInsn(182, input, "readFloat", "()F", false);
                else if (type == double.class) visitor.visitMethodInsn(182, input, "readDouble", "()D", false);
                else if (type == char.class) visitor.visitMethodInsn(182, input, "readChar", "()C", false);
                else if (type == boolean.class) visitor.visitMethodInsn(182, input, "readBoolean", "()Z", false);
                visitor.visitFieldInsn(181, location, field.getName(), Type.getDescriptor(type));
            }
        }
        visitor.visitInsn(177);
        visitor.visitMaxs(wide ? 5 : 4, 4);
        visitor.visitEnd();
    }
    
    @SuppressWarnings("deprecation")
    public Object create() {
        try {
            final MethodHandles.Lookup lookup = MethodHandles.privateLookupIn(type, MethodHandles.lookup());
            final Class<?> thing = lookup.defineClass(writer.toByteArray());
            return thing.newInstance();
        } catch (IllegalAccessException | InstantiationException ex) {
            throw new RuntimeException(ex);
        }
    }
    
    
}

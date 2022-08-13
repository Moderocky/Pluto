package mx.kenzie.pluto;

import org.objectweb.asm.Label;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Type;

import java.lang.invoke.MethodHandles;
import java.lang.reflect.Field;
import java.util.concurrent.atomic.AtomicInteger;

class AutomaticSerialiser extends Marshalling {
    
    private static final String[] SERIALISERS = {Type.getInternalName(Stored.Serialiser.class), Type.getInternalName(Stored.Deserialiser.class)};
    private static final AtomicInteger COUNTER = new AtomicInteger();
    
    protected AutomaticSerialiser(Class<?> type, Pluto pluto) {
        super(pluto, type);
        try {
            Class.forName(type.getName() + "$Serialiser"); // check for collision
            this.key = "Serialiser";
        } catch (ClassNotFoundException ex) {
            this.key = "Serialiser" + COUNTER.incrementAndGet(); // won't collide, counter is global
        }
        this.internal = location + '$' + key;
        this.writer.visit(61, 0x0001 | 0x0010 | 0x1000 | 0x0020, internal, null, "java/lang/Object", SERIALISERS);
        this.writer.visitNestHost(location);
        this.writer.visitInnerClass(internal, location, key, 0x0001 | 0x0008);
    }
    
    @Override
    public void prepareFields(boolean all) {
        for (final Field field : type.getFields()) {
            final int modifiers = field.getModifiers();
            if (verify(modifiers)) fields.add(field);
        }
        if (!all) return;
        Class<?> found = type;
        do {
            for (final Field field : found.getDeclaredFields()) {
                final int modifiers = field.getModifiers();
                if (verify(modifiers)) fields.add(field);
            }
        } while ((found = type.getSuperclass()) != null && found != Object.class && found != Record.class);
    }
    
    @Override
    public void writeConstructor() {
        final MethodVisitor visitor = writer.visitMethod(0x00000001, "<init>", "()V", null, null);
        visitor.visitCode();
        visitor.visitVarInsn(25, 0);
        visitor.visitMethodInsn(183, "java/lang/Object", "<init>", "()V", false);
        visitor.visitInsn(177);
        visitor.visitMaxs(1, 1);
        visitor.visitEnd();
    }
    
    @Override
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
                final Label jump = new Label();
                visitor.visitVarInsn(25, 0); // X
                visitor.visitFieldInsn(180, location, field.getName(), Type.getDescriptor(type)); // V
                visitor.visitJumpInsn(198, jump);
                visitor.visitVarInsn(25, 0); // X
                visitor.visitFieldInsn(180, location, field.getName(), Type.getDescriptor(type)); // V
                visitor.visitVarInsn(25, 3);
                visitor.visitLdcInsn(Type.getObjectType(Type.getInternalName(type))); // T,P,C
                visitor.visitMethodInsn(182, "mx/kenzie/pluto/Pluto", "getSerialiser", "(Ljava/lang/Class;)L" + SERIALISERS[0] + ";", false); // T,Q
                visitor.visitInsn(95); // Q,V
                visitor.visitVarInsn(25, 2); // Q,V,S
                visitor.visitVarInsn(25, 3); // Q,V,S,P
                visitor.visitMethodInsn(185, SERIALISERS[0], "run", "(Ljava/lang/Object;Ljava/io/DataOutputStream;Lmx/kenzie/pluto/Pluto;)V", true);
                visitor.visitLabel(jump); // land with nothing
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
    
    @Override
    public void writeDeserialiser() {
        boolean wide = false;
        final MethodVisitor visitor = writer.visitMethod(0x00000001, "run", "(Ljava/lang/Object;Ljava/io/DataInputStream;Lmx/kenzie/pluto/Pluto;)V", null, null);
        final String input = "java/io/DataInputStream"; // clash code already came off the stack
        for (final Field field : fields) {
            final Label next = new Label();
            final Class<?> type = field.getType();
            if (type == long.class || type == double.class) wide = true;
            if (!type.isPrimitive() && type != String.class) {
                final Label jump = new Label();
                visitor.visitVarInsn(25, 3);
                visitor.visitVarInsn(25, 2); // P,S
                visitor.visitMethodInsn(182, input, "readShort", "()S", false); // use short code to find serialiser
                visitor.visitJumpInsn(154, jump);
                visitor.visitInsn(87); // pop Pluto
                visitor.visitJumpInsn(167, next); // go next - it's null
                visitor.visitLabel(jump);
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
            visitor.visitLabel(next);
        }
        visitor.visitInsn(177);
        visitor.visitMaxs(wide ? 5 : 4, 4);
        visitor.visitEnd();
    }
    
    @Override
    @SuppressWarnings("deprecation")
    public Object create() {
        this.bytecode = writer.toByteArray();
        try {
            final MethodHandles.Lookup lookup = MethodHandles.privateLookupIn(type, MethodHandles.lookup());
            final Class<?> thing = lookup.defineClass(writer.toByteArray());
            return thing.newInstance();
        } catch (IllegalAccessException | InstantiationException ex) {
            throw new RuntimeException(ex);
        }
    }
    
    private boolean verify(int modifiers) {
        if ((modifiers & 0x00000008) != 0) return false;
        if ((modifiers & 0x00000080) != 0) return false;
        return (modifiers & 0x00000010) == 0;
    }
    
    
}

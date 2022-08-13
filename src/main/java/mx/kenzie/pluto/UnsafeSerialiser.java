package mx.kenzie.pluto;

import org.objectweb.asm.Label;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Type;
import sun.misc.Unsafe;

import java.lang.invoke.MethodHandles;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;

public class UnsafeSerialiser extends Marshalling {
    
    private static final String[] SERIALISERS = {Type.getInternalName(Stored.Serialiser.class), Type.getInternalName(Stored.Deserialiser.class)};
    private static final AtomicInteger COUNTER = new AtomicInteger();
    
    protected final Unsafe unsafe;
    
    protected UnsafeSerialiser(Class<?> type, Pluto pluto, Unsafe unsafe) {
        super(pluto, type);
        if (unsafe == null) this.unsafe = FindUnsafe.getUnsafe();
        else this.unsafe = unsafe;
        try {
            Class.forName(type.getName() + "$UnsafeSerialiser"); // check for collision
            this.key = "UnsafeSerialiser";
        } catch (ClassNotFoundException ex) {
            this.key = "UnsafeSerialiser" + COUNTER.incrementAndGet(); // won't collide, counter is global
        }
        this.internal = location + '$' + key;
        this.writer.visit(61, 0x0001 | 0x0010 | 0x1000 | 0x0020, internal, null, "java/lang/Object", SERIALISERS);
        this.writer.visitNestHost(location);
        this.writer.visitInnerClass(internal, location, key, 0x0001 | 0x0008);
        this.writer.visitField(0x0002 | 0x0010, "unsafe", "Lsun/misc/Unsafe;", null, null).visitEnd();
        this.writer.visitField(0x0002 | 0x0010, "offsets", "[J", null, null).visitEnd();
    }
    
    @Override
    public void prepareFields(boolean all) {
        Class<?> found = type;
        do {
            for (final Field field : found.getDeclaredFields()) {
                final int modifiers = field.getModifiers();
                if ((modifiers & 0x00000008) != 0) continue; // static
                if ((modifiers & 0x00000080) != 0) continue; // transient
                this.fields.add(field);
            }
        } while ((found = type.getSuperclass()) != null && found != Object.class && found != Record.class);
    }
    
    @Override
    public void writeConstructor() {
        final MethodVisitor visitor = writer.visitMethod(0x0001, "<init>", "(Lsun/misc/Unsafe;[J)V", null, null);
        visitor.visitCode();
        visitor.visitVarInsn(25, 0);
        visitor.visitMethodInsn(183, "java/lang/Object", "<init>", "()V", false);
        visitor.visitVarInsn(25, 0);
        visitor.visitInsn(89);
        visitor.visitVarInsn(25, 1);
        visitor.visitFieldInsn(181, internal, "unsafe", "Lsun/misc/Unsafe;");
        visitor.visitVarInsn(25, 2);
        visitor.visitFieldInsn(181, internal, "offsets", "[J");
        visitor.visitInsn(177);
        visitor.visitMaxs(3, 3);
        visitor.visitEnd();
    }
    
    @Override
    public void writeSerialiser() {
        final MethodVisitor visitor = writer.visitMethod(0x00000001, "run", "(Ljava/lang/Object;Ljava/io/DataOutputStream;Lmx/kenzie/pluto/Pluto;)V", null, null);
        final String output = "java/io/DataOutputStream";
        final short clash = pluto.clashCode(type);
        visitor.visitVarInsn(25, 2);
        visitor.visitIntInsn(17, clash);
        visitor.visitMethodInsn(182, output, "writeShort", "(I)V", false);
        visitor.visitVarInsn(25, 0); // stack this
        visitor.visitFieldInsn(180, internal, "unsafe", "Lsun/misc/Unsafe;");
        int index = 0;
        for (final Field field : fields) {
            final Class<?> type = field.getType();
            final String[] parts = this.computationParts(type);
            visitor.visitInsn(89); // dup unsafe
            visitor.visitVarInsn(25, 2); // stack stream
            visitor.visitInsn(95); // swap stream/unsafe
            visitor.visitVarInsn(25, 1); // stack target
            visitor.visitVarInsn(25, 0); // stack this
            visitor.visitFieldInsn(180, internal, "offsets", "[J"); // load offsets
            visitor.visitIntInsn(16, index); // push index -> U,S,U,T,O,I
            visitor.visitInsn(47); // U,S,U,T,JJ
            if (type.isPrimitive()) {
                visitor.visitMethodInsn(182, "sun/misc/Unsafe", "get" + parts[0], "(Ljava/lang/Object;J)" + parts[1], false); // U,S,VV?
                visitor.visitMethodInsn(182, "java/io/DataOutputStream", "write" + parts[0], parts[2], false); // U
            } else if (type == String.class) { // need to cast
                visitor.visitMethodInsn(182, "sun/misc/Unsafe", "getObject", "(Ljava/lang/Object;J)Ljava/lang/Object;", false); // U,S,V
                visitor.visitTypeInsn(192, "java/lang/String"); // U,S,V
                visitor.visitMethodInsn(182, "java/io/DataOutputStream", "writeUTF", "(Ljava/lang/String;)V", false); // U
            } else {
                final Label jump = new Label(), next = new Label();
                visitor.visitMethodInsn(182, "sun/misc/Unsafe", "getObject", "(Ljava/lang/Object;J)Ljava/lang/Object;", false); // U,S,V
                visitor.visitInsn(89); // U,S,V,V
                visitor.visitJumpInsn(199, jump); // U,S,V
                visitor.visitInsn(88); // U
                visitor.visitJumpInsn(167, next);
                visitor.visitLabel(jump); // U,S,V
                visitor.visitVarInsn(25, 3); // load pluto U,S,V,P
                visitor.visitLdcInsn(Type.getObjectType(Type.getInternalName(type))); // U,S,V,P,C
                visitor.visitMethodInsn(182, "mx/kenzie/pluto/Pluto", "getSerialiser", "(Ljava/lang/Class;)L" + SERIALISERS[0] + ";", false); // U,S,V,Q
                visitor.visitInsn(95); // swap serialiser/value // U,S,Q,V
                visitor.visitVarInsn(25, 2); // U,S,Q,V,S
                visitor.visitVarInsn(25, 3); // U,S,Q,V,S,P
                visitor.visitMethodInsn(185, SERIALISERS[0], "run", "(Ljava/lang/Object;Ljava/io/DataOutputStream;Lmx/kenzie/pluto/Pluto;)V", true); // U,S
                visitor.visitInsn(87); // U
                visitor.visitLabel(next);
            }
            index++;
        }
        visitor.visitInsn(177);
        visitor.visitMaxs(6, 4); // max is always six because of the long offset :(
        visitor.visitEnd();
    }
    
    protected String[] computationParts(Class<?> type) {
        final String[] strings = new String[4];
        if (type == byte.class) {
            strings[0] = "Byte";
            strings[1] = "B";
            strings[2] = "(I)V";
        } else if (type == short.class) {
            strings[0] = "Short";
            strings[1] = "S";
            strings[2] = "(I)V";
        } else if (type == int.class) {
            strings[0] = "Int";
            strings[1] = "I";
            strings[2] = "(I)V";
        } else if (type == long.class) {
            strings[0] = "Long";
            strings[1] = "J";
            strings[2] = "(J)V";
        } else if (type == float.class) {
            strings[0] = "Float";
            strings[1] = "F";
            strings[2] = "(F)V";
        } else if (type == double.class) {
            strings[0] = "Double";
            strings[1] = "D";
            strings[2] = "(D)V";
        } else if (type == char.class) {
            strings[0] = "Char";
            strings[1] = "C";
            strings[2] = "(C)V";
        } else if (type == boolean.class) {
            strings[0] = "Boolean";
            strings[1] = "Z";
            strings[2] = "(Z)V";
        }
        return strings;
    }
    
    @Override
    public void writeDeserialiser() {
        int max = 6;
        final MethodVisitor visitor = writer.visitMethod(0x00000001, "run", "(Ljava/lang/Object;Ljava/io/DataInputStream;Lmx/kenzie/pluto/Pluto;)V", null, null);
        final String input = "java/io/DataInputStream"; // clash code already came off the stack
        visitor.visitVarInsn(25, 0); // stack this
        visitor.visitFieldInsn(180, internal, "unsafe", "Lsun/misc/Unsafe;"); // U
        int index = 0;
        for (final Field field : fields) {
            final Class<?> type = field.getType();
            if (max < 7 && (type == long.class || type == double.class)) max = 7;
            visitor.visitInsn(89); // dup unsafe U,U
            visitor.visitVarInsn(25, 1); // U,U,T
            visitor.visitVarInsn(25, 0); // U,U,T,X
            visitor.visitFieldInsn(180, internal, "offsets", "[J"); // U,U,T,O
            visitor.visitIntInsn(16, index); // // U,U,T,O,I
            visitor.visitInsn(47); // U,U,T,JJ
            if (type.isPrimitive()) {
                final String[] parts = this.computationParts(type);
                visitor.visitVarInsn(25, 2); // U,U,T,JJ,S
                visitor.visitMethodInsn(182, "java/io/DataInputStream", "read" + parts[0], "()" + parts[1], false); // U,U,T,JJ,VV?
                visitor.visitMethodInsn(182, "sun/misc/Unsafe", "put" + parts[0], "(Ljava/lang/Object;J" + parts[1] + ")V", false); // U
            } else if (type == String.class) {
                visitor.visitVarInsn(25, 2); // U,U,T,JJ,S
                visitor.visitMethodInsn(182, "java/io/DataInputStream", "readUTF", "()Ljava/lang/String;", false); // U,U,T,JJ,V
                visitor.visitMethodInsn(182, "sun/misc/Unsafe", "putObject", "(Ljava/lang/Object;JLjava/lang/Object;)V", false); // U
            } else {
                final Label skip = new Label(), jump = new Label(), next = new Label();
                max = 9;
                visitor.visitInsn(93); // U,U,JJ,T,JJ
                visitor.visitMethodInsn(182, "sun/misc/Unsafe", "getObject", "(Ljava/lang/Object;J)Ljava/lang/Object;", false); // U,U,JJ,V
                visitor.visitInsn(89); // U,U,JJ,V,V
                visitor.visitJumpInsn(199, skip);
                visitor.visitInsn(87);// U,U,JJ
                visitor.visitFieldInsn(180, internal, "unsafe", "Lsun/misc/Unsafe;"); // U,U,JJ,U
                visitor.visitLdcInsn(Type.getObjectType(Type.getInternalName(type))); // U,U,JJ,U,C
                visitor.visitMethodInsn(182, "sun/misc/Unsafe", "allocateInstance", "(Ljava/lang/Class;)Ljava/lang/Object;", false); // U,U,JJ,V
                visitor.visitLabel(skip); // U,U,JJ,V
                visitor.visitTypeInsn(192, Type.getInternalName(type)); // U,U,JJ,V
                visitor.visitInsn(89); // U,U,JJ,V,V
                visitor.visitVarInsn(25, 3); // U,U,JJ,V,V,P
                visitor.visitVarInsn(25, 2); // U,U,JJ,V,V,P,S
                visitor.visitMethodInsn(182, input, "readShort", "()S", false); // U,U,JJ,V,V,P,s
                visitor.visitJumpInsn(154, jump); // need to clear the stack before we jump // U,U,JJ,V,V,P
                visitor.visitInsn(87); // U,U,JJ,V,V
                visitor.visitInsn(88); // U,U,JJ
                visitor.visitInsn(88); // U,U
                visitor.visitInsn(87); // U
                visitor.visitJumpInsn(167, next); // go next - it's null
                visitor.visitLabel(jump);
                visitor.visitLdcInsn(Type.getObjectType(Type.getInternalName(type))); // U,U,JJ,V,V,P,C
                visitor.visitMethodInsn(182, "mx/kenzie/pluto/Pluto", "getDeserialiser", "(Ljava/lang/Class;)L" + SERIALISERS[1] + ";", false); // U,U,JJ,V,V,Q
                visitor.visitInsn(95); // U,U,JJ,V,Q,V
                visitor.visitVarInsn(25, 2); // U,U,JJ,V,Q,V,S
                visitor.visitVarInsn(25, 3); // U,U,JJ,V,Q,V,S,P
                visitor.visitMethodInsn(185, SERIALISERS[0], "run", "(Ljava/lang/Object;Ljava/io/DataInputStream;Lmx/kenzie/pluto/Pluto;)V", true); // U,U,JJ,V
                visitor.visitMethodInsn(182, "sun/misc/Unsafe", "putObject", "(Ljava/lang/Object;JLjava/lang/Object;)V", false); // U
                visitor.visitLabel(next); // land with U
            }
            index++;
        }
        visitor.visitInsn(177);
        visitor.visitMaxs(max, 4);
        visitor.visitEnd();
    }
    
    @Override
    public Object create() {
        final long[] offsets = FindUnsafe.fieldOffsets(fields);
        this.bytecode = writer.toByteArray();
        Class<?> thing = null;
        try {
            final MethodHandles.Lookup lookup = MethodHandles.privateLookupIn(type, MethodHandles.lookup());
            thing = lookup.defineClass(writer.toByteArray());
            final Constructor<?> constructor = thing.getConstructor(Unsafe.class, long[].class);
            return constructor.newInstance(unsafe, offsets);
        } catch (IllegalAccessException | InstantiationException | InvocationTargetException ex) {
            if (thing == null) throw new RuntimeException(ex);
            try { // backup creator
                final Object object = unsafe.allocateInstance(thing);
                this.unsafe.putObject(object, unsafe.objectFieldOffset(thing.getDeclaredField("unsafe")), unsafe);
                this.unsafe.putObject(object, unsafe.objectFieldOffset(thing.getDeclaredField("offsets")), offsets);
                return object;
            } catch (Throwable e) {
                throw new RuntimeException(e);
            }
        } catch (NoSuchMethodException ex) {
            throw new RuntimeException(ex);
        }
    }
}

class FindUnsafe {
    private static final Unsafe UNSAFE;
    
    static {
        try {
            final Field field = Unsafe.class.getDeclaredField("theUnsafe");
            field.setAccessible(true);
            UNSAFE = (Unsafe) field.get(null);
        } catch (Throwable ex) {
            throw new Error("Unable to find Java's unsafe.", ex);
        }
    }
    
    static long[] fieldOffsets(Set<Field> fields) {
        final long[] offsets = new long[fields.size()];
        int index = 0, length = 12;
        for (final Field field : fields) {
            if (field.getDeclaringClass().isRecord()) {
                offsets[index++] = length;
                final Class<?> type = field.getType();
                if (!type.isPrimitive()) length += 4;
                else if (type == long.class || type == double.class) length += 8;
                else if (type == int.class || type == float.class) length += 4;
                else if (type == char.class || type == short.class) length += 2;
                else length += 1;
            } else offsets[index++] = UNSAFE.objectFieldOffset(field);
        }
        return offsets;
    }
    
    
    static Unsafe getUnsafe() {
        return UNSAFE;
    }
}

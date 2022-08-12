package mx.kenzie.pluto;

import java.io.*;

public interface Stored {
    
    default void read(InputStream stream) {
        throw new Error("Untransformed storage class.");
    }
    
    default void write(OutputStream stream) {
        throw new Error("Untransformed storage class.");
    }
    
    @FunctionalInterface
    interface Serialiser<Type> {
        void run(Type thing, DataInputStream stream, Pluto pluto);
    }
    
    @FunctionalInterface
    interface Deserialiser<Type> {
        void run(Type thing, DataOutputStream stream, Pluto pluto);
    }
    
    
}

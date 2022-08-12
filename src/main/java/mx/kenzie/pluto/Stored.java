package mx.kenzie.pluto;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.InputStream;
import java.io.OutputStream;

public interface Stored {
    
    default void read(InputStream stream) {
        throw new Error("Untransformed storage class.");
    }
    
    default void write(OutputStream stream) {
        throw new Error("Untransformed storage class.");
    }
    
    @FunctionalInterface
    interface Serialiser {
        void run(Object thing, DataOutputStream stream, Pluto pluto);
    }
    
    @FunctionalInterface
    interface Deserialiser {
        void run(Object thing, DataInputStream stream, Pluto pluto);
    }
    
    
}

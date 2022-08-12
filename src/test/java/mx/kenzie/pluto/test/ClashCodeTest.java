package mx.kenzie.pluto.test;

import mx.kenzie.pluto.Pluto;
import org.junit.Test;

public class ClashCodeTest {
    
    @Test
    public void test() {
        final Pluto pluto = new Pluto();
        final short a = pluto.clashCode(ClashCodeTest.class);
        assert a == pluto.clashCode(ClashCodeTest.class);
    }
    
}

package fmpp;

import static org.junit.Assert.*;

import org.junit.Test;

public class EngineVersionTest {

    @Test
    public void getVersion() {
        assertNotNull(Engine.getVersion());
    }

    @Test
    public void getVersionNumber() {
        assertNotNull(Engine.getVersion());
    }
    
}

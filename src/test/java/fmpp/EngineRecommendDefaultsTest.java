package fmpp;

import static org.junit.Assert.*;

import org.junit.Test;

import freemarker.ext.beans.BeansWrapper;
import freemarker.template.Configuration;
import freemarker.template.DefaultObjectWrapper;
import freemarker.template.ObjectWrapper;
import freemarker.template.Version;

public class EngineRecommendDefaultsTest {

    @Test
    public void limits() {
        try {
            new Engine(new Version(0, 9, 14));
            fail();
        } catch (IllegalArgumentException e) {
            // Expected
        }
        
        try {
            Version curVer = Engine.getVersion();
            new Engine(new Version(curVer.getMajor(), curVer.getMinor(), curVer.getMicro() + 1));
            fail();
        } catch (IllegalArgumentException e) {
            // Expected
        }
    }
    
    @Test
    public void effectOnObjectWrapper() {
        {
            Engine engine = new Engine((Version) null);
            assertEquals(Engine.VERSION_0_9_15, engine.getRecommendedDefaults());
            assertEquals(Configuration.DEFAULT_INCOMPATIBLE_IMPROVEMENTS,
                    engine.getFreemarkerIncomplatibleImprovements());
            ObjectWrapper ow = engine.getFreemarkerConfiguration().getObjectWrapper();
            assertEquals(BeansWrapper.class, ow.getClass());
        }
        
        {
            Engine engine = new Engine(Engine.VERSION_0_9_16);
            assertEquals(Engine.VERSION_0_9_16, engine.getRecommendedDefaults());
            assertEquals(Configuration.VERSION_2_3_28, engine.getFreemarkerIncomplatibleImprovements());
            ObjectWrapper ow = engine.getFreemarkerConfiguration().getObjectWrapper();
            assertEquals(DefaultObjectWrapper.class, ow.getClass());
            assertEquals(Configuration.VERSION_2_3_27, ((DefaultObjectWrapper) ow).getIncompatibleImprovements());
            assertFalse(((DefaultObjectWrapper) ow).getForceLegacyNonListCollections());
            assertTrue(((DefaultObjectWrapper) ow).getIterableSupport());
        }
        
        {
            Engine engine = new Engine(Engine.VERSION_0_9_16, Configuration.VERSION_2_3_21, null);
            assertEquals(Configuration.VERSION_2_3_21, engine.getFreemarkerIncomplatibleImprovements());
            ObjectWrapper ow = engine.getFreemarkerConfiguration().getObjectWrapper();
            assertEquals(Configuration.VERSION_2_3_21, ((DefaultObjectWrapper) ow).getIncompatibleImprovements());
            assertFalse(((DefaultObjectWrapper) ow).getForceLegacyNonListCollections());
            assertTrue(((DefaultObjectWrapper) ow).getIterableSupport());
        }
        
        {
            Engine engine = new Engine(Engine.VERSION_0_9_16, null, new BeansWrapper(Configuration.VERSION_2_3_24));
            assertEquals(Engine.VERSION_0_9_16, engine.getRecommendedDefaults());
            assertEquals(Configuration.VERSION_2_3_28, engine.getFreemarkerIncomplatibleImprovements());
            ObjectWrapper ow = engine.getFreemarkerConfiguration().getObjectWrapper();
            assertEquals(BeansWrapper.class, ow.getClass());
            assertEquals(Configuration.VERSION_2_3_24, ((BeansWrapper) ow).getIncompatibleImprovements());
        }
        
    }
    
}

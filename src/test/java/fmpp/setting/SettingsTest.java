package fmpp.setting;

import static org.junit.Assert.*;

import java.io.File;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

import org.hamcrest.Matchers;
import org.junit.Before;
import org.junit.Test;

import freemarker.template.utility.NullArgumentException;

public class SettingsTest {
    
    private Settings settings;

    @Before
    public void setup() throws SettingException {
        settings = new Settings(new File("."));
        settings.define("num", Settings.TYPE_INTEGER, false, false);
        settings.define("seq", Settings.TYPE_SEQUENCE, true, false);
        settings.define("seqNoMerge", Settings.TYPE_SEQUENCE, false, false);
    }
    
    @Test
    public void testUndefinedSetting() {
        assertNull(settings.get("wrongName"));
        
        try {
            settings.set("wrongName", "x");
            fail();
        } catch (SettingException e) {
            // expected
        }
        
        try {
            settings.setDefault("wrongName", "x");
            fail();
        } catch (SettingException e) {
            // expected
        }
        
        try {
            settings.add("wrongName", "x");
            fail();
        } catch (SettingException e) {
            // expected
        }
        
        try {
            settings.addDefault("wrongName", "x");
            fail();
        } catch (SettingException e) {
            // expected
        }
    }

    @Test
    public void testWithIntegerSettingType() throws SettingException {
        assertNull(settings.get("num"));

        settings.set("num", 1);
        assertEquals(1, settings.get("num"));
        
        settings.set("num", 12);
        assertEquals(12, settings.get("num"));
        
        settings.add("num", 123);
        assertEquals(123, settings.get("num"));
        
        settings.setDefault("num", 1234);
        assertEquals(123, settings.get("num")); // Not changed

        try {
            settings.set("num", null);
            fail();
        } catch (NullArgumentException e) {
            // expected
        }
        
        settings.remove("num");
        assertNull(settings.get("num"));

        settings.setDefault("num", 1234);
        assertEquals(1234, settings.get("num")); // Now changed
        
        settings.setWithString("num", "1");
        assertEquals(1, settings.get("num"));
    }


    @Test
    public void testWithMergingSequenceSettingType() throws SettingException {
        assertNull(settings.get("seq"));

        settings.set("seq", Arrays.asList(1, 2, 3));
        assertEquals(Arrays.asList(1, 2, 3), settings.get("seq"));

        settings.set("seq", Arrays.asList(1, 2));
        assertEquals(Arrays.asList(1, 2), settings.get("seq"));

        settings.add("seq", Arrays.asList(0));
        assertEquals(Arrays.asList(0, 1, 2), settings.get("seq"));
        
        settings.setDefault("seq", Arrays.asList(3));
        assertEquals(Arrays.asList(0, 1, 2), settings.get("seq"));
        
        settings.addDefault("seq", Arrays.asList(3));
        assertEquals(Arrays.asList(0, 1, 2, 3), settings.get("seq"));
    }

    @Test
    public void testWithNonMergingSequenceSettingType() throws SettingException {
        assertNull(settings.get("seqNoMerge"));

        settings.set("seqNoMerge", Arrays.asList(1));
        assertEquals(Arrays.asList(1), settings.get("seqNoMerge"));

        settings.set("seqNoMerge", Arrays.asList(1, 2));
        assertEquals(Arrays.asList(1, 2), settings.get("seqNoMerge"));

        settings.add("seqNoMerge", Arrays.asList(0));
        assertEquals(Arrays.asList(0), settings.get("seqNoMerge"));
        
        settings.setDefault("seqNoMerge", Arrays.asList(1));
        assertEquals(Arrays.asList(0), settings.get("seqNoMerge"));
        
        settings.addDefault("seqNoMerge", Arrays.asList(1));
        assertEquals(Arrays.asList(0), settings.get("seqNoMerge"));
        
        settings.remove("seqNoMerge");
        settings.addDefault("seqNoMerge", Arrays.asList(1));
        assertEquals(Arrays.asList(1), settings.get("seqNoMerge"));
        
        settings.remove("seqNoMerge");
        settings.add("seqNoMerge", Arrays.asList(2));
        assertEquals(Arrays.asList(2), settings.get("seqNoMerge"));
    }

    @Test
    public void testSetWithMap() throws SettingException {
        {
            Map<String, Object> values = new HashMap<String, Object>();
            values.put("num", 0);
            values.put("seq", Arrays.asList(0));
            values.put("seqNoMerge", Arrays.asList(100));
            settings.setDefaults(values);
            assertEquals(0, settings.get("num"));
            assertEquals(Arrays.asList(0), settings.get("seq"));
            assertEquals(Arrays.asList(100), settings.get("seqNoMerge"));
        }
        {
            Map<String, Object> values = new HashMap<String, Object>();
            values.put("num", 1);
            values.put("seq", Arrays.asList(1));
            values.put("seqNoMerge", Arrays.asList(10));
            settings.set(values);
            assertEquals(1, settings.get("num"));
            assertEquals(Arrays.asList(1), settings.get("seq"));
            assertEquals(Arrays.asList(10), settings.get("seqNoMerge"));
        }
        {
            Map<String, Object> values = new HashMap<String, Object>();
            values.put("num", 2);
            values.put("seq", Arrays.asList(2));
            values.put("seqNoMerge", Arrays.asList(20));
            settings.add(values);
            assertEquals(2, settings.get("num"));
            assertEquals(Arrays.asList(2, 1), settings.get("seq"));
            assertEquals(Arrays.asList(20), settings.get("seqNoMerge"));
        }
        {
            Map<String, Object> values = new HashMap<String, Object>();
            values.put("num", 3);
            values.put("seq", Arrays.asList(3));
            values.put("seqNoMerge", Arrays.asList(30));
            settings.setDefaults(values);
            assertEquals(2, settings.get("num"));
            assertEquals(Arrays.asList(2, 1), settings.get("seq"));
            assertEquals(Arrays.asList(20), settings.get("seqNoMerge"));
        }
        {
            Map<String, Object> values = new HashMap<String, Object>();
            values.put("num", 3);
            values.put("seq", Arrays.asList(3));
            values.put("seqNoMerge", Arrays.asList(30));
            settings.addDefaults(values);
            assertEquals(2, settings.get("num"));
            assertEquals(Arrays.asList(2, 1, 3), settings.get("seq"));
            assertEquals(Arrays.asList(20), settings.get("seqNoMerge"));
        }
    }

    @Test
    public void testSetWithProperties() throws SettingException {
        {
            Properties values = new Properties();
            values.setProperty("num", "0");
            values.setProperty("seq", "0");
            values.setProperty("seqNoMerge", "100");
            settings.setDefaultsWithStrings(values);
            assertEquals(0, settings.get("num"));
            assertEquals(Arrays.asList(0), settings.get("seq"));
            assertEquals(Arrays.asList(100), settings.get("seqNoMerge"));
        }
        {
            Properties values = new Properties();
            values.setProperty("num", "1");
            values.setProperty("seq", "1");
            values.setProperty("seqNoMerge", "10");
            settings.setWithStrings(values);
            assertEquals(1, settings.get("num"));
            assertEquals(Arrays.asList(1), settings.get("seq"));
            assertEquals(Arrays.asList(10), settings.get("seqNoMerge"));
        }
        {
            Properties values = new Properties();
            values.setProperty("num", "2");
            values.setProperty("seq", "2");
            values.setProperty("seqNoMerge", "20");
            settings.addWithStrings(values);
            assertEquals(2, settings.get("num"));
            assertEquals(Arrays.asList(2, 1), settings.get("seq"));
            assertEquals(Arrays.asList(20), settings.get("seqNoMerge"));
        }
        {
            Properties values = new Properties();
            values.setProperty("num", "3");
            values.setProperty("seq", "3");
            values.setProperty("seqNoMerge", "30");
            settings.setDefaultsWithStrings(values);
            assertEquals(2, settings.get("num"));
            assertEquals(Arrays.asList(2, 1), settings.get("seq"));
            assertEquals(Arrays.asList(20), settings.get("seqNoMerge"));
        }
        {
            Properties values = new Properties();
            values.setProperty("num", "3");
            values.setProperty("seq", "3");
            values.setProperty("seqNoMerge", "30");
            settings.addDefaultsWithStrings(values);
            assertEquals(2, settings.get("num"));
            assertEquals(Arrays.asList(2, 1, 3), settings.get("seq"));
            assertEquals(Arrays.asList(20), settings.get("seqNoMerge"));
        }
    }

    @Test
    public void testIntegerTypeConversion() throws SettingException {
        settings.set("num", 1);
        assertEquals(1, settings.get("num"));
        
        settings.set("num", 2.0);
        assertEquals(2, settings.get("num"));
        
        try {
            settings.set("num", "3");
            fail();
        } catch (SettingException e) {
            assertThat(e.getMessage(), Matchers.containsString("string"));
        }
        settings.setWithString("num", "3");
        assertEquals(3, settings.get("num"));
    }

    @Test
    public void testSequenceTypeConversion() throws SettingException {
        settings.set("seq", new int[] { 1, 2 });
        assertEquals(Arrays.asList(1, 2), settings.get("seq"));
        
        settings.set("seq", "1, 2, 3");
        assertEquals(Arrays.asList("1, 2, 3"), settings.get("seq"));
            
        settings.setWithString("seq", "1, 2, 3");
        assertEquals(Arrays.asList(1, 2, 3), settings.get("seq"));
        
        settings.setWithString("seq", "[1, 2, 3]");
        assertEquals(Arrays.asList(1, 2, 3), settings.get("seq"));
    }
    
}

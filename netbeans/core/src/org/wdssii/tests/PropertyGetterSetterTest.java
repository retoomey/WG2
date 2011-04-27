package org.wdssii.tests;

import java.util.Properties;

import org.wdssii.core.PropertyGetterSetter;

import junit.framework.TestCase;

public class PropertyGetterSetterTest extends TestCase {

    public static class TestObject {

        private int myInteger = 1;
        private long myLong = 2L;
        private double myDouble = 3.0;
        private float myFloat = 4.0f;
        private boolean myBool = true;
        private String myString = "initial";
        private float[] myFloatArray = new float[]{0.5f, 1.5f, 2.0f};

        public float[] getMyFloatArray() {
            return myFloatArray;
        }

        public void setMyFloatArray(float[] myFloatArray) {
            this.myFloatArray = myFloatArray;
        }

        public boolean isMyBool() {
            return myBool;
        }

        public void setMyBool(boolean myBool) {
            this.myBool = myBool;
        }

        public double getMyDouble() {
            return myDouble;
        }

        public void setMyDouble(double myDouble) {
            this.myDouble = myDouble;
        }

        public float getMyFloat() {
            return myFloat;
        }

        public void setMyFloat(float myFloat) {
            this.myFloat = myFloat;
        }

        public int getMyInteger() {
            return myInteger;
        }

        public void setMyInteger(int myInteger) {
            this.myInteger = myInteger;
        }

        public long getMyLong() {
            return myLong;
        }

        public void setMyLong(long myLong) {
            this.myLong = myLong;
        }

        public String getMyString() {
            return myString;
        }

        public void setMyString(String myString) {
            this.myString = myString;
        }
    }

    /*
     * Test method for 'org.wdssii.core.PropertySetter.setProperties(Object, Properties)'
     */
    public void testSetPropertiesWithDefaults() {
        Properties defaults = new Properties();
        defaults.setProperty("myInteger", "10");
        defaults.setProperty("myLong", "11");
        defaults.setProperty("myFloat", "12.0");
        defaults.setProperty("myDouble", "13.0");
        defaults.setProperty("myBool", "true");
        defaults.setProperty("myString", "hello");
        defaults.setProperty("myFloatArray", "3.5 4.0 4.5");
        TestObject test = new TestObject();
        Properties props = new Properties(defaults);
        props.setProperty("myLong", "21");
        props.setProperty("myBool", "false");
        PropertyGetterSetter.setProperties(test, props);
        assertEquals(test.getMyDouble(), 13.0, 0.001);
        assertEquals(test.getMyFloat(), 12.0f, 0.001);
        assertEquals(test.getMyString(), "hello");
        assertEquals(test.getMyInteger(), 10);
        assertEquals(test.getMyLong(), 21);
        assertEquals(test.isMyBool(), false);
        assertEquals(test.myFloatArray.length, 3);
        assertEquals(test.myFloatArray[0], 3.5f, 0.001);
    }

    /*
     * Test method for 'org.wdssii.core.PropertySetter.setProperties(Object, Properties)'
     */
    public void testSetPropertiesWithoutDefaults() {
        TestObject test = new TestObject();
        Properties props = new Properties();
        props.setProperty("myLong", "21");
        props.setProperty("myBool", "false");
        PropertyGetterSetter.setProperties(test, props);
        assertEquals(test.getMyDouble(), 3.0, 0.001);
        assertEquals(test.getMyFloat(), 4.0f, 0.001);
        assertEquals(test.getMyString(), "initial");
        assertEquals(test.getMyInteger(), 1);
        assertEquals(test.getMyLong(), 21);
        assertEquals(test.isMyBool(), false);
        assertEquals(test.myFloatArray.length, 3);
        assertEquals(test.myFloatArray[0], 0.5f, 0.001);
    }

    /*
     * Test method for 'org.wdssii.core.PropertySetter.setProperties(Object, Properties)'
     */
    public void testGetProperties() {
        TestObject test = new TestObject();
        Properties props = PropertyGetterSetter.getProperties(test);
        assertEquals(props.getProperty("myInteger"), "1");
        assertEquals(props.getProperty("myLong"), "2");
        assertEquals(Double.parseDouble(props.getProperty("myDouble")), 3, 0.001);
        assertEquals(Float.parseFloat(props.getProperty("myFloat")), 4.0f, 0.001);
        assertEquals(Boolean.getBoolean(props.getProperty("myBool")), false);
        assertEquals(props.getProperty("myString"), "initial");
    }
}

/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package in.co.s13.SIPS.datastructure;

import in.co.s13.SIPS.datastructure.Property;
import java.util.Hashtable;

/**
 *
 * @author nika
 */
public class Resource {

    public static enum TYPE {
        STORAGE, CPU, GPU, MEMORY
    }

    public static enum CPU_ARCH {
        x86, x86_64, arm, arm64, HSA
    }

    public static enum STORAGE_TYPE {
        SSD, HDD, MAGNETIC, CD, DVD, USB
    }

    private Hashtable<String, Property> properties = new Hashtable<>();
    private TYPE type;
    private String name;

    public Resource(TYPE type) {
        this.type = type;
    }

    public Resource(TYPE type, String name) {
        this.type = type;
        this.name = name;
        this.addProperty(new Property("name", name));
    }

    public Hashtable<String, Property> getProperties() {
        return properties;
    }

    public void setProperties(Hashtable<String, Property> properties) {
        this.properties = properties;
    }

    public TYPE getType() {
        return type;
    }

    public void setType(TYPE type) {
        this.type = type;
    }

    public String getName() {
        return this.getProperty("name").getValue();
    }

    public void setName(String name) {
        this.setProperty(new Property("name", name));
    }

    public Property getProperty(String name) {
        return properties.get(name);
    }

    public void addProperty(Property newProperty) {
        properties.put(newProperty.getName(), newProperty);
    }

    public void setProperty(Property toUpdate) {
        properties.get(name).setValue(toUpdate.getValue());
    }

    @Override
    public String toString() {
        return "Resource:[" + "properties:" + properties + ", type:" + type + ", name:" + name + ']';
    }

}

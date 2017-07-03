/* 
 * Copyright (C) 2017 Navdeep Singh Sidhu
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
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
        properties.get(toUpdate.getName()).setValue(toUpdate.getValue());
    }

    public void removeProperty(Property toRemove) {
        properties.remove(toRemove.getName()).setValue(toRemove.getValue());
    }
       
    @Override
    public String toString() {
        return "Resource:[" + "properties:" + properties + ", type:" + type + ", name:" + name + ']';
    }

}

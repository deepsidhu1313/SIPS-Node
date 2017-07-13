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
package in.co.s13.SIPS.virtualdb;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Objects;
import org.json.JSONObject;

/**
 *
 * @author NAVDEEP SINGH SIDHU <navdeepsingh.sidhu95@gmail.com>
 */
public class NodeDBRow {

    private int queue_length;
    private String uuid, operatingSytem, hostname, processor_name;
    private long memory, hdd_size;
    private ArrayList<String> ipAddresses;

    public NodeDBRow(String uuid, String host, String os, String processor, int qlen,
            long ram, long hdd_size) {
        this.uuid = uuid;
        this.operatingSytem = os;
        this.hostname = host;
        this.queue_length = qlen;
        this.memory = ram;
        this.processor_name = processor;
        this.hdd_size = hdd_size;
    }

    public String getUuid() {
        return uuid;
    }

    public void setUuid(String uuid) {
        this.uuid = uuid;
    }

    public String getOperatingSytem() {
        return operatingSytem;
    }

    public void setOperatingSytem(String os) {
        this.operatingSytem = os;
    }

    public String getHostname() {
        return hostname;
    }

    public void setHostname(String hostname) {
        this.hostname = hostname;
    }

    public int getQueue_length() {
        return queue_length;
    }

    public void setQueue_length(int length) {
        this.queue_length = length;
    }

    public long getHdd_size() {
        return hdd_size;
    }

    public void setHdd_size(long hdd_size) {
        this.hdd_size = hdd_size;
    }

    public long getMemory() {
        return memory;
    }

    public void setMemory(long Memory) {
        this.memory = Memory;
    }

    public ArrayList<String> getIpAddresses() {
        return ipAddresses;
    }

    public void setIpAddresses(ArrayList<String> ipAddresses) {
        this.ipAddresses = ipAddresses;
    }

    public String getProcessor_name() {
        return processor_name;
    }

    public void setProcessor_name(String name) {
        this.processor_name = name;
    }

    public void addIp(String ip) {
        if (!this.ipAddresses.contains(ip)) {
            this.ipAddresses.add(ip);
        }
    }

    public boolean removeIp(String ip) {
        return this.ipAddresses.remove(ip);
    }

    @Override
    public String toString() {
        return "NodeDBRow:["
                + " uuid: " + uuid
                + ", que_length:" + queue_length
                + ", operatingSytem:" + operatingSytem
                + ", hostname:" + hostname
                + ", processor_name:" + processor_name
                + ", memory:" + memory + ']';
    }

    public JSONObject toJSON() {
        JSONObject jSONObject = new JSONObject();
        jSONObject.put("uuid", uuid);
        jSONObject.put("queue_length", queue_length);
        jSONObject.put("operatingSystem", operatingSytem);
        jSONObject.put("hostname", hostname);
        jSONObject.put("processor_name", processor_name);
        jSONObject.put("memory", memory);
        jSONObject.put("hdd_size", hdd_size);
        jSONObject.put("ip", ipAddresses.toString());

        return jSONObject;
    }

    @Override
    public int hashCode() {
        int hash = 3;
        hash = 31 * hash + Objects.hashCode(this.uuid);
        hash = 31 * hash + Objects.hashCode(this.operatingSytem);
        hash = 31 * hash + Objects.hashCode(this.hostname);
        hash = 31 * hash + Objects.hashCode(this.processor_name);
        hash = 31 * hash + (int) (this.memory ^ (this.memory >>> 32));
        return hash;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        final NodeDBRow other = (NodeDBRow) obj;

        if (!Objects.equals(this.uuid, other.uuid)) {
            return false;
        }
        return true;
    }

   public enum NodeDBRowComparator implements Comparator<NodeDBRow> {

        UUID_SORT {
            public int compare(NodeDBRow o1, NodeDBRow o2) {
                return o1.getUuid().compareTo(o2.getUuid());
            }
        },
        OS_SORT {
            public int compare(NodeDBRow o1, NodeDBRow o2) {
                return (o1.getOperatingSytem()).compareTo(o2.getOperatingSytem());
            }
        },
        HOST_SORT {
            public int compare(NodeDBRow o1, NodeDBRow o2) {
                return (o1.getHostname()).compareTo(o2.getHostname());
            }
        },
        QLEN_SORT {
            public int compare(NodeDBRow o1, NodeDBRow o2) {
                return Integer.valueOf(o1.getQueue_length()).compareTo(o2.getQueue_length());
            }
        },
        RAM_SORT {
            public int compare(NodeDBRow o1, NodeDBRow o2) {
                return Long.valueOf(o1.getMemory()).compareTo(o2.getMemory());
            }
        },
        PROCESSOR_SORT {
            public int compare(NodeDBRow o1, NodeDBRow o2) {
                return (o1.getProcessor_name()).compareTo(o2.getProcessor_name());
            }
        };

        public static Comparator<NodeDBRow> decending(final Comparator<NodeDBRow> other) {
            return new Comparator<NodeDBRow>() {
                public int compare(NodeDBRow o1, NodeDBRow o2) {
                    return -1 * other.compare(o1, o2);
                }
            };
        }

        public static Comparator<NodeDBRow> getComparator(final NodeDBRowComparator... multipleOptions) {
            return new Comparator<NodeDBRow>() {
                public int compare(NodeDBRow o1, NodeDBRow o2) {
                    for (NodeDBRowComparator option : multipleOptions) {
                        int result = option.compare(o1, o2);
                        if (result != 0) {
                            return result;
                        }
                    }
                    return 0;
                }
            };
        }
    }
}

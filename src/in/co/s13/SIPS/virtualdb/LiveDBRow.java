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

import in.co.s13.SIPS.Scanner.NetScanner;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Objects;
import org.json.JSONArray;
import org.json.JSONObject;

/**
 *
 * @author NAVDEEP SINGH SIDHU <navdeepsingh.sidhu95@gmail.com>
 */
public class LiveDBRow {

    private int que_length, waiting_in_que;
    private String uuid, operatingSytem, hostname, processor_name;

    private long memory, free_memory, hdd_size, hdd_free;
    private ArrayList<String> ipAddresses = new ArrayList<>();
    private JSONObject benchmarking_results;

    public LiveDBRow(String uuid, String host, String os, String processor, int qlen,
            int qwait, long ram, long free_memory, long hdd_size, long hdd_free, JSONObject benchmarking_results) {
        this.uuid = uuid;
        this.operatingSytem = os;
        this.hostname = host;
        this.que_length = qlen;
        this.waiting_in_que = qwait;
        this.memory = ram;
        this.free_memory = free_memory;
        this.processor_name = processor;
        this.hdd_size = hdd_size;
        this.hdd_free = hdd_free;
        this.benchmarking_results = benchmarking_results;
    }

    public LiveDBRow(JSONObject livedbRow) {
        uuid = livedbRow.getString("uuid");
        que_length = livedbRow.getInt("que_length");
        waiting_in_que = livedbRow.getInt("waiting_in_que");
        operatingSytem = livedbRow.getString("operatingSytem");
        hostname = livedbRow.getString("hostname");
        processor_name = livedbRow.getString("processor_name");
        memory = livedbRow.getLong("memory");
        free_memory = livedbRow.getLong("free_memory");
        hdd_size = livedbRow.getLong("hdd_size");
        hdd_free = livedbRow.getLong("hdd_free");
        JSONArray array= livedbRow.getJSONArray("ipAddresses");
        for (int i = 0; i < array.length(); i++) {
            String ip = array.getString(i);
            addIp(ip);
            NetScanner.addip(ip);
        }
        benchmarking_results = livedbRow.getJSONObject("benchmarking_results");
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

    public int getQue_length() {
        return que_length;
    }

    public void setQue_length(int length) {
        this.que_length = length;
    }

    public int getWaiting_in_que() {
        return waiting_in_que;
    }

    public void setWaiting_in_que(int alreadyInQue) {

        this.waiting_in_que = alreadyInQue;
    }

    public long getMemory() {
        return memory;
    }

    public void setMemory(long Memory) {
        this.memory = Memory;
    }

    public long getFree_memory() {
        return free_memory;
    }

    public void setFree_memory(long free_memory) {
        this.free_memory = free_memory;
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

    public long getHdd_size() {
        return hdd_size;
    }

    public void setHdd_size(long hdd_size) {
        this.hdd_size = hdd_size;
    }

    public long getHdd_free() {
        return hdd_free;
    }

    public void setHdd_free(long hdd_free) {
        this.hdd_free = hdd_free;
    }

    public void addIp(String ip) {
        if (!this.ipAddresses.contains(ip)) {
            this.ipAddresses.add(ip);
        }
    }

    public boolean removeIp(String ip) {
        return this.ipAddresses.remove(ip);
    }

    public JSONObject getBenchmarking_results() {
        return benchmarking_results;
    }

    public void setBenchmarking_results(JSONObject benchmarking_results) {
        this.benchmarking_results = benchmarking_results;
    }

    @Override
    public String toString() {
        return this.toJSON().toString(4);
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

    public JSONObject toJSON() {
        JSONObject result = new JSONObject();
        result.put("uuid", uuid);
        result.put("que_length", que_length);
        result.put("waiting_in_que", waiting_in_que);
        result.put("operatingSytem", operatingSytem);
        result.put("hostname", hostname);
        result.put("processor_name", processor_name);
        result.put("memory", memory);
        result.put("free_memory", free_memory);
        result.put("hdd_size", hdd_size);
        result.put("hdd_free", hdd_free);
        result.put("ipAddresses", new JSONArray(ipAddresses));
        result.put("benchmarking_results", benchmarking_results);
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        final LiveDBRow other = (LiveDBRow) obj;

        if (!Objects.equals(this.uuid, other.uuid)) {
            return false;
        }
        return true;
    }

    enum LiveDBRowComparator implements Comparator<LiveDBRow> {

        IP_SORT {
            public int compare(LiveDBRow o1, LiveDBRow o2) {
                return o1.getUuid().compareTo(o2.getUuid());
            }
        },
        OS_SORT {
            public int compare(LiveDBRow o1, LiveDBRow o2) {
                return (o1.getOperatingSytem()).compareTo(o2.getOperatingSytem());
            }
        },
        HOST_SORT {
            public int compare(LiveDBRow o1, LiveDBRow o2) {
                return (o1.getHostname()).compareTo(o2.getHostname());
            }
        },
        QLEN_SORT {
            public int compare(LiveDBRow o1, LiveDBRow o2) {
                return Integer.valueOf(o1.getQue_length()).compareTo(o2.getQue_length());
            }
        },
        QWAIT_SORT {
            public int compare(LiveDBRow o1, LiveDBRow o2) {
                return Integer.valueOf(o1.getWaiting_in_que()).compareTo(o2.getWaiting_in_que());
            }
        },
        RAM_SORT {
            public int compare(LiveDBRow o1, LiveDBRow o2) {
                return Long.valueOf(o1.getMemory()).compareTo(o2.getMemory());
            }
        },
        FREE_RAM_SORT {
            public int compare(LiveDBRow o1, LiveDBRow o2) {
                return Long.valueOf(o1.getFree_memory()).compareTo(o2.getFree_memory());
            }
        },
        PROCESSOR_SORT {
            public int compare(LiveDBRow o1, LiveDBRow o2) {
                return (o1.getProcessor_name()).compareTo(o2.getProcessor_name());
            }
        };

        public static Comparator<LiveDBRow> decending(final Comparator<LiveDBRow> other) {
            return new Comparator<LiveDBRow>() {
                public int compare(LiveDBRow o1, LiveDBRow o2) {
                    return -1 * other.compare(o1, o2);
                }
            };
        }

        public static Comparator<LiveDBRow> getComparator(final LiveDBRowComparator... multipleOptions) {
            return new Comparator<LiveDBRow>() {
                public int compare(LiveDBRow o1, LiveDBRow o2) {
                    for (LiveDBRowComparator option : multipleOptions) {
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

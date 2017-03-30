/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package in.co.s13.SIPS.virtualdb;

import java.util.Comparator;
import java.util.Objects;

/**
 *
 * @author NAVDEEP SINGH SIDHU <navdeepsingh.sidhu95@gmail.com>
 */
public class LiveDBRow {

    private int que_length, waiting_in_que;
    private String ip, operatingSytem, hostname, processor_name, cluster_name;
    private double performance;
    private long memory;

    public LiveDBRow(String ip, double prfm, String cluster, String host, String os, String processor, int qlen,
            int qwait, long ram) {
        this.ip = ip;
        this.operatingSytem = os;
        this.hostname = host;
        this.que_length = qlen;
        this.waiting_in_que = qwait;
        this.memory = ram;
        this.performance = prfm;
        this.processor_name = processor;
        this.cluster_name = cluster;
    }

    public String getIp() {
        return ip;
    }

    public void setIp(String ip) {
        this.ip = ip;
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

    public void setHostname(String vartype) {
        this.hostname = vartype;
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

    public double getPerformance() {
        return performance;
    }

    public void setPerformance(double performance) {
        this.performance = performance;
    }

    public String getProcessor_name() {
        return processor_name;
    }

    public void setProcessor_name(String name) {
        this.processor_name = name;
    }

    public String getCluster_name() {
        return cluster_name;
    }

    public void setCluster_name(String name) {
        this.cluster_name = name;
    }

    @Override
    public String toString() {
        return "LiveDBRow:[" + "que_length:" + que_length + ", waiting_in_que:" + waiting_in_que + ", ip:" + ip + ", operatingSytem:" + operatingSytem + ", hostname:" + hostname + ", processor_name:" + processor_name + ", cluster_name:" + cluster_name + ", performance:" + performance + ", memory:" + memory + ']';
    }

    @Override
    public int hashCode() {
        int hash = 3;
        hash = 31 * hash + Objects.hashCode(this.ip);
        hash = 31 * hash + Objects.hashCode(this.operatingSytem);
        hash = 31 * hash + Objects.hashCode(this.hostname);
        hash = 31 * hash + Objects.hashCode(this.processor_name);
        hash = 31 * hash + Objects.hashCode(this.cluster_name);
        hash = 31 * hash + (int) (this.memory ^ (this.memory >>> 32));
        return hash;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        final LiveDBRow other = (LiveDBRow) obj;
        if (this.memory != other.memory) {
            return false;
        }
        if (!Objects.equals(this.ip, other.ip)) {
            return false;
        }
        if (!Objects.equals(this.operatingSytem, other.operatingSytem)) {
            return false;
        }
        if (!Objects.equals(this.hostname, other.hostname)) {
            return false;
        }
        if (!Objects.equals(this.processor_name, other.processor_name)) {
            return false;
        }
        if (!Objects.equals(this.cluster_name, other.cluster_name)) {
            return false;
        }
        return true;
    }

    
    enum LiveDBRowComparator implements Comparator<LiveDBRow> {

        IP_SORT {
            public int compare(LiveDBRow o1, LiveDBRow o2) {
                return o1.getIp().compareTo(o2.getIp());
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
        PRFM_SORT {
            public int compare(LiveDBRow o1, LiveDBRow o2) {
                return Double.valueOf(o1.getPerformance()).compareTo(o2.getPerformance());
            }
        },
        PROCESSOR_SORT {
            public int compare(LiveDBRow o1, LiveDBRow o2) {
                return (o1.getProcessor_name()).compareTo(o2.getProcessor_name());
            }
        },
        CLUSTER_SORT {
            public int compare(LiveDBRow o1, LiveDBRow o2) {
                return (o1.getCluster_name()).compareTo(o2.getCluster_name());
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

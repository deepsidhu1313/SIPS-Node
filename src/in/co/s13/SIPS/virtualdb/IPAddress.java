/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package in.co.s13.SIPS.virtualdb;

import java.util.Comparator;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleLongProperty;
import javafx.beans.property.SimpleStringProperty;

/**
 * 
 * @author NAVDEEP SINGH SIDHU <navdeepsingh.sidhu95@gmail.com>
 */
public class IPAddress {

        private final SimpleStringProperty firstName;
        private final SimpleStringProperty operatingSystem;
        private final SimpleStringProperty nStatus;
        private final SimpleStringProperty hostName;
        private final SimpleIntegerProperty processLimit;
        private final SimpleIntegerProperty processWaiting;
        private final SimpleLongProperty totalMem;
        private final SimpleDoubleProperty cpuLoad;
        private final SimpleStringProperty cpuName;
        private final SimpleStringProperty clusterName;

        public IPAddress(String fName, String s, String os, String hn, Integer pl, Integer pw, Long tm, Double ld, String cpn, String cln) {
            this.firstName = new SimpleStringProperty(fName);
            this.nStatus = new SimpleStringProperty(s);
            this.operatingSystem = new SimpleStringProperty(os);
            this.hostName = new SimpleStringProperty(hn);
            this.processLimit = new SimpleIntegerProperty(pl);
            this.processWaiting = new SimpleIntegerProperty(pw);
            this.totalMem = new SimpleLongProperty(tm);
            this.cpuLoad = new SimpleDoubleProperty(ld);
            this.cpuName = new SimpleStringProperty(cpn);
            this.clusterName = new SimpleStringProperty(cln);
        }

        public String getFirstName() {
            return firstName.get();
        }

        public void setFirstName(String fName) {
            firstName.set(fName);
        }

        public String getNStatus() {
            return nStatus.get();
        }

        public void setNStatus(String fName) {
            nStatus.set(fName);
        }

        public String getOperatingSystem() {
            return operatingSystem.get();
        }

        public void setOperatingSystem(String fName) {
            operatingSystem.set(fName);
        }

        public String getHostName() {
            return hostName.get();
        }

        public void setHostName(String fName) {
            hostName.set(fName);
        }

        public int getProcessLimit() {
            return processLimit.get();
        }

        public void setProcessLimit(int fName) {
            processLimit.set(fName);
        }

        public int getProcessWaiting() {
            return processWaiting.get();
        }

        public void setProcessWaiting(int fName) {
            processWaiting.set(fName);
        }

        public long getTotalMem() {
            return totalMem.get();
        }

        public void setTotalMem(long fName) {
            totalMem.set(fName);
        }

        public double getCpuLoad() {
            return cpuLoad.get();
        }

        public void setCpuLoad(double fName) {
            cpuLoad.set(fName);
        }

        public String getCpuName() {
            return cpuName.get();
        }

        public void setCpuName(String fName) {
            cpuName.set(fName);
        }

        public String getClusterName() {
            return clusterName.get();
        }

        public void setClusterName(String fName) {
            clusterName.set(fName);
        }

        @Override
    public String toString() {
        return "" +  firstName.get() + "\t"  + nStatus.get()+ "\t" + operatingSystem.get()+ "\t" +hostName.get()+ "\t" + processLimit.get()+ "\t" 
                +processWaiting.get()+ "\t" + totalMem.get()+ "\t" +cpuLoad.get()+ "\t" +cpuName.get()+ "\t" + clusterName.get() + "\n";

    }

    enum IPAddressComparator implements Comparator<IPAddress> {

        IP_SORT {
                    public int compare(IPAddress o1, IPAddress o2) {
                        return o1.getFirstName().compareTo(o2.getFirstName());
                    }
                },
        LEN_SORT {
                    public int compare(IPAddress o1, IPAddress o2) {
                        return Integer.valueOf(o1.getFirstName().length()).compareTo(o2.getFirstName().length());
                    }
                },
        STATUS_SORT {
                    public int compare(IPAddress o1, IPAddress o2) {
                        return (o1.getNStatus()).compareTo(o2.getNStatus());
                    }
                },
        OS_SORT {
                    public int compare(IPAddress o1, IPAddress o2) {
                        return (o1.getOperatingSystem()).compareTo(o2.getOperatingSystem());
                    }
                },
        HOST_SORT {
                    public int compare(IPAddress o1, IPAddress o2) {
                        return (o1.getHostName()).compareTo(o2.getHostName());
                    }
                },
        QLEN_SORT {
                    public int compare(IPAddress o1, IPAddress o2) {
                        return Integer.valueOf(o1.getProcessLimit()).compareTo(o2.getProcessLimit());
                    }
                },
        QWAIT_SORT {
                    public int compare(IPAddress o1, IPAddress o2) {
                        return Integer.valueOf(o1.getProcessWaiting()).compareTo(o2.getProcessWaiting());
                    }
                },
        RAM_SORT {
                    public int compare(IPAddress o1, IPAddress o2) {
                        return Long.valueOf(o1.getTotalMem()).compareTo(o2.getTotalMem());
                    }
                },
        PRFM_SORT {
                    public int compare(IPAddress o1, IPAddress o2) {
                        return Double.valueOf(o1.getCpuLoad()).compareTo(o2.getCpuLoad());
                    }
                },
        PROCESSOR_SORT {
                    public int compare(IPAddress o1, IPAddress o2) {
                        return (o1.getCpuName()).compareTo(o2.getCpuName());
                    }
                },
        CLUSTER_SORT {
                    public int compare(IPAddress o1, IPAddress o2) {
                        return (o1.getClusterName()).compareTo(o2.getClusterName());
                    }
                };

        public static Comparator<IPAddress> decending(final Comparator<IPAddress> other) {
            return new Comparator<IPAddress>() {
                public int compare(IPAddress o1, IPAddress o2) {
                    return -1 * other.compare(o1, o2);
                }
            };
        }

        public static Comparator<IPAddress> getComparator(final IPAddressComparator... multipleOptions) {
            return new Comparator<IPAddress>() {
                public int compare(IPAddress o1, IPAddress o2) {
                    for (IPAddressComparator option : multipleOptions) {
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

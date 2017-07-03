/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package in.co.s13.SIPS.virtualdb;

import java.util.Comparator;
import javafx.beans.property.ReadOnlyDoubleWrapper;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleLongProperty;
import javafx.beans.property.SimpleStringProperty;

/**
 *
 */
public class LiveNode {

    private SimpleStringProperty name;
    private ReadOnlyDoubleWrapper salary;
    //private ReadOnlyDoubleWrapper salaryPercentage;
    private SimpleStringProperty clusterName;
    private SimpleStringProperty hostName;
    private SimpleStringProperty osName;
    private SimpleStringProperty cpuName;
    private SimpleIntegerProperty pLimit;
    private SimpleIntegerProperty pWait;
    private SimpleLongProperty tMem;

    public SimpleStringProperty nameProperty() {
        if (name == null) {
            name = new SimpleStringProperty(this, "name");
        }
        return name;
    }

    public SimpleStringProperty clusterNameProperty() {
        if (clusterName == null) {
            clusterName = new SimpleStringProperty(this, "clusterName");
        }
        return clusterName;
    }

    public SimpleStringProperty hostNameProperty() {
        if (hostName == null) {
            hostName = new SimpleStringProperty(this, "hostName");
        }
        return hostName;
    }

    public SimpleStringProperty osNameProperty() {
        if (osName == null) {
            osName = new SimpleStringProperty(this, "osName");
        }
        return osName;
    }

    public SimpleStringProperty cpuNameProperty() {
        if (cpuName == null) {
            cpuName = new SimpleStringProperty(this, "cpuName");
        }
        return cpuName;
    }

    public SimpleIntegerProperty pLimitProperty() {
        if (pLimit == null) {
            pLimit = new SimpleIntegerProperty(this, "pLimit");
        }
        return pLimit;
    }

    public SimpleIntegerProperty pWaitProperty() {
        if (pWait == null) {
            pWait = new SimpleIntegerProperty(this, "pWait");
        }
        return pWait;
    }

    public SimpleLongProperty tMemProperty() {
        if (tMem == null) {
            tMem = new SimpleLongProperty(this, "tMem");
        }
        return tMem;
    }

    public ReadOnlyDoubleWrapper salaryProperty() {
        if (salary == null) {
            salary = new ReadOnlyDoubleWrapper(this, "salary");
        }
        return salary;
    }

    /*public ReadOnlyDoubleWrapper salaryPercentProperty() {
        if (salaryPercentage == null) {
            salaryPercentage = new ReadOnlyDoubleWrapper(this, "salaryPercentage");
        }
        return salaryPercentage;
    }
*/
    public LiveNode(String name, Double salary,  String clusterName, String hostName, String osName, String cpuName, Integer pLimit, Integer pWait, Long tMem) {
        this.name = new SimpleStringProperty(name);
        this.salary = new ReadOnlyDoubleWrapper(salary);
    //    this.salaryPercentage = new ReadOnlyDoubleWrapper(salaryPercentage);
        this.clusterName = new SimpleStringProperty(clusterName);
        this.hostName = new SimpleStringProperty(hostName);
        this.osName = new SimpleStringProperty(osName);
        this.cpuName = new SimpleStringProperty(cpuName);
        this.pLimit = new SimpleIntegerProperty(pLimit);
        this.pWait = new SimpleIntegerProperty(pWait);
        this.tMem = new SimpleLongProperty(tMem);
    }

    public String getName() {
        return name.get();
    }

    public void setName(String fName) {
        name.set(fName);
    }

    public String getClusterName() {
        return clusterName.get();
    }

    public void setClusterName(String fName) {
        clusterName.set(fName);
    }

    public String getHostName() {
        return hostName.get();
    }

    public void setHostName(String fName) {
        hostName.set(fName);
    }

    public String getOsName() {
        return osName.get();
    }

    public void setOsName(String fName) {
        osName.set(fName);
    }

    public String getCpuName() {
        return cpuName.get();
    }

    public void setCpuName(String fName) {
        cpuName.set(fName);
    }

    public Integer getPLimit() {
        return pLimit.get();
    }

    public void setPLimit(Integer fName) {
        pLimit.set(fName);
    }

    public Integer getPWait() {
        return pWait.get();
    }

    public void setPWait(Integer fName) {
        pWait.set(fName);
    }

    public Long getTMem() {
        return tMem.get();
    }

    public void setTMem(Long fName) {
        tMem.set(fName);
    }
    
    public Double getSalary() {
        return salary.get();
    }

    public void setSalary(Double fName) {
        salary.set(fName);
    }
/*
    public Double getSalaryPercentage() {
        return salaryPercentage.get();
    }

    public void setSalaryPercentage(Double fName) {
        salaryPercentage.set(fName);
    }*/
    
  public enum LiveNodeComparator implements Comparator<LiveNode> {

        IP_SORT {
                    public int compare(LiveNode o1, LiveNode o2) {
                        return o1.getName().compareTo(o2.getName());
                    }
                },
        OS_SORT {
                    public int compare(LiveNode o1, LiveNode o2) {
                        return (o1.getOsName()).compareTo(o2.getOsName());
                    }
                },
        HOST_SORT {
                    public int compare(LiveNode o1, LiveNode o2) {
                        return (o1.getHostName()).compareTo(o2.getHostName());
                    }
                },
        QLEN_SORT {
                    public int compare(LiveNode o1, LiveNode o2) {
                        return Integer.valueOf(o1.getPLimit()).compareTo(o2.getPLimit());
                    }
                },
        QWAIT_SORT {
                    public int compare(LiveNode o1, LiveNode o2) {
                        return Integer.valueOf(o1.getPWait()).compareTo(o2.getPWait());
                    }
                },
        RAM_SORT {
                    public int compare(LiveNode o1, LiveNode o2) {
                        return Long.valueOf(o1.getTMem()).compareTo(o2.getTMem());
                    }
                },
        PRFM_SORT {
                    public int compare(LiveNode o1, LiveNode o2) {
                        return Double.valueOf(o1.getSalary()).compareTo(o2.getSalary());
                    }
                },
        PROCESSOR_SORT {
                    public int compare(LiveNode o1, LiveNode o2) {
                        return (o1.getCpuName()).compareTo(o2.getCpuName());
                    }
                },
        CLUSTER_SORT {
                    public int compare(LiveNode o1, LiveNode o2) {
                        return (o1.getClusterName()).compareTo(o2.getClusterName());
                    }
                };

        public static Comparator<LiveNode> decending(final Comparator<LiveNode> other) {
            return new Comparator<LiveNode>() {
                public int compare(LiveNode o1, LiveNode o2) {
                    return -1 * other.compare(o1, o2);
                }
            };
        }

        public static Comparator<LiveNode> getComparator(final LiveNodeComparator... multipleOptions) {
            return new Comparator<LiveNode>() {
                public int compare(LiveNode o1, LiveNode o2) {
                    for (LiveNodeComparator option : multipleOptions) {
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

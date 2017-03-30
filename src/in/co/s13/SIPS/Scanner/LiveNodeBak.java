/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package in.co.s13.SIPS.Scanner;

import javafx.beans.property.ReadOnlyDoubleWrapper;
import javafx.beans.property.ReadOnlyStringWrapper;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleLongProperty;
import javafx.beans.property.SimpleStringProperty;

/**
 *
 * @author Sammy Guergachi <sguergachi at gmail.com>
 */
public class LiveNodeBak {

    private final ReadOnlyStringWrapper ipAdress;
    private final SimpleStringProperty operatingSystem;
    private final SimpleStringProperty hostName;
    private final SimpleIntegerProperty processLimit;
    private final SimpleIntegerProperty processWaiting;
    private final SimpleLongProperty totalMem;
    private final SimpleDoubleProperty performance;
    private ReadOnlyDoubleWrapper perfPercentage;
    private final SimpleStringProperty cpuName;
    private final SimpleStringProperty clusterName;
    private final SimpleBooleanProperty isSelected;

    public LiveNodeBak(String ip, String os, String hn, Integer pl, Integer pw, Long tm, Double performace, Double perfPercentage, String cpn, String cln, Boolean selected) {
        this.ipAdress = new ReadOnlyStringWrapper(ip);
        this.operatingSystem = new SimpleStringProperty(os);
        this.hostName = new SimpleStringProperty(hn);
        this.processLimit = new SimpleIntegerProperty(pl);
        this.processWaiting = new SimpleIntegerProperty(pw);
        this.totalMem = new SimpleLongProperty(tm);
        this.performance = new SimpleDoubleProperty(performace);
        this.perfPercentage = new ReadOnlyDoubleWrapper(perfPercentage);
        this.cpuName = new SimpleStringProperty(cpn);
        this.clusterName = new SimpleStringProperty(cln);
        this.isSelected = new SimpleBooleanProperty(selected);
    }

    public ReadOnlyDoubleWrapper perfPercentProperty() {
        if (perfPercentage == null) {
            perfPercentage = new ReadOnlyDoubleWrapper(this, "perfPercentage");
        }
        return perfPercentage;
    }

    public String getIP() {
        return ipAdress.get();
    }

    public void setIP(String ip) {
        ipAdress.set(ip);
    }

    public Boolean getIsSelected() {
        return isSelected.get();
    }

    public void setIsSelected(Boolean fName) {
        isSelected.set(fName);
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

    public double getPerformance() {
        return performance.get();
    }

    public void setPerformance(double fName) {
        performance.set(fName);
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

}

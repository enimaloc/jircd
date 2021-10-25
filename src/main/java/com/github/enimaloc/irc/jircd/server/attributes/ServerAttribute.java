package com.github.enimaloc.irc.jircd.server.attributes;

import java.util.Optional;
import java.util.OptionalInt;

public class ServerAttribute extends Attribute {
    String  caseMapping;
    String  eList;
    String  maxList;
    int     maxTarget;
    int     modes;
    String  network;
    boolean safeList;
    int     silence;
    String  targMax;

    public ServerAttribute(
            String caseMapping, String eList, String maxList, int maxTarget, int modes, String network,
            boolean safeList,
            int silence,
            String targMax
    ) {
        this.caseMapping = caseMapping;
        this.eList       = eList;
        this.maxList     = maxList;
        this.maxTarget   = maxTarget;
        this.modes       = modes;
        this.network     = network;
        this.safeList    = safeList;
        this.silence     = silence;
        this.targMax     = targMax;
    }

    public void caseMapping(String caseMapping) {
        this.caseMapping = caseMapping;
    }

    public String caseMapping() {
        return caseMapping;
    }

    public void eList(String eList) {
        this.eList = eList;
    }

    public String eList() {
        return eList;
    }

    public void maxList(String maxList) {
        this.maxList = maxList;
    }

    public String maxList() {
        return maxList;
    }

    public void maxTarget(int maxTarget) {
        this.maxTarget = maxTarget;
    }

    public OptionalInt maxTarget() {
        return maxTarget < 0 ? OptionalInt.empty() : OptionalInt.of(maxTarget);
    }

    public void modes(int modes) {
        this.modes = modes;
    }

    public OptionalInt modes() {
        return modes < 0 ? OptionalInt.empty() : OptionalInt.of(modes);
    }

    public void network(String network) {
        this.network = network;
    }

    public String network() {
        return network;
    }

    public boolean safeList() {
        return safeList;
    }

    public void safeList(boolean safeList) {
        this.safeList = safeList;
    }

    public OptionalInt silence() {
        return silence < 0 ? OptionalInt.empty() : OptionalInt.of(silence);
    }

    public void silence(int silence) {
        this.silence = silence;
    }

    public void targMax(String targMax) {
        this.targMax = targMax;
    }

    public Optional<String> targMax() {
        return Optional.ofNullable(targMax);
    }

    @Override
    public String toString() {
        return "ServerAttribute{" +
               "caseMapping='" + caseMapping + '\'' +
               ", eList='" + eList + '\'' +
               ", maxList='" + maxList + '\'' +
               ", maxTarget=" + maxTarget +
               ", modes=" + modes +
               ", network='" + network + '\'' +
               ", safeList=" + safeList +
               ", silence=" + silence +
               ", targMax='" + targMax + '\'' +
               '}';
    }
}
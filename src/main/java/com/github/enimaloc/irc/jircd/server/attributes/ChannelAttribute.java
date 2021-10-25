package com.github.enimaloc.irc.jircd.server.attributes;

import java.util.Arrays;
import java.util.Optional;

public class ChannelAttribute extends Attribute {
    String chanLimit;
    char[] chanModes;
    int    channelLen;
    String chanTypes;
    char   excepts;
    String extBan;
    char   invEx;
    int    kickLen;
    int    topicLen;
    String prefix;
    String statusMsg;

    public ChannelAttribute(
            String chanLimit, char[] chanModes, int channelLen, String chanTypes, char excepts, String extBan,
            char invEx,
            int kickLen,
            int topicLen,
            String prefix,
            String statusMsg
    ) {
        this.chanLimit  = chanLimit;
        this.chanModes  = chanModes;
        this.channelLen = channelLen;
        this.chanTypes  = chanTypes;
        this.excepts    = excepts;
        this.extBan     = extBan;
        this.invEx      = invEx;
        this.kickLen    = kickLen;
        this.topicLen   = topicLen;
        this.prefix     = prefix;
        this.statusMsg  = statusMsg;
    }

    public void chanLimit(String chanLimit) {
        this.chanLimit = chanLimit;
    }

    public String chanLimit() {
        return chanLimit;
    }

    public void chanModes(char... chanModes) {
        this.chanModes = chanModes;
    }

    public char[] chanModes() {
        return chanModes;
    }

    public void channelLen(int channelLen) {
        this.channelLen = channelLen;
    }

    public int channelLen() {
        return channelLen;
    }

    public void chanTypes(String chanTypes) {
        this.chanTypes = chanTypes;
    }

    public Optional<String> chanTypes() {
        return Optional.ofNullable(chanTypes);
    }

    public char excepts() {
        return excepts;
    }

    public void excepts(char excepts) {
        this.excepts = excepts;
    }

    public String extBan() {
        return extBan;
    }

    public void extBan(String extBan) {
        this.extBan = extBan;
    }

    public char invEx() {
        return invEx;
    }

    public void invEx(char invEx) {
        this.invEx = invEx;
    }

    public int kickLen() {
        return kickLen;
    }

    public void kickLen(int kickLen) {
        this.kickLen = kickLen;
    }

    public int topicLen() {
        return topicLen;
    }

    public void topicLen(int topicLen) {
        this.topicLen = topicLen;
    }

    public Optional<String> prefix() {
        return Optional.ofNullable(prefix);
    }

    public void prefix(String prefix) {
        this.prefix = prefix;
    }

    public String statusMsg() {
        return statusMsg;
    }

    public void statusMsg(String statusMsg) {
        this.statusMsg = statusMsg;
    }

    @Override
    public String toString() {
        return "ChannelAttribute{" +
               "chanLimit='" + chanLimit + '\'' +
               ", chanModes=" + Arrays.toString(chanModes) +
               ", channelLen=" + channelLen +
               ", chanTypes='" + chanTypes + '\'' +
               ", excepts=" + excepts +
               ", extBan='" + extBan + '\'' +
               ", invEx=" + invEx +
               ", kickLen=" + kickLen +
               ", topicLen=" + topicLen +
               ", prefix='" + prefix + '\'' +
               ", statusMsg='" + statusMsg + '\'' +
               '}';
    }
}
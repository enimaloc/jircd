package com.github.enimaloc.irc.jircd.internal;

import java.lang.reflect.Field;
import java.util.*;
import java.util.function.BiPredicate;

public final class SupportAttribute {
    private int     awayLen;
    private String  caseMapping;
    private String  chanLimit;
    private char[]  chanModes;
    private int     channelLen;
    private String  chanTypes;
    private String  eList;
    private char    excepts;
    private String  extBan;
    private int     hostLen;
    private char    invEx;
    private int     kickLen;
    private String  maxList;
    private int     maxTarget;
    private int     modes;
    private String  network;
    private int     nickLen;
    private String  prefix;
    private boolean safeList;
    private int     silence;
    private String  statusMsg;
    private String  targMax;
    private int     topicLen;
    private int     userLen;

    public SupportAttribute(
            int awayLen,
            String caseMapping,
            String chanLimit,
            char[] chanModes,
            int channelLen,
            String chanTypes,
            String eList,
            char excepts,
            String extBan,
            int hostLen,
            char invEx,
            int kickLen,
            String maxList,
            int maxTarget,
            int modes,
            String network,
            int nickLen,
            String prefix,
            boolean safeList,
            int silence,
            String statusMsg,
            String targMax,
            int topicLen,
            int userLen
    ) {
        this.awayLen     = awayLen;
        this.caseMapping = caseMapping;
        this.chanLimit   = chanLimit;
        this.chanModes   = chanModes;
        this.channelLen  = channelLen;
        this.chanTypes   = chanTypes;
        this.eList       = eList;
        this.excepts     = excepts;
        this.extBan      = extBan;
        this.hostLen     = hostLen;
        this.invEx       = invEx;
        this.kickLen     = kickLen;
        this.maxList     = maxList;
        this.maxTarget   = maxTarget;
        this.modes       = modes;
        this.network     = network;
        this.nickLen     = nickLen;
        this.prefix      = prefix;
        this.safeList    = safeList;
        this.silence     = silence;
        this.statusMsg   = statusMsg;
        this.targMax     = targMax;
        this.topicLen    = topicLen;
        this.userLen     = userLen;
    }

    public List<Map<String, Object>> asMapsWithLimit(int limit) {
        return asMapsWithLimit(limit, null);
    }

    public List<Map<String, Object>> asMapsWithLimit(int limit, BiPredicate<String, Object> filter) {
        List<Map<String, Object>> ret = new ArrayList<>();
        if (asMap(filter).keySet().size() > limit) {
            Map<String, Object> actual = new HashMap<>();
            for (int i = 0; i < asMap(filter).keySet().size(); i++) {
                if (i % limit == 0) {
                    ret.add(actual);
                    actual = new HashMap<>();
                }
                String key = asMap(filter).keySet().toArray(String[]::new)[i];
                actual.put(key, asMap(filter).get(key));
            }
            ret.add(actual);
        } else {
            ret.add(asMap(filter));
        }
        return ret;
    }

    public Map<String, Object> asMap() {
        return asMap(null);
    }

    public Map<String, Object> asMap(BiPredicate<String, Object> filter) {
        Map<String, Object> ret = new HashMap<>();
        for (Field declaredField : this.getClass().getDeclaredFields()) {
            try {
                if (filter != null && filter.test(declaredField.getName(), declaredField.get(this))) {
                    ret.put(declaredField.getName(), declaredField.get(this));
                }
            } catch (IllegalAccessException e) {
                e.printStackTrace();
            }
        }
        return ret;
    }

    public int length() {
        return asMap((s, o) -> o != null).size();
    }

    @Override
    public String toString() {
        return "IsSupportAttribute[" +
               "awayLen=" + awayLen + ", " +
               "caseMapping=" + caseMapping + ", " +
               "chanModes=" + chanModes + ", " +
               "channelLen=" + channelLen + ", " +
               "chanTypes=" + chanTypes + ", " +
               "eList=" + eList + ", " +
               "excepts=" + excepts + ", " +
               "extBan=" + extBan + ", " +
               "hostLen=" + hostLen + ", " +
               "invEx=" + invEx + ", " +
               "kickLen=" + kickLen + ", " +
               "maxList=" + maxList + ", " +
               "maxTarget=" + maxTarget + ", " +
               "modes=" + modes + ", " +
               "network=" + network + ", " +
               "nickLen=" + nickLen + ", " +
               "prefix=" + prefix + ", " +
               "safeList=" + safeList + ", " +
               "silence=" + silence + ", " +
               "statusMsg=" + statusMsg + ", " +
               "targMax=" + targMax + ", " +
               "topicLen=" + topicLen + ", " +
               "userLen=" + userLen + ']';
    }

    public void awayLen(int awayLen) {
        this.awayLen = awayLen;
    }

    public int awayLen() {
        return awayLen;
    }

    public void caseMapping(String caseMapping) {
        this.caseMapping = caseMapping;
    }

    public String caseMapping() {
        return caseMapping;
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

    public void eList(String eList) {
        this.eList = eList;
    }

    public String eList() {
        return eList;
    }

    public void excepts(char excepts) {
        this.excepts = excepts;
    }

    public Optional<Character> excepts() {
        return excepts == '\u0000' ? Optional.empty() : Optional.of(excepts);
    }

    public void extBan(String extBan) {
        this.extBan = extBan;
    }

    public String extBan() {
        return extBan;
    }

    public void hostLen(int hostLen) {
        this.hostLen = hostLen;
    }

    public int hostLen() {
        return hostLen;
    }

    public void invEx(char invEx) {
        this.invEx = invEx;
    }

    public Optional<Character> invEx() {
        return invEx == '\u0000' ? Optional.empty() : Optional.of(invEx);
    }

    public void kickLen(int kickLen) {
        this.kickLen = kickLen;
    }

    public int kickLen() {
        return kickLen;
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

    public void nickLen(int nickLen) {
        this.nickLen = nickLen;
    }

    public int nickLen() {
        return nickLen;
    }

    public void prefix(String prefix) {
        this.prefix = prefix;
    }

    public Optional<String> prefix() {
        return Optional.ofNullable(prefix);
    }

    public void safeList(boolean safeList) {
        this.safeList = safeList;
    }

    public boolean safeList() {
        return safeList;
    }

    public void silence(int silence) {
        this.silence = silence;
    }

    public OptionalInt silence() {
        return silence < 0 ? OptionalInt.empty() : OptionalInt.of(silence);
    }

    public void statusMsg(String statusMsg) {
        this.statusMsg = statusMsg;
    }

    public String statusMsg() {
        return statusMsg;
    }

    public void targMax(String targMax) {
        this.targMax = targMax;
    }

    public Optional<String> targMax() {
        return Optional.ofNullable(targMax);
    }

    public void topicLen(int topicLen) {
        this.topicLen = topicLen;
    }

    public int topicLen() {
        return topicLen;
    }

    public void userLen(int userLen) {
        this.userLen = userLen;
    }

    public int userLen() {
        return userLen;
    }

}

package com.github.enimaloc.irc.jircd.server.attributes;

import java.util.*;
import java.util.function.BiPredicate;

public final class SupportAttribute extends Attribute {
    private final ChannelAttribute channelAttribute;
    private final UserAttribute    userAttribute;
    private final ServerAttribute  serverAttribute;

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
        this(
                new ChannelAttribute(chanLimit, chanModes, channelLen, chanTypes, excepts, extBan, invEx, kickLen,
                                     topicLen, prefix, statusMsg),
                new UserAttribute(awayLen, nickLen, userLen, hostLen),
                new ServerAttribute(caseMapping, eList, maxList, maxTarget, modes, network, safeList, silence, targMax)
        );
    }

    public SupportAttribute(
            ChannelAttribute channelAttribute, UserAttribute userAttribute,
            ServerAttribute serverAttribute
    ) {
        this.channelAttribute = channelAttribute;
        this.userAttribute    = userAttribute;
        this.serverAttribute  = serverAttribute;
    }

    public ChannelAttribute channelAttribute() {
        return channelAttribute;
    }

    public UserAttribute userAttribute() {
        return userAttribute;
    }

    public ServerAttribute serverAttribute() {
        return serverAttribute;
    }

    @Override
    public String toString() {
        return "SupportAttribute{" +
               "channelAttribute=" + channelAttribute +
               ", userAttribute=" + userAttribute +
               ", serverAttribute=" + serverAttribute +
               '}';
    }

    @Override
    public Map<String, Object> asMap(BiPredicate<String, Object> filter) {
        Map<String, Object> map = new HashMap<>();
        map.putAll(serverAttribute.asMap(filter));
        map.putAll(channelAttribute.asMap(filter));
        map.putAll(userAttribute.asMap(filter));
        return map;
    }
}

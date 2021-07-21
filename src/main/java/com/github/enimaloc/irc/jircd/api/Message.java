package com.github.enimaloc.irc.jircd.api;

import java.util.Arrays;

public class Message {

    public static final Message CMD_JOIN = new Message(":%s JOIN %s");

    public static final Message RPL_WELCOME      = new Message(":%s 001 %s :Welcome to the %s Network, %s");
    public static final Message RPL_YOURHOST     = new Message(":%s 002 %s :Your host is %s, running version %s");
    public static final Message RPL_CREATED      = new Message(":%s 003 %s :This server was created %tD %tT");
    public static final Message RPL_MYINFO       = new Message(":%s 004 %s %s %s %s %s");
    public static final Message RPL_ISUPPORT     = new Message(":%s 005 %s %s :are supported by this server");
    public static final Message RPL_NOTOPIC      = new Message(":%s 331 %s %s :No topic is set");
    public static final Message RPL_TOPIC        = new Message(":%s 332 %s %s :%s");
    public static final Message RPL_TOPICWHOTIME = new Message(":%s 333 %s %s %s %s");
    public static final Message RPL_NAMREPLY     = new Message(":%s 353 %s %s %s :%s");
    public static final Message RPL_ENDOFNAMES   = new Message(":%s 366 %s %s :End of /NAMES list");
    public static final Message RPL_MOTD         = new Message(":%s 372 %s :%s");
    public static final Message RPL_MOTDSTART    = new Message(":%s 375 %s :- %s Message of the day - ");
    public static final Message RPL_ENDOFMOTD    = new Message(":%s 376 %s :End of /MOTD command.");
    public static final Message RPL_YOUREOPER    = new Message(":%s 381 %s :You are now an IRC operator");

    public static final Message ERR_NOSUCHCHANNEL     = new Message(":%s 403 %s %s :No such channel");
    public static final Message ERR_TOOMANYCHANNELS   = new Message(":%s 405 %s %s :You have joined too many channels");
    public static final Message ERR_NOMOTD            = new Message(":%s 422 %s :MOTD File is missing");
    public static final Message ERR_ERRONEUSNICKNAME  = new Message(":%s 432 %s %s :Erroneus nickname");
    public static final Message ERR_NICKNAMEINUSE     = new Message(":%s 433 %s %s :Nickname is already in use");
    public static final Message ERR_NOTONCHANNEL      = new Message(":%s 442 %s %s :You're not on that channel");
    public static final Message ERR_NEEDMOREPARAMS    = new Message(":%s 461 %s %s :Not enough parameters");
    public static final Message ERR_ALREADYREGISTERED = new Message(":%s 462 %s :You may not reregister");
    public static final Message ERR_PASSWDMISMATCH    = new Message(":%s 464 %s :Password incorrect");
    public static final Message ERR_CHANNELISFULL     = new Message(":%s 471 %s %s :Cannot join channel (+l)");
    public static final Message ERR_INVITEONLYCHAN    = new Message(":%s 473 %s %s :Cannot join channel (+i)");
    public static final Message ERR_BANNEDFROMCHAN    = new Message(":%s 474 %s %s :Cannot join channel (+b)");
    public static final Message ERR_BADCHANNELKEY     = new Message(":%s 475 %s %s :Cannot join channel (+k)");
    public static final Message ERR_CHANOPRIVSNEEDED  = new Message(":%s 482 %s %s :You're not channel operator");

    private final String   base;
    private final boolean  haveTrailing;
    private       Object[] parameters = new Object[0];
    private       String   trailing;

    public Message(String base) {
        this(base, false);
    }

    Message(String base, boolean haveTrailing) {
        this.base         = base;
        this.haveTrailing = haveTrailing;
    }

    public Object[] parameters() {
        return parameters;
    }

    public Message parameters(Object... parameters) {
        this.parameters = parameters;
        return this;
    }

    public String trailing() {
        return trailing;
    }

    public Message trailing(String trailing) {
        this.trailing = trailing;
        return this;
    }

    public String format(String source) {
        Object[] objects = new Object[parameters.length + 1];
        objects[0] = source;
        System.arraycopy(parameters, 0, objects, 1, parameters.length);
        return base.formatted(objects) + (haveTrailing && trailing != null ? " :" + trailing : "");
    }

    @Override
    public String toString() {
        return "Message{" +
               "base='" + base + '\'' +
               ", parameters=" + Arrays.toString(parameters) +
               ", haveTrailing=" + haveTrailing +
               ", trailing='" + trailing + '\'' +
               '}';
    }
}

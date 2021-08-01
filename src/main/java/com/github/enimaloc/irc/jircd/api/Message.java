package com.github.enimaloc.irc.jircd.api;

import java.util.Arrays;

public class Message {

    public static final Message CMD_JOIN = new Message(":%s JOIN %s");

    public static final Message RPL_WELCOME         = new Message(":%s 001 %s :Welcome to the %s Network, %s");
    public static final Message RPL_YOURHOST        = new Message(":%s 002 %s :Your host is %s, running version %s");
    public static final Message RPL_CREATED         = new Message(":%s 003 %s :This server was created %tD %tT");
    public static final Message RPL_MYINFO          = new Message(":%s 004 %s %s %s %s %s");
    public static final Message RPL_ISUPPORT        = new Message(":%s 005 %s %s :are supported by this server");
    public static final Message RPL_STATSLINKLINE   = new Message(":%s 211 %s %s %s");
    public static final Message RPL_STATSCOMMANDS   = new Message(":%s 212 %s %s");
    public static final Message RPL_STATSCLINE      = new Message(":%s 213 C %s * %s %s %s");
    public static final Message RPL_STATSILINE      = new Message(":%s 215 I %s * %s %s %s");
    public static final Message RPL_STATSKLINE      = new Message(":%s 216 K %s * %s %s %s");
    public static final Message RPL_ENDOFSTATS      = new Message(":%s 219 %s :End of /STATS report");
    public static final Message RPL_UMODEIS         = new Message(":%s 221 %s %s");
    public static final Message RPL_STATSLLINE      = new Message(":%s 241 L %s * %s %s");
    public static final Message RPL_STATSUPTIME     = new Message(":%s 242 :Server Up %d days %d:%02d:%02d");
    public static final Message RPL_STATSOLINE      = new Message(":%s 243 O %s * %s");
    public static final Message RPL_STATSHLINE      = new Message(":%s 244 H %s * %s");
    public static final Message RPL_ADMINME         = new Message(":%s 256 %s %s :Administrative info");
    public static final Message RPL_ADMINLOC1       = new Message(":%s 257 %s :%s");
    public static final Message RPL_ADMINLOC2       = new Message(":%s 258 %s :%s");
    public static final Message RPL_ADMINEMAIL      = new Message(":%s 259 %s :%s");
    public static final Message RPL_AWAY            = new Message(":%s 301 %s %s :%s");
    public static final Message RPL_USERHOST        = new Message(":%s 302 %s :%s");
    public static final Message RPL_LISTSTART       = new Message(":%s 321 %s Channel :Users  Name");
    public static final Message RPL_LIST            = new Message(":%s 322 %s %s %s :%s");
    public static final Message RPL_LISTEND         = new Message(":%s 323 %s :End of /LIST");
    public static final Message RPL_CHANNELMODEIS   = new Message(":%s 324 %s %s %s %s");
    public static final Message RPL_CREATIONTIME    = new Message(":%s 329 %s %s %s");
    public static final Message RPL_NOTOPIC         = new Message(":%s 331 %s %s :No topic is set");
    public static final Message RPL_TOPIC           = new Message(":%s 332 %s %s :%s");
    public static final Message RPL_TOPICWHOTIME    = new Message(":%s 333 %s %s %s %s");
    public static final Message RPL_INVITELIST      = new Message(":%s 346 %s %s %s");
    public static final Message RPL_ENDOFINVITELIST = new Message(":%s 347 %s %s :End of channel invite list");
    public static final Message RPL_EXCEPTLIST      = new Message(":%s 348 %s %s %s");
    public static final Message RPL_ENDOFEXCEPTLIST = new Message(":%s 349 %s %s :End of channel exception list");
    public static final Message RPL_VERSION         = new Message(":%s 351 %s %s %s :%s");
    public static final Message RPL_NAMREPLY        = new Message(":%s 353 %s %s %s :%s");
    public static final Message RPL_ENDOFNAMES      = new Message(":%s 366 %s %s :End of /NAMES list");
    public static final Message RPL_BANLIST         = new Message(":%s 367 %s %s %s");
    public static final Message RPL_ENDOFBANLIST    = new Message(":%s 368 %s %s :End of channel ban list");
    public static final Message RPL_INFO            = new Message(":%s 371 :%s");
    public static final Message RPL_ENDOFINFO       = new Message(":%s 374 :End of /INFO list");
    public static final Message RPL_MOTD            = new Message(":%s 372 %s :%s");
    public static final Message RPL_MOTDSTART       = new Message(":%s 375 %s :- %s Message of the day - ");
    public static final Message RPL_ENDOFMOTD       = new Message(":%s 376 %s :End of /MOTD command.");
    public static final Message RPL_YOUREOPER       = new Message(":%s 381 %s :You are now an IRC operator");
    public static final Message RPL_TIME            = new Message(":%s 391 %s :%s");

    public static final Message ERR_UNKNOWNERROR      = new Message(":%s 400 %s %s :%s");
    public static final Message ERR_NOSUCHNICK        = new Message(":%s 401 %s %s :No such nick/channel");
    public static final Message ERR_NOSUCHSERVER      = new Message(":%s 402 %s %s :No such server");
    public static final Message ERR_NOSUCHCHANNEL     = new Message(":%s 403 %s %s :No such channel");
    public static final Message ERR_CANNOTSENDTOCHAN  = new Message(":%s 404 %s %s :Cannot send to channel");
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
    public static final Message ERR_UMODEUNKNOWNFLAG  = new Message(":%s 501 %s :Unknown MODE flag");
    public static final Message ERR_USERSDONTMATCH    = new Message(":%s 502 %s :Cant change mode for other users");

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

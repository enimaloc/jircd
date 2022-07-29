package fr.enimaloc.jircd.message;

import fr.enimaloc.jircd.user.UserInfo;
import fr.enimaloc.enutils.classes.PatternEngine;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.function.BiConsumer;
import java.util.regex.Pattern;

public class Message {

    public static final Message CMD_JOIN = new Message(":<source> JOIN %s");
    public static final Message CMD_AWAY    = new Message(":<source> AWAY");
    public static final Message CMD_WALLOPS = new Message(":<source> WALLOPS :%s");

    public static final Message RPL_WELCOME         =
            new Message(":<source> 001 <client> :Welcome to the <network> Network, <nick>");
    public static final Message RPL_YOURHOST        =
            new Message(":<source> 002 <client> :Your host is <servername>, running version <version>");
    public static final Message RPL_CREATED         =
            new Message(":<source> 003 <client> :This server was created <datetime>");
    public static final Message RPL_MYINFO          =
            new Message(":<source> 004 <client> <servername> <version> <available user modes>" +
                        " <available channel modes>");
    public static final Message RPL_ISUPPORT        =
            new Message(":<source> 005 <client> <tokens> :are supported by this server");
    public static final Message RPL_STATSLINKLINE   =
            new Message(":<source> 211 <linkname> <sendq> <sent messages> <sent Kbytes>" +
                        " <received messages> <received Kbytes> <time open>");
    public static final Message RPL_STATSCOMMANDS   =
            new Message(":<source> 212 <command> <count>");// <byte count> <remote count>");
    public static final Message RPL_STATSCLINE      =
            new Message(":<source> 213 C <host> * <name> <port> <class>");
    public static final Message RPL_STATSILINE      =
            new Message(":<source> 215 I <host> * <host> <port> <class>");
    public static final Message RPL_STATSKLINE      =
            new Message(":<source> 216 K <host> * <username> <port> <class>");
    public static final Message RPL_ENDOFSTATS      =
            new Message(":<source> 219 <stats letter> :End of /STATS report");
    public static final Message RPL_UMODEIS         =
            new Message(":<source> 221 <client> <user modes>");
    public static final Message RPL_STATSLLINE      =
            new Message(":<source> 241 L <hostmask> * <servername> <maxdepth>");
    public static final Message RPL_STATSUPTIME     =
            new Message(":<source> 242 :Server Up %d days %d:%02d:%02d");
    public static final Message RPL_STATSOLINE      =
            new Message(":<source> 243 O <hostmask> * <name>");
    public static final Message RPL_STATSHLINE      =
            new Message(":<source> 244 H <hostmask> * <servername>");
    public static final Message RPL_LUSERCLIENT     =
            new Message(":<source> 251 <client> :There are <u> users and <i> invisibles on <s> servers");
    public static final Message RPL_LUSEROP         =
            new Message(":<source> 252 <client> <ops> :operator(s) online");
    public static final Message RPL_LUSERUNKNOWN    =
            new Message(":<source> 253 <client> <connections> :unknown connection(s)");
    public static final Message RPL_LUSERCHANNELS   =
            new Message(":<source> 254 <client> <channels> :channels formed");
    public static final Message RPL_LUSERME         =
            new Message(":<source> 255 <client> :I have <u> clients and <s> servers");
    public static final Message RPL_ADMINME         =
            new Message(":<source> 256 <client> <server> :Administrative info");
    public static final Message RPL_ADMINLOC1       =
            new Message(":<source> 257 <client> :<info>");
    public static final Message RPL_ADMINLOC2       =
            new Message(":<source> 258 <client> :<info>");
    public static final Message RPL_ADMINEMAIL      =
            new Message(":<source> 259 <client> :<info>");
    public static final Message RPL_WHOISCERTFP     =
            new Message(":<source> 276 <client> <nick> :has client certificate fingerprint <fingerprint>");
    public static final Message RPL_AWAY            =
            new Message(":<source> 301 <client> <nick> :<message>");
    public static final Message RPL_USERHOST        =
            new Message(":<source> 302 <client> :<reply>");
    public static final Message RPL_UNAWAY          =
            new Message(":<source> 305 <client> :You are no longer marked as being away");
    public static final Message RPL_NOWAWAY         =
            new Message(":<source> 306 <client> :You have been marked as being away");
    public static final Message RPL_WHOISREGNICK    =
            new Message(":<source> 307 <client> <nick> :is a registered nick");
    public static final Message RPL_WHOISUSER       =
            new Message(":<source> 311 <client> <nick> <username> <host> * :<realname>");
    public static final Message RPL_WHOISSERVER     =
            new Message(":<source> 312 <client> <nick> <server> :<serverinfo>");
    public static final Message RPL_WHOISOPERATOR   =
            new Message(":<source> 313 <client> <nick> :is an IRC operator");
    public static final Message RPL_ENDOFWHO        =
            new Message(":<source> 315 <client> <mask> :End of /WHO list");
    public static final Message RPL_WHOISIDLE       =
            new Message(":<source> 317 <client> <nick> <secs> <signon> :seconds idle, signon time");
    public static final Message RPL_ENDOFWHOIS       =
            new Message(":<source> 318 <client> <nick> :End of /WHOIS list");
    public static final Message RPL_WHOISCHANNELS   =
            new Message(":<source> 319 <client> <nick> :<channels>");
    public static final Message RPL_WHOISSPECIAL    =
            new Message(":<source> 320 <client> <nick> :<special>");
    public static final Message RPL_LISTSTART       =
            new Message(":<source> 321 <client> Channel :Users  Name");
    public static final Message RPL_LIST            =
            new Message(":<source> 322 <client> <channel> <client count> :<topic>");
    public static final Message RPL_LISTEND         =
            new Message(":<source> 323 <client> :End of /LIST");
    public static final Message RPL_CHANNELMODEIS   =
            new Message(":<source> 324 <client> <channel> <modestring> <mode arguments>");
    public static final Message RPL_CREATIONTIME    =
            new Message(":<source> 329 <client> <channel> <creationtime>");
    public static final Message RPL_WHOISACCOUNT    =
            new Message(":<source> 330 <client> <nick> <account> :is logged in as");
    public static final Message RPL_NOTOPIC         =
            new Message(":<source> 331 <client> <channel> :No topic is set");
    public static final Message RPL_TOPIC           =
            new Message(":<source> 332 <client> <channel> :<topic>");
    public static final Message RPL_TOPICWHOTIME    =
            new Message(":<source> 333 <client> <channel> <nick> <setat>");
    // TODO: 20/07/2022 - Add correctly RPL_WHOISACTUALLY, multiples answers ?
    @Deprecated // see TODO above
    public static final Message RPL_WHOISACTUALLY   =
            new Message(":<source> 338 <client> <nick> <actually>");
    public static final Message RPL_INVITELIST      =
            new Message(":<source> 346 <client> <channel> <mask>");
    public static final Message RPL_ENDOFINVITELIST =
            new Message(":<source> 347 <client> <channel> :End of channel invite list");
    public static final Message RPL_EXCEPTLIST      =
            new Message(":<source> 348 <client> <channel> <mask>");
    public static final Message RPL_ENDOFEXCEPTLIST =
            new Message(":<source> 349 <client> <channel> :End of channel exception list");
    public static final Message RPL_VERSION         =
            new Message(":<source> 351 <client> <version> <server> :<comments>");
    public static final Message RPL_WHOREPLY        =
            new Message(":<source> 352 <client> <channel> <username> <host> <server> <nick> <flags> :<hopcount> <realname>");
    public static final Message RPL_NAMREPLY        =
            new Message(":<source> 353 <client> <symbol> <channel> :<nicknames>");
    public static final Message RPL_LINKS           =
            new Message(":<source> 364 <client> <mask> <server> :<hopcount> <serverinfo>");
    public static final Message RPL_ENDOFLINKS      =
            new Message(":<source> 365 <client> <mask> :End of /LINKS list");
    public static final Message RPL_ENDOFNAMES      =
            new Message(":<source> 366 <client> <channel> :End of /NAMES list");
    public static final Message RPL_BANLIST         =
            new Message(":<source> 367 <client> <channel> <mask>");
    public static final Message RPL_ENDOFBANLIST    =
            new Message(":<source> 368 <client> <channel> :End of channel ban list");
    public static final Message RPL_ENDOFWHOWAS     =
            new Message(":<source> 369 <client> <nick> :End of WHOWAS");
    public static final Message RPL_INFO            =
            new Message(":<source> 371 :<string>");
    public static final Message RPL_ENDOFINFO       =
            new Message(":<source> 374 :End of /INFO list");
    public static final Message RPL_MOTD            =
            new Message(":<source> 372 <client> :<line of the motd>");
    public static final Message RPL_MOTDSTART       =
            new Message(":<source> 375 <client> :- <server> Message of the day - ");
    public static final Message RPL_ENDOFMOTD       =
            new Message(":<source> 376 <client> :End of /MOTD command.");
    // Warning: RPL_WHOISHOST is unclear, view original source for more information (Unreal as described by alien.net.au)
    public static final Message RPL_WHOISHOST       =
            new Message(":<source> 378 <client> <nick> :is connecting from <host>");
    public static final Message RPL_WHOISMODES      =
            new Message(":<source> 379 <client> <nick> :is using modes <modes>");
    public static final Message RPL_YOUREOPER       =
            new Message(":<source> 381 <client> :You are now an IRC operator");
    public static final Message RPL_REHASHING       =
            new Message(":<source> 382 <client> <file> :Rehashing");
    public static final Message RPL_TIME            =
            new Message(":<source> 391 <server> :<string showing server's local time>");

    public static final Message ERR_UNKNOWNERROR      =
            new Message(":<source> 400 <client> <command> :<info>");
    public static final Message ERR_NOSUCHNICK        =
            new Message(":<source> 401 <client> <nickname> :No such nick/channel");
    public static final Message ERR_NOSUCHSERVER      =
            new Message(":<source> 402 <client> <server name> :No such server");
    public static final Message ERR_NOSUCHCHANNEL     =
            new Message(":<source> 403 <client> <channel> :No such channel");
    public static final Message ERR_CANNOTSENDTOCHAN  =
            new Message(":<source> 404 <client> <channel> :Cannot send to channel");
    public static final Message ERR_TOOMANYCHANNELS   =
            new Message(":<source> 405 <client> <channel> :You have joined too many channels");
    public static final Message ERR_WASNOSUCHNICK     =
            new Message(":<source> 406 <client> :There was no such nickname");
    public static final Message ERR_NOMOTD            =
            new Message(":<source> 422 <client> :MOTD File is missing");
    public static final Message ERR_ERRONEUSNICKNAME  =
            new Message(":<source> 432 <client> <nick> :Erroneus nickname");
    public static final Message ERR_NICKNAMEINUSE     =
            new Message(":<source> 433 <client> <nick> :Nickname is already in use");
    public static final Message ERR_USERNOTINCHANNEL  =
            new Message(":<source> 441 <client> <nick> <channel> :They aren't on that channel");
    public static final Message ERR_NOTONCHANNEL      =
            new Message(":<source> 442 <client> <channel> :You're not on that channel");
    public static final Message ERR_NEEDMOREPARAMS    =
            new Message(":<source> 461 <client> <command> :Not enough parameters");
    public static final Message ERR_ALREADYREGISTERED =
            new Message(":<source> 462 <client> :You may not reregister");
    public static final Message ERR_PASSWDMISMATCH    =
            new Message(":<source> 464 <client> :Password incorrect");
    public static final Message ERR_CHANNELISFULL     =
            new Message(":<source> 471 <client> <channel> :Cannot join channel (+l)");
    public static final Message ERR_INVITEONLYCHAN    =
            new Message(":<source> 473 <client> <channel> :Cannot join channel (+i)");
    public static final Message ERR_BANNEDFROMCHAN    =
            new Message(":<source> 474 <client> <channel> :Cannot join channel (+b)");
    public static final Message ERR_BADCHANNELKEY     =
            new Message(":<source> 475 <client> <channel> :Cannot join channel (+k)");
    public static final Message ERR_NOPRIVILEGES      =
            new Message(":<source> 481 <client> :Permission Denied- You're not an IRC operator");
    public static final Message ERR_CHANOPRIVSNEEDED  =
            new Message(":<source> 482 <client> <channel> :You're not channel operator");
    public static final Message ERR_NOOPERHOST        =
            new Message(":<source> 491 <client> :No O-lines for your host");
    public static final Message ERR_UMODEUNKNOWNFLAG  =
            new Message(":<source> 501 <client> :Unknown MODE flag");
    public static final Message ERR_USERSDONTMATCH =
            new Message(":<source> 502 <client> :Cant change mode for other users");
    public static final Message ERR_HELPNOTFOUND   =
            new Message(":<source> 524 <client> <subject> :No help available on this topic");
    public static final Message RPL_WHOISSECURE =
            new Message(":<source> 671 <client> <nick> :is using a secure connection");

    private final String                            base;
    private final boolean                           haveTrailing;
    private       UserInfo                          client;
    private       Map<String, String>               format       = new HashMap<>();
    private       Object[]                          rawFormat    = new Object[0];
    private       String                            trailing;
    private       BiConsumer<String, StringBuilder> unknownToken = PatternEngine.DEFAULT_UNKNOWN_TOKEN;

    public Message(String base) {
        this(base, false);
    }

    Message(String base, boolean haveTrailing) {
        this.base         = base;
        this.haveTrailing = haveTrailing;
    }

    public Message client(UserInfo client) {
        this.client = client;
        return this;
    }

    public Message rawFormat(Object... rawFormat) {
        this.rawFormat = rawFormat;
        return this;
    }

    public Message format(Map<String, String> format) {
        this.format = format;
        return this;
    }

    public Message addFormat(String key, Object value) {
        this.format.put(key, value.toString());
        return this;
    }

    public Message trailing(String trailing) {
        this.trailing = trailing;
        return this;
    }

    public Message unknownToken(BiConsumer<String, StringBuilder> unknownToken) {
        this.unknownToken = unknownToken;
        return this;
    }

    public String format(String source) {
        addFormat("source", source);
        if (client != null) {
            addFormat("client", client.format());
        }
        return (PatternEngine.replaceTokens(Pattern.compile("<(.+?)>"), unknownToken, base.formatted(rawFormat), format) +
               (haveTrailing && trailing != null ? " :" + trailing : ""));
    }

    @Override
    public String toString() {
        return "Message{" +
               "base='" + base + '\'' +
               ", haveTrailing=" + haveTrailing +
               ", format=" + format +
               ", rawFormat=" + Arrays.toString(rawFormat) +
               ", trailing='" + trailing + '\'' +
               '}';
    }
}

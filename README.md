# JIRCD

JIRCD is a Java [Internet Relay Chat](https://wikipedia.org/wiki/Internet_Relay_Chat) Deamon which can be used as Internet Relay Chat server,
this project follows [Oracle Code Convention](https://www.oracle.com/java/technologies/javase/codeconventions-contents.html), 
and we follow the [Internet Relay Chat documentation](https://modern.ircdocs.horse/index.html) 
and for undocumented part needed we follow [RFC 2812](https://datatracker.ietf.org/doc/html/rfc2812).

## Supported commands

- [Channel](https://modern.ircdocs.horse/index.html#channel-operations) (ed1be41f01481f007ba83e352e58f1af84a13642)
  - [Join](https://modern.ircdocs.horse/index.html#join-message)
  - [List](https://modern.ircdocs.horse/index.html#list-message)
  - [Names](https://modern.ircdocs.horse/index.html#names-message)
  - [Part](https://modern.ircdocs.horse/index.html#part-message)
  - [Topic](https://modern.ircdocs.horse/index.html#topic-message)
- [Connection](https://modern.ircdocs.horse/index.html#connection-messages) (d37e51341e1e9193b7c7343c3c19ce9f44c023bb)
  - [Nick](https://modern.ircdocs.horse/index.html#nick-message)
  - [Oper](https://modern.ircdocs.horse/index.html#oper-message)
  - [Quit](https://modern.ircdocs.horse/index.html#quit-message)
  - [User](https://modern.ircdocs.horse/index.html#user-message)
- [Sending Messages](https://modern.ircdocs.horse/index.html#sending-messages) (feafd0a467cff85bfa04dd41342a6b66b5a666e6)
  - [Notice](https://modern.ircdocs.horse/index.html#notice-message)
  - [Privmsg](https://modern.ircdocs.horse/index.html#privmsg-message)
- [Operator](https://modern.ircdocs.horse/index.html#operator-messages) (c58bb0c1fde1d16c240a81fb25cb27f27f50c8e9)
  - [Kill](https://modern.ircdocs.horse/index.html#kill-message)
- [Optional](https://modern.ircdocs.horse/index.html#optional-messages) (25a0fc4de3c9cc08133e9d5943cdaf47de2be5b6)
  - [Userhost](https://modern.ircdocs.horse/index.html#userhost-message)
- [Server Queries and Commands](https://modern.ircdocs.horse/index.html#server-queries-and-commands) (363648516b2323af0b3b952f3f558612328deb2a)
  - [Admin](https://modern.ircdocs.horse/index.html#admin-message)
  - [Connect](https://modern.ircdocs.horse/index.html#connect-message)
  - [Info](https://modern.ircdocs.horse/index.html#info-message)
  - [Mode](https://modern.ircdocs.horse/index.html#mode-message)
  - [Motd](https://modern.ircdocs.horse/index.html#motd-message)
  - [Stats](https://modern.ircdocs.horse/index.html#stats-message)
  - [Time](https://modern.ircdocs.horse/index.html#time-message)
  - [Version](https://modern.ircdocs.horse/index.html#version-message)
- Undocumented ([RFC 2812](https://datatracker.ietf.org/doc/html/rfc2812))
  - [Miscellaneous](https://datatracker.ietf.org/doc/html/rfc2812#section-3.7) (c58bb0c1fde1d16c240a81fb25cb27f27f50c8e9)
    - [Ping](https://datatracker.ietf.org/doc/html/rfc2812#section-3.7.2)
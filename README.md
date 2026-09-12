# JIRCD

JIRCD is a Java [Internet Relay Chat](https://wikipedia.org/wiki/Internet_Relay_Chat) Deamon which can be used as Internet Relay Chat server,
this project follows [Oracle Code Convention](https://www.oracle.com/java/technologies/javase/codeconventions-contents.html), 
and we follow the [Internet Relay Chat documentation](https://modern.ircdocs.horse/index.html) 
and for undocumented part needed we follow [RFC 2812](https://datatracker.ietf.org/doc/html/rfc2812).
For commit we follow [Conventional Commit](https://www.conventionalcommits.org/en/v1.0.0/).
All pull requests which do not respect the conventions above will be considered as [invalid](https://github.com/enimaloc/jircd/issues?q=label%3Ainvalid).

## Supported commands

- [Channel](https://modern.ircdocs.horse/index.html#channel-operations) ([unimplemented]())
  - [Join](https://modern.ircdocs.horse/index.html#join-message)
  - [List](https://modern.ircdocs.horse/index.html#list-message)
  - [Names](https://modern.ircdocs.horse/index.html#names-message)
  - [Part](https://modern.ircdocs.horse/index.html#part-message)
  - [Topic](https://modern.ircdocs.horse/index.html#topic-message)
- [Connection](https://modern.ircdocs.horse/index.html#connection-messages) ([unimplemented]())
  - [Nick](https://modern.ircdocs.horse/index.html#nick-message)
  - [Oper](https://modern.ircdocs.horse/index.html#oper-message)
  - [Quit](https://modern.ircdocs.horse/index.html#quit-message)
  - [User](https://modern.ircdocs.horse/index.html#user-message)
- [Sending Messages](https://modern.ircdocs.horse/index.html#sending-messages) ([unimplemented]())
  - [Notice](https://modern.ircdocs.horse/index.html#notice-message)
  - [Privmsg](https://modern.ircdocs.horse/index.html#privmsg-message)
- [Operator](https://modern.ircdocs.horse/index.html#operator-messages) ([unimplemented]())
  - [Kill](https://modern.ircdocs.horse/index.html#kill-message)
- [Optional](https://modern.ircdocs.horse/index.html#optional-messages) ([unimplemented]())
  - [Userhost](https://modern.ircdocs.horse/index.html#userhost-message)
- [Server Queries and Commands](https://modern.ircdocs.horse/index.html#server-queries-and-commands) ([unimplemented]())
  - [Admin](https://modern.ircdocs.horse/index.html#admin-message)
  - [Connect](https://modern.ircdocs.horse/index.html#connect-message)
  - [Info](https://modern.ircdocs.horse/index.html#info-message)
  - [Mode](https://modern.ircdocs.horse/index.html#mode-message)
  - [Motd](https://modern.ircdocs.horse/index.html#motd-message)
  - [Stats](https://modern.ircdocs.horse/index.html#stats-message)
  - [Time](https://modern.ircdocs.horse/index.html#time-message)
  - [Version](https://modern.ircdocs.horse/index.html#version-message)
- Undocumented ([RFC 2812](https://datatracker.ietf.org/doc/html/rfc2812))
  - [Miscellaneous](https://datatracker.ietf.org/doc/html/rfc2812#section-3.7) ([unimplemented]())
    - [Ping](https://datatracker.ietf.org/doc/html/rfc2812#section-3.7.2)

## License

The source code for the site is licensed under the MIT license, which you can find in
the [LICENSE](LICENCE) file.
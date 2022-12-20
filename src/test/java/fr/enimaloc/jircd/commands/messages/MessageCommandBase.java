/*
 * MessageCommandBase
 *
 * 0.0.1
 *
 * 06/08/2022
 */
package fr.enimaloc.jircd.commands.messages;

import fr.enimaloc.jircd.SocketBase;

/**
 *
 */
public class MessageCommandBase extends SocketBase {
    @Override
    protected void init() {
        super.init();
        connections[0].createUser("bob", "bobby", "Mobbye Plav");
        addConnections(1);
        connections[1].createUser("john", "John Doe");
    }
}

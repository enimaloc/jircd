/*
 * ServerCommandBase
 *
 * 0.0.1
 *
 * 06/08/2022
 */
package fr.enimaloc.jircd.commands.server;

import fr.enimaloc.jircd.SocketBase;

/**
 *
 */
public class ServerCommandBase extends SocketBase {

    @Override
    protected void init() {
        super.init();
        connections[0].createUser("bob", "bobby", "Mobbye Plav");
    }
}

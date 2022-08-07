/*
 * CommandChanneltest
 *
 * 0.0.1
 *
 * 05/08/2022
 */
package fr.enimaloc.jircd.commands.channel;

import fr.enimaloc.jircd.SocketBase;

/**
 *
 */
public class CommandChannelBase extends SocketBase {
    @Override
    protected void init() {
        super.init();
        connections[0].createUser("bob", "bobby", "Mobbye Plav");
    }
}

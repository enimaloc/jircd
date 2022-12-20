package fr.enimaloc.jircd.channel;

import java.util.ArrayList;
import java.util.Optional;
import java.util.OptionalInt;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.api.Assumptions.assumeFalse;

class ChannelModesTest {

    @Test
    void password() {
        ChannelModes channelModes = new ChannelModes().password("password");
        assertEquals(Optional.of("password"), channelModes.password());
        assertTrue(channelModes.password().isPresent());
        assertEquals("k", channelModes.modesString());
        assertEquals("password", channelModes.modesArguments());
    }

    @Test
    void limit() {
        ChannelModes channelModes = new ChannelModes().limit(1);
        assertEquals(OptionalInt.of(1), channelModes.limit());
        assertTrue(channelModes.limit().isPresent());
        assertEquals("l", channelModes.modesString());
        assertEquals("1", channelModes.modesArguments());
    }

    @Test
    void inviteOnly() {
        ChannelModes channelModes = new ChannelModes().inviteOnly(true);
        assertTrue(channelModes.inviteOnly());
        assertEquals("i", channelModes.modesString());
        assertEquals("", channelModes.modesArguments());
    }

    @Test
    void secret() {
        ChannelModes channelModes = new ChannelModes().secret(true);
        assertTrue(channelModes.secret());
        assertEquals("s", channelModes.modesString());
        assertEquals("", channelModes.modesArguments());
    }

    @Test
    void bans() {
        ChannelModes channelModes = new ChannelModes();
        channelModes.bans().add("b!an@ed");
        assertEquals("b", channelModes.modesString());
        assertEquals("", channelModes.modesArguments());
    }

    @Test
    void moderate() {
        ChannelModes channelModes = new ChannelModes().moderate(true);
        assertTrue(channelModes.moderate());
        assertEquals("m", channelModes.modesString());
        assertEquals("", channelModes.modesArguments());
    }

    @Test
    void protected0() {
        ChannelModes channelModes = new ChannelModes().protected0(true);
        assertTrue(channelModes.protected0());
        assertEquals("t", channelModes.modesString());
        assertEquals("", channelModes.modesArguments());
    }

    @Test
    void noExternalMessage() {
        ChannelModes channelModes = new ChannelModes().noExternalMessage(true);
        assertTrue(channelModes.noExternalMessage());
        assertEquals("n", channelModes.modesString());
        assertEquals("", channelModes.modesArguments());
    }

    @Test
    void except() {
        ChannelModes channelModes = new ChannelModes();
        channelModes.except().add("e!xce@pt");
        assertEquals("e", channelModes.modesString());
        assertEquals("", channelModes.modesArguments());
    }

    @Test
    void invEx() {
        ChannelModes channelModes = new ChannelModes();
        channelModes.invEx().add("e!xem@pted");
        assertEquals("I", channelModes.modesString());
        assertEquals("", channelModes.modesArguments());
    }
}
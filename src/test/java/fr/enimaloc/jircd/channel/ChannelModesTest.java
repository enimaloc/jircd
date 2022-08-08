package fr.enimaloc.jircd.channel;

import java.util.ArrayList;
import java.util.Optional;
import java.util.OptionalInt;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class ChannelModesTest {

    @Test
    void password() {
        ChannelModes channelModes = new ChannelModes(
                null, null, null, "password", 0, false, false, false, false, false
        );
        assertEquals(Optional.of("password"), channelModes.password());
        assertTrue(channelModes.password().isPresent());
        assertEquals("k", channelModes.modesString());
        assertEquals("password", channelModes.modesArguments());
    }

    @Test
    void limit() {
        ChannelModes channelModes = new ChannelModes(
                null, null, null, null, 1, false, false, false, false, false
        );
        assertEquals(OptionalInt.of(1), channelModes.limit());
        assertTrue(channelModes.limit().isPresent());
        assertEquals("l", channelModes.modesString());
        assertEquals("1", channelModes.modesArguments());
    }

    @Test
    void inviteOnly() {
        ChannelModes channelModes = new ChannelModes(
                null, null, null, null, 0, false, true, false, false, false
        );
        assertTrue(channelModes.inviteOnly());
        assertEquals("i", channelModes.modesString());
        assertEquals("", channelModes.modesArguments());
    }

    @Test
    void secret() {
        ChannelModes channelModes = new ChannelModes(
                null, null, null, null, 0, false, false, true, false, false
        );
        assertTrue(channelModes.secret());
        assertEquals("s", channelModes.modesString());
        assertEquals("", channelModes.modesArguments());
    }

    @Test
    void bans() {
        ChannelModes channelModes = new ChannelModes(
                null,
                new ArrayList<>() {{add("b!an@ed");}},
                null,
                null,
                0,
                false,
                false,
                false,
                false,
                false
        );
        assertFalse(channelModes.bans().isEmpty());
        assertEquals("b", channelModes.modesString());
        assertEquals("", channelModes.modesArguments());
    }

    @Test
    void moderate() {
        ChannelModes channelModes = new ChannelModes(
                null, null, null, null, 0, true, false, false, false, false
        );
        assertTrue(channelModes.moderate());
        assertEquals("m", channelModes.modesString());
        assertEquals("", channelModes.modesArguments());
    }

    @Test
    void _protected() {
        ChannelModes channelModes = new ChannelModes(
                null, null, null, null, 0, false, false, false, true, false
        );
        assertTrue(channelModes._protected());
        assertEquals("t", channelModes.modesString());
        assertEquals("", channelModes.modesArguments());
    }

    @Test
    void noExternalMessage() {
        ChannelModes channelModes = new ChannelModes(
                null, null, null, null, 0, false, false, false, false, true
        );
        assertTrue(channelModes.noExternalMessage());
        assertEquals("n", channelModes.modesString());
        assertEquals("", channelModes.modesArguments());
    }

    @Test
    void except() {
        ChannelModes channelModes = new ChannelModes(
                new ArrayList<>() {{add("e!xce@pt");}},
                null,
                null,
                null,
                0,
                false,
                false,
                false,
                false,
                false
        );
        assertFalse(channelModes.except().isEmpty());
        assertEquals("e", channelModes.modesString());
        assertEquals("", channelModes.modesArguments());
    }

    @Test
    void invEx() {
        ChannelModes channelModes = new ChannelModes(
                null,
                null,
                new ArrayList<>() {{add("e!xem@pted");}},
                null,
                0,
                false,
                false,
                false,
                false,
                false
        );
        assertFalse(channelModes.invEx().isEmpty());
        assertEquals("I", channelModes.modesString());
        assertEquals("", channelModes.modesArguments());
    }
}
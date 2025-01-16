package io.github.hellojonas.tcp;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.charset.StandardCharsets;
import java.util.Collections;

import org.junit.jupiter.api.Disabled;

public class TCPMessageTest {

    @Disabled
    public void TestMarshalUnmarshal() {
        String expected = "A message to be sent! not in a bottle though áéíóú-àèìòù-âêô-ãõ-ç :)";
        expected = String.join("\n", Collections.nCopies(12, expected));

        TCPMessage toSend = TCPMessage.builder()
                .version((byte) 1)
                .flags((byte) 8)
                .data(expected.getBytes(StandardCharsets.UTF_8))
                .build();

        byte[] marshalled = TCPMessage.MarshalBinary(toSend);

        assertNotNull(marshalled);
        assertTrue(marshalled.length > 0);

        TCPMessage msg = TCPMessage.UnmarshalBinary(marshalled);

        assertEquals(msg.getVersion(), toSend.getVersion());
        assertEquals(msg.getFlags(), toSend.getFlags());
        assertEquals(msg.getData().length, toSend.getData().length);

        String actual = new String(msg.getData(), StandardCharsets.UTF_8);
        assertEquals(expected, actual);
    }
}

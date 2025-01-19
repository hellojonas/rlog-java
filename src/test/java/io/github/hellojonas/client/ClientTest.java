package io.github.hellojonas.client;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

import org.junit.jupiter.api.Disabled;

public class ClientTest {

	@Disabled
	public void testClientAuth() throws IOException {
		Client.Credential cred = new Client.Credential();
		cred.setAppId("");
		cred.setSecret("");

		Client client = new Client("127.0.0.1", 8008, cred);

		for (int i = 0; i < 10; i++) {
			String msg = String.format("Client message entry %d\n", i + 1);
			client.getConn().Send(msg.getBytes(StandardCharsets.UTF_8));
		}
	}
}

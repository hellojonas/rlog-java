package io.github.hellojonas.rlog.client;

import java.io.IOException;
import java.io.InputStream;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import com.google.gson.Gson;

import io.github.hellojonas.rlog.tcp.TCPConnection;
import io.github.hellojonas.rlog.tcp.TCPMessage;


public class Client {
	public final static String AUTH_OK = "AUTH_OK";
	public final static String AUTH_REQUEST = "AUTH_REQUEST";
	public final static int AUTH_MESSAGE_TIMEOUT = 5000;

	final private String addr;
	final private int port;
	private Socket socket;
	final private Credential credential;
	private final Lock locker;
	private TCPConnection conn;

	public Client(String addr, int port, Credential cred) throws IOException {
		this.addr = addr;
		this.port = port;
		this.socket = new Socket();
		this.credential = cred;
		this.conn = new TCPConnection(this.socket);
		this.locker = new ReentrantLock();

		this.socket.connect(new InetSocketAddress(addr, port));

		if (!authenticate()) {
			throw new RuntimeException("client authentication failed, check your app credentials");
		}
	}

	public void reconnect() throws IOException {
		if (!this.socket.isClosed()) {
			return;
		}

		this.socket.connect(new InetSocketAddress(this.addr, this.port));

		if (!authenticate()) {
			throw new RuntimeException("client authentication failed, check your app credentials");
		}
	}

	private boolean authenticate() throws IOException {
		byte[] chunk = new byte[TCPMessage.MESSAGE_MAX_LENGTH];
		InputStream input = socket.getInputStream();

		try {
			locker.lock();
			socket.setSoTimeout(AUTH_MESSAGE_TIMEOUT);
			input.read(chunk);
		} finally {
			locker.unlock();
		}

		TCPMessage welcome = TCPMessage.unmarshalBinary(chunk);

		if ((welcome.getFlags() & TCPMessage.FLAG_MESSAGE_AUTH) == 0) {
			throw new RuntimeException("not authenticated");
		}

		if ((welcome.getFlags() & TCPMessage.FLAG_MESSAGE_END) == 0
				&& (welcome.getFlags() & TCPMessage.FLAG_MESSAGE_START) == 0) {
			throw new RuntimeException("auth message must be contained in a single message");
		}

		String welcomeMsg = new String(welcome.getData(), StandardCharsets.UTF_8);

		if (!welcomeMsg.equals(AUTH_REQUEST)) {
			throw new RuntimeException("authentication not requested");
		}

		Gson gson = new Gson();
		byte[] ccData = gson.toJson(credential, Credential.class).getBytes(StandardCharsets.UTF_8);

		conn.Send(ccData, TCPMessage.FLAG_MESSAGE_AUTH);

		try {
			locker.lock();
			socket.setSoTimeout(AUTH_MESSAGE_TIMEOUT);
			input.read(chunk);
		} finally {
			locker.unlock();
		}

		TCPMessage authRes = TCPMessage.unmarshalBinary(chunk);

		if ((authRes.getFlags() & TCPMessage.FLAG_MESSAGE_AUTH) == 0) {
			throw new RuntimeException("not authenticaed");
		}

		if ((authRes.getFlags() & TCPMessage.FLAG_MESSAGE_END) == 0
				&& (authRes.getFlags() & TCPMessage.FLAG_MESSAGE_START) == 0) {
			throw new RuntimeException("auth message must be contained in a single message");
		}

		boolean isAuth = new String(authRes.getData(), StandardCharsets.UTF_8).equals(AUTH_OK);

		if (!isAuth) {
			close();
		}

		return isAuth;
	}

	public TCPConnection getConn() {
		return this.conn;
	}

	public void close() throws IOException {
		socket.close();
	}

	public static class Credential {
		private String appId;

		public String getAppId() {
			return appId;
		}

		public void setAppId(String appId) {
			this.appId = appId;
		}

		public String getSecret() {
			return secret;
		}

		public void setSecret(String secret) {
			this.secret = secret;
		}

		private String secret;
	}
}

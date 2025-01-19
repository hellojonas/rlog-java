package io.github.hellojonas.rlog.tcp;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

public class TCPConnection {
    public static final int SOCKET_TIMEOUT = 5000;

    final private Socket socket;
    final private Lock locker;

    public TCPConnection(Socket socket) {
        this.socket = socket;
        locker = new ReentrantLock();
    }

    public void Send(byte[] data, byte flags) throws IOException {
        int maxPayload = TCPMessage.MESSAGE_MAX_LENGTH -
                TCPMessage.MESSAGE_HEADER_LENGTH;
        int parts = (int) Math.ceil(((double) data.length) / maxPayload);

        for (int i = 0; i < parts; i++) {
            int start = i * maxPayload;
            int end = (i + 1) * maxPayload;
            byte msgFlags = flags;

            if (i == 0) {
                msgFlags |= TCPMessage.FLAG_MESSAGE_START;
            }

            if (i == parts - 1) {
                msgFlags |= TCPMessage.FLAG_MESSAGE_END;
                end = data.length;
            }

            byte[] dataPart = Arrays.copyOfRange(data, start, end);

            TCPMessage msg = TCPMessage.builder()
                    .version(TCPMessage.MESSAGE_VERSION).flags(msgFlags)
                    .data(dataPart).build();

            byte[] payload = TCPMessage.marshalBinary(msg);

            try {
                locker.lock();
                socket.setSoTimeout(SOCKET_TIMEOUT);
                OutputStream out = socket.getOutputStream();
                out.write(payload);
                out.flush();
            } finally {
                locker.unlock();
            }
        }
    }

    public byte[] Recv() throws IOException {
        byte[] hChunk = new byte[TCPMessage.MESSAGE_HEADER_LENGTH];
        List<byte[]> data = new ArrayList<>();
        int read = 0;

        while (true) {
            InputStream input = socket.getInputStream();
            try {
                locker.lock();
                socket.setSoTimeout(SOCKET_TIMEOUT);
                input.read(hChunk);
            } finally {
                locker.unlock();
            }

            TCPMessage header = TCPMessage.unmarshalHeaderBinary(hChunk);
            byte[] payload = new byte[header.getLength()];

            try {
                locker.lock();
                socket.setSoTimeout(SOCKET_TIMEOUT);
                input.read(payload);
            } finally {
                locker.unlock();
            }

            read += header.getLength();
            data.add(payload);

            if ((header.getFlags() & TCPMessage.FLAG_MESSAGE_END) != 0) {
                break;
            }
        }

        byte[] d = new byte[read];
        int idx = 0;
        for (byte[] b : data) {
            System.arraycopy(b, 0, d, idx, b.length);
            idx = b.length;
        }

        return d;
    }

    public void Send(byte[] data) throws IOException {
        Send(data, (byte) 0);
    }

    public byte[] Recv(long timeout) {
        return Recv(timeout);
    }
}

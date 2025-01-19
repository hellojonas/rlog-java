package io.github.hellojonas.rlog.tcp;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;

public class TCPMessage {
    public static final byte MESSAGE_VERSION = 1;
    public static final int MESSAGE_MAX_LENGTH = 1024;
    public static final byte MESSAGE_HEADER_LENGTH = 4;

    public static final byte FLAG_MESSAGE_START = 1 << 0;
    public static final byte FLAG_MESSAGE_END = 1 << 1;
    public static final byte FLAG_MESSAGE_ERROR = 1 << 2;
    public static final byte FLAG_MESSAGE_AUTH = 1 << 3;

    private byte version;
    private byte flags;
    private short length;
    private byte[] data;

    public static byte[] marshalBinary(TCPMessage msg) {
        int msgLength = MESSAGE_HEADER_LENGTH + msg.data.length;

        if (msg.data == null || msg.data.length == 0) {
            throw new RuntimeException("invalid message data");
        }

        if (msgLength < MESSAGE_HEADER_LENGTH) {
            throw new RuntimeException("invalid message length. no data");
        }

        if (msgLength > MESSAGE_MAX_LENGTH) {
            throw new RuntimeException("message exceeds max length");
        }

        byte[] data = ByteBuffer.allocate(msgLength)
                .order(ByteOrder.BIG_ENDIAN)
                .put(msg.version)
                .put(msg.flags)
                .putShort((short) msg.data.length)
                .put(msg.data)
                .array();

        return data;
    }

    public static TCPMessage unmarshalHeaderBinary(byte[] data) {
        ByteBuffer buffer = ByteBuffer.wrap(data).order(ByteOrder.BIG_ENDIAN);
        byte version = buffer.get();
        byte flags = buffer.get();
        short length = buffer.getShort();

        return TCPMessage.builder()
                .version(version)
                .flags(flags)
                .length(length)
                .build();
    }

    public static TCPMessage unmarshalBinary(byte[] data) {
        ByteBuffer buffer = ByteBuffer.wrap(data).order(ByteOrder.BIG_ENDIAN);
        byte version = buffer.get();
        byte flags = buffer.get();
        short length = buffer.getShort();

        byte[] payload = new byte[length];

        buffer.get(payload, 0, length);

        return TCPMessage.builder()
                .version(version)
                .flags(flags)
                .length(length)
                .data(payload)
                .build();
    }

    public static TCPMessageBuilder builder() {
        return new TCPMessageBuilder();
    }

    public static class TCPMessageBuilder {
        private byte version = MESSAGE_VERSION;
        private byte flags;
        private short length;
        private byte[] data;

        public TCPMessageBuilder version(byte version) {
            this.version = version;
            return this;
        }

        public TCPMessageBuilder flags(byte flags) {
            this.flags = flags;
            return this;
        }

        public TCPMessageBuilder data(byte[] data) {
            this.data = data;
            return this;
        }

        public TCPMessageBuilder length(short length) {
            this.length = length;
            return this;
        }

        public TCPMessage build() {
            TCPMessage msg = new TCPMessage();
            msg.version = version;
            msg.flags = flags;
            msg.length = length;
            msg.data = data;
            return msg;
        }
    }

    public byte getVersion() {
        return this.version;
    }

    public byte getFlags() {
        return this.flags;
    }

    public short getLength() {
        return this.length;
    }

    public byte[] getData() {
        return this.data;
    }
}

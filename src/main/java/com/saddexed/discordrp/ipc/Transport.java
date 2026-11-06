package com.saddexed.discordrp.ipc;

import java.io.Closeable;
import java.io.IOException;
import java.nio.ByteBuffer;

public interface Transport extends Closeable {
    void write(ByteBuffer data) throws IOException;

    void readFully(ByteBuffer buffer) throws IOException;

    interface Factory {
        Transport open() throws IOException;
    }

    static Factory forOs(String osName) {
        return osName != null && osName.startsWith("Windows")
                ? WindowsPipeTransport.FACTORY
                : UnixSocketTransport.FACTORY;
    }
}

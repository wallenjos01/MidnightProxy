package org.wallentines.mdproxy.packet;

import io.netty.buffer.ByteBuf;

public class ServerboundLoginFinishedPacket implements Packet {

    public static final int ID = 2;

    @Override
    public int getId() {
        return ID;
    }

    @Override
    public void write(ByteBuf buf) { }

}

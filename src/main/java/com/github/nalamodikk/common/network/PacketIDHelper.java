package com.github.nalamodikk.common.network;

public class PacketIDHelper {
    private static int nextId = 0;

    public static int getNextId() {
        return nextId++;
    }
}

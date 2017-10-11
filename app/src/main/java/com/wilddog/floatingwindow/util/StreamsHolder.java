package com.wilddog.floatingwindow.util;

import com.wilddog.video.RemoteStream;
import com.wilddog.video.base.LocalStream;

/**
 * Created by fly on 17-10-9.
 */

public class StreamsHolder {
    private static LocalStream localStream;
    private static RemoteStream remoteStream;

    public static LocalStream getLocalStream() {
        return localStream;
    }

    public static RemoteStream getRemoteStream() {
        return remoteStream;
    }

    public static void setLocalStream(LocalStream localStream) {
        StreamsHolder.localStream = localStream;
    }

    public static void setRemoteStream(RemoteStream remoteStream) {
        StreamsHolder.remoteStream = remoteStream;
    }
}

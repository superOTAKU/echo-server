package org.otaku;

import java.net.InetSocketAddress;
import java.net.Socket;
import java.nio.charset.StandardCharsets;

public class EchoClient {
    public static void main(String[] args) throws Exception {
        Socket socket = new Socket();
        socket.setTcpNoDelay(true);
        socket.setKeepAlive(true);
        socket.connect(new InetSocketAddress(80), 15000);
        socket.getOutputStream().write("Hello".getBytes(StandardCharsets.UTF_8));
        //等待5秒，观察nio selector事件
        Thread.sleep(5000);
        //半关闭连接，表示数据发送完毕
        socket.shutdownOutput();
        //读到EOF为止
        byte[] receive = socket.getInputStream().readAllBytes();
        String receiveValue = new String(receive, StandardCharsets.UTF_8);
        System.out.printf("receive from server: %s%n", receiveValue);
    }
}

package org.otaku;

import java.net.*;
import java.nio.charset.StandardCharsets;
import java.util.Objects;

public class EchoServer {
    //绑定端口号
    private static final int PORT = 80;
    //等待accept的队列长度
    private static final int BACKLOG = 4096;

    public static void main(String[] args) throws Exception {
        ServerSocket ss = new ServerSocket();
        //允许TIME_WAIT状态的端口被重用
        ss.setReuseAddress(true);
        //accept超时时间
        ss.setSoTimeout(1000000);
        //缓冲区大小
        ss.setReceiveBufferSize(64000);
        //先设置好option再绑定端口
        ss.bind(new InetSocketAddress(PORT), BACKLOG);
        System.out.printf("server bind at port %s%n", PORT);
        while (true) {
            Socket s = ss.accept();
            new Thread(new ClientHandler(s)).start();
        }
    }

    public static class ClientHandler implements Runnable {
        private final Socket s;

        public ClientHandler(Socket s) {
            this.s = Objects.requireNonNull(s);
        }

        @Override
        public void run() {
            try {
                //禁用nagle算法，该算法以固定时间间隔发生packet，可能导致不必要的延迟
                s.setTcpNoDelay(true);
                //对于客户端socket，SO_TIMEOUT设置read和write阻塞时间，仅blocking mode有用
                s.setSoTimeout(10000);
                //空闲时间超过2小时，自动发生TCP心跳包
                s.setKeepAlive(true);
                //读到EOF为止
                byte[] receive = s.getInputStream().readAllBytes();
                String receiveValue = new String(receive, StandardCharsets.UTF_8);
                System.out.printf("receive from client: %s%n", receiveValue);
                s.getOutputStream().write(receive);
            } catch (Exception e) {
                e.printStackTrace();
            } finally {
                try {
                    s.close();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }
    }

}

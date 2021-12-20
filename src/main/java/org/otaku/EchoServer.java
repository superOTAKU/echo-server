package org.otaku;

import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

public class EchoServer {
    //绑定端口号
    private static final int PORT = 80;
    //等待accept的队列长度
    private static final int BACKLOG = 4096;
    private static final Map<SocketChannel, ByteBuffer> readBuffers = new HashMap<>();

    public static void main(String[] args) throws Exception {
        //selector用于选择就绪的channel进行读写
        Selector selector = Selector.open();
        //channel就是读写数据的通道
        ServerSocketChannel ssc = ServerSocketChannel.open();
        ssc.configureBlocking(false);
        ssc.bind(new InetSocketAddress(PORT), BACKLOG);
        System.out.printf("server bind at %s%n", PORT);
        //将自身注册到selector
        ssc.register(selector, SelectionKey.OP_ACCEPT);
        //selector开始select
        while (true) {
            //阻塞1秒
            selector.select(1000);
            //获取已就绪的事件进行处理
            Set<SelectionKey> selectionKeys = selector.selectedKeys();
            Iterator<SelectionKey> iter = selectionKeys.iterator();
            while (iter.hasNext()) {
                SelectionKey selectionKey = iter.next();
                if (!selectionKey.isValid()) {
                    System.out.printf("selectKey %s invalid", selectionKey);
                    iter.remove();
                    continue;
                }
                if (selectionKey.isAcceptable()) {
                    handleAcceptable(selector, selectionKey);
                }
                if (selectionKey.isReadable()) {
                    handleReadable(selectionKey);
                }
                //处理完移除SelectionKey
                iter.remove();
            }
        }
    }

    private static void handleAcceptable(Selector selector, SelectionKey selectionKey) throws Exception {
        ServerSocketChannel ssc = (ServerSocketChannel) selectionKey.channel();
        SocketChannel sc = ssc.accept();
        if (sc == null) {
            return;
        } else {
            System.out.printf("accept %s%n", sc);
        }
        sc.configureBlocking(false);
        readBuffers.put(sc, ByteBuffer.allocate(512));
        sc.register(selector, SelectionKey.OP_READ);
        System.out.printf("register %s to selector%n", sc);
    }

    private static void handleReadable(SelectionKey selectionKey) throws Exception {
        SocketChannel sc = (SocketChannel) selectionKey.channel();
        System.out.printf("channel %s readable%n", sc);
        ByteBuffer readBuffer = readBuffers.get(sc);
        //end of stream被认为是可读的，所以如果不cancel掉selectionKey，一直会被选中
        int read = sc.read(readBuffer);
        if (read == -1) {
            readBuffer.flip();
            byte[] data = new byte[readBuffer.remaining()];
            readBuffer.get(data);
            System.out.printf("from client: %s%n", new String(data, StandardCharsets.UTF_8));
            readBuffer.flip();
            //非阻塞写，直到写完所有字节为止，采用这种实现只是为了简单考虑，也可以追加到本地缓存，在writable事件中去写
            //netty的实现就是追加到写队列，然后在每次eventLoop中，如果channel可写就尝试写
            while (readBuffer.remaining() > 0) {
                sc.write(readBuffer);
            }
            selectionKey.cancel();
            sc.close();
            readBuffers.remove(sc);
        }
    }

}

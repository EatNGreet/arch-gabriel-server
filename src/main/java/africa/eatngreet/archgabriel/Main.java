package africa.eatngreet.archgabriel;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

public class Main {
    static Set<SocketChannel> socketChannels;
    public static void main(String[] args) {
        System.out.println("DO NOT BE AFRAID!!!");

        socketChannels = new HashSet<SocketChannel>();
        try (Selector selector = Selector.open();
             ServerSocketChannel ssc = ServerSocketChannel.open();) {
            ssc.bind(new InetSocketAddress(8008));
            ssc.configureBlocking(false);
            ssc.register(selector, SelectionKey.OP_ACCEPT);

            ByteBuffer bb = ByteBuffer.allocate(256);
            while (true) {
                selector.select();
                Set<SelectionKey> keys = selector.selectedKeys();
                Iterator<SelectionKey> iterator = keys.iterator();
                while(iterator.hasNext()) {
                    SelectionKey key = iterator.next();
                    if (key.isAcceptable()) {
                        SocketChannel socketChannel = ssc.accept();
                        socketChannel.configureBlocking(false);
                        socketChannel.register(selector, SelectionKey.OP_READ);
                        key.attach(socketChannel.getRemoteAddress());

                        System.out.println("New Client "+ socketChannel.getRemoteAddress().toString());
                        socketChannels.add(socketChannel);
                    }
                    if (key.isReadable()) {
                        try (SocketChannel socketChannel = (SocketChannel) key.channel()) {
                            int r = socketChannel.read(bb);

                            if(r == -1) {
                                socketChannels.remove(socketChannel);
                                String address = socketChannel.getRemoteAddress().toString();
                                socketChannel.close();
                                System.out.println("Client disconnected " + address);
                            }

                            bb.flip();

                            String message = new String(bb.array()).trim();
                            System.out.println("Broadcasting: " + message);
                            for(SocketChannel sock : socketChannels) {
                                if(sock.isConnected()) {
                                    sock.write(bb);
                                    bb.rewind();
                                }

                            }
                        }
                    }

                    iterator.remove();

                }
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        } finally {
            for(SocketChannel sock : socketChannels) {
                try {
                    sock.close();
                } catch (IOException e) {
                    System.out.println(e.getMessage());
                }
            }
        }
    }
}
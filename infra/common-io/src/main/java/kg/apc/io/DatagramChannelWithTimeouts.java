package kg.apc.io;

import java.io.IOException;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketAddress;
import java.net.SocketOption;
import java.net.SocketTimeoutException;
import java.nio.ByteBuffer;
import java.nio.channels.DatagramChannel;
import java.nio.channels.MembershipKey;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.util.Set;
import org.slf4j.LoggerFactory;
import org.slf4j.Logger;

public class DatagramChannelWithTimeouts extends DatagramChannel {

    protected DatagramChannel channel;
    protected Selector selector;
    private long readTimeout = 10000;
    protected SelectionKey channelKey;
    private static final Logger log = LoggerFactory.getLogger(DatagramChannelWithTimeouts.class);
    private boolean fastFirstPacketRead;

    protected DatagramChannelWithTimeouts() throws IOException {
        super(null);
        log.debug("Creating DatagramChannel");
        selector = Selector.open();
        channel = DatagramChannel.open();
        channel.configureBlocking(false);
        channelKey = channel.register(selector, SelectionKey.OP_READ);
    }

    public static DatagramChannel open() throws IOException {
        return new DatagramChannelWithTimeouts();
    }

    @Override
    public DatagramChannel bind(SocketAddress socketAddress) throws IOException {
        return channel.bind(socketAddress);
    }

    @Override
    public SocketAddress getLocalAddress() throws IOException {
        return channel.getLocalAddress();
    }

    @Override
    public <T> DatagramChannel setOption(SocketOption<T> socketOption, T t) throws IOException {
        return channel.setOption(socketOption, t);
    }

    @Override
    public <T> T getOption(SocketOption<T> socketOption) throws IOException {
        return channel.getOption(socketOption);
    }

    @Override
    public Set<SocketOption<?>> supportedOptions() {
        return channel.supportedOptions();
    }

    public int read(ByteBuffer dst) throws IOException {
        int bytesRead = 0;
        while (selector.select(readTimeout) > 0) {
            if (log.isDebugEnabled()) {
                log.debug("Loop " + bytesRead);
            }
            // damn NPE in unit tests...
            if (selector.selectedKeys() != null) {
                selector.selectedKeys().remove(channelKey);
            }
            int cnt = channel.read(dst);
            if (cnt < 1) {
                if (bytesRead < 1) {
                    bytesRead = -1;
                }
                return bytesRead;
            } else {
                bytesRead += cnt;
                if (!fastFirstPacketRead) {
                    fastFirstPacketRead = true;
                    return bytesRead;
                }
            }
        }

        if (bytesRead < 1) {
            throw new SocketTimeoutException("Timeout exceeded while reading from socket");
        }
        return bytesRead;
    }

    public long read(ByteBuffer[] dsts, int offset, int length) throws IOException {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    public int write(ByteBuffer src) throws IOException {
        fastFirstPacketRead = false;
        int res = 0;
        int size = src.remaining();
        while (res < size) {
            res += channel.write(src);
        }
        return res;
    }

    public long write(ByteBuffer[] srcs, int offset, int length) throws IOException {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    protected void implCloseSelectableChannel() throws IOException {
        channel.close();
        selector.close();
    }

    protected void implConfigureBlocking(boolean block) throws IOException {
        throw new UnsupportedOperationException("This class is blocking implementation of SocketChannel");
    }

    public boolean isConnected() {
        return channel.isConnected();
    }

    public void setReadTimeout(int t) {
        readTimeout = t;
    }

    public DatagramSocket socket() {
        return channel.socket();
    }

    public DatagramChannel disconnect() throws IOException {
        return channel.disconnect();
    }

    public SocketAddress receive(ByteBuffer dst) throws IOException {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    public int send(ByteBuffer src, SocketAddress target) throws IOException {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    public DatagramChannel connect(SocketAddress remote) throws IOException {
        return channel.connect(remote);
    }

    public SocketAddress getRemoteAddress() throws IOException {
        return null;
    }

    @Override
    public MembershipKey join(InetAddress inetAddress, NetworkInterface networkInterface) throws IOException {
        return channel.join(inetAddress, networkInterface);
    }

    @Override
    public MembershipKey join(InetAddress inetAddress, NetworkInterface networkInterface, InetAddress inetAddress1) throws IOException {
        return channel.join(inetAddress, networkInterface, inetAddress1);
    }
}

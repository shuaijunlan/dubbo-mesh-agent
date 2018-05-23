package cn.shuaijunlan.agent.provider.httpserver;

import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.socket.SocketChannel;
import io.netty.handler.codec.http.HttpObjectAggregator;
import io.netty.handler.codec.http.HttpRequestDecoder;
import io.netty.handler.codec.http.HttpResponseEncoder;
import io.netty.util.concurrent.DefaultEventExecutorGroup;
import io.netty.util.concurrent.EventExecutorGroup;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.atomic.AtomicInteger;


/**
 * @author Junlan Shuai[shuaijunlan@gmail.com].
 * @date Created on 21:15 2018/4/30.
 */
public class HttpSnoopServerInitializer extends ChannelInitializer<SocketChannel> {
    Logger logger = LoggerFactory.getLogger(HttpSnoopServerInitializer.class);
    private static AtomicInteger atomicInteger = new AtomicInteger(0);



    public HttpSnoopServerInitializer( ) {
    }

    @Override
    public void initChannel(SocketChannel ch) {
        logger.info("Create a new HttpSnoopServerInitializer instance: {}!", atomicInteger.incrementAndGet());
        EventExecutorGroup group = new DefaultEventExecutorGroup(16);

        ChannelPipeline p = ch.pipeline();
        p.addLast(new HttpRequestDecoder());
        // Uncomment the following line if you don't want to handle HttpChunks.
        p.addLast(new HttpObjectAggregator(2048));
        p.addLast(new HttpResponseEncoder());

        p.addLast(group, "handler", new HttpServerHandler());
    }
}

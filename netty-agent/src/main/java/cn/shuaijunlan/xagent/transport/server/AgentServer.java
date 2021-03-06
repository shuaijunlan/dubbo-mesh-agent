package cn.shuaijunlan.xagent.transport.server;

import cn.shuaijunlan.xagent.transport.support.kryo.KryoCodecUtil;
import cn.shuaijunlan.xagent.transport.support.kryo.KryoDecoder;
import cn.shuaijunlan.xagent.transport.support.kryo.KryoEncoder;
import cn.shuaijunlan.xagent.transport.support.kryo.KryoPoolFactory;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.*;
import io.netty.channel.epoll.EpollEventLoopGroup;
import io.netty.channel.epoll.EpollServerSocketChannel;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.timeout.IdleStateHandler;
import io.netty.util.concurrent.DefaultEventExecutorGroup;
import io.netty.util.concurrent.EventExecutorGroup;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author Junlan Shuai[shuaijunlan@gmail.com].
 * @date Created on 21:50 2018/4/28.
 */
public class AgentServer {
    private static final Logger LOGGER = LoggerFactory.getLogger(AgentServer.class);
    /**
     * Starting agent server
     * @param port
     */
    public static void start(int port) {
        EventLoopGroup bossGroup = new NioEventLoopGroup(1);
        EventLoopGroup workGroup = new NioEventLoopGroup(8);
        KryoCodecUtil util = new KryoCodecUtil(KryoPoolFactory.getKryoPoolInstance());
        EventExecutorGroup group = new DefaultEventExecutorGroup(256);
        try {
            ServerBootstrap bootstrap = new ServerBootstrap();
            bootstrap
                    .group(bossGroup, workGroup)
                    .channel(NioServerSocketChannel.class)
                    //保持长连接状态
                    .childOption(ChannelOption.SO_KEEPALIVE, true)
                    .childHandler(new ChannelInitializer<SocketChannel>() {
                        @Override
                        protected void initChannel(SocketChannel socketChannel) throws Exception {
                            ChannelPipeline p = socketChannel.pipeline();
//                            p.addLast(new IdleStateHandler(10, 0, 0));
///                            p.addLast(new LengthFieldBasedFrameDecoder(1024, 0, 4, -4, 0));
                            p.addLast(new KryoDecoder(util));
                            p.addLast(new KryoEncoder(util));
                            p.addLast(group, "handler", new AgentServerHandler());
                        }
                    });

            Channel ch = bootstrap.bind(port).sync().channel();
            if (LOGGER.isInfoEnabled()){
                LOGGER.info("Server agent is running, listening on port:{}", port);
            }
            ch.closeFuture().sync();
        } catch (Exception e) {
            throw new RuntimeException(e);
        } finally {
            bossGroup.shutdownGracefully();
            workGroup.shutdownGracefully();
            if (LOGGER.isInfoEnabled()){
                LOGGER.info("Agent server shutdown!");
            }
        }
    }
}

package cn.shuaijunlan.xagent.httpserver;

import cn.shuaijunlan.xagent.registry.Endpoint;
import cn.shuaijunlan.xagent.registry.EtcdRegistry;
import cn.shuaijunlan.xagent.registry.IRegistry;
import cn.shuaijunlan.xagent.transport.client.AgentClient;
import cn.shuaijunlan.xagent.transport.client.AgentClientManager;
import cn.shuaijunlan.xagent.transport.client.ResultMap;
import cn.shuaijunlan.xagent.transport.support.MessageRequest;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.*;
import io.netty.handler.codec.http.*;
import io.netty.util.CharsetUtil;
import io.netty.util.concurrent.DefaultEventExecutor;
import io.netty.util.concurrent.Promise;
import org.apache.commons.lang3.RandomStringUtils;
import org.asynchttpclient.AsyncHttpClient;
import org.asynchttpclient.ListenableFuture;
import org.asynchttpclient.Response;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


import java.io.IOException;
import java.util.List;
import java.util.Random;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

import static io.netty.handler.codec.http.HttpResponseStatus.BAD_REQUEST;
import static io.netty.handler.codec.http.HttpResponseStatus.OK;
import static io.netty.handler.codec.http.HttpVersion.HTTP_1_1;

/**
 * @author Junlan Shuai[shuaijunlan@gmail.com].
 * @date Created on 14:27 2018/5/6.
 */
public class HttpServerHandler extends ChannelInboundHandlerAdapter {
    private Logger logger = LoggerFactory.getLogger(ChannelInboundHandlerAdapter.class);
    private AsyncHttpClient asyncHttpClient = org.asynchttpclient.Dsl.asyncHttpClient();
    private AtomicInteger atomicInteger = new AtomicInteger(0);
    private String url = Constants.URLS[2];

    public HttpServerHandler(){
    }

    @Override
    public void channelReadComplete(ChannelHandlerContext ctx) {
        ctx.flush();
    }
    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) {
        if (msg instanceof FullHttpRequest){
            FullHttpRequest req = (FullHttpRequest) msg;
            ByteBuf content = req.content();
            if (content.isReadable()) {
                // 将耗时任务交给任务线程池处理
                ctx.executor().execute(() -> {
                    //执行远程调用
//                    String[] tmp = content.toString(CharsetUtil.UTF_8).split("&parameter=");
//                    content.release();
//                    String str = "";
//                    if (tmp.length > 1){
//                        str = tmp[1];
//                    }
                    String requestUrl = new StringBuilder(url).append("?").append(content.toString(CharsetUtil.UTF_8)).toString();
                    logger.info(requestUrl);
                    content.release();
                    org.asynchttpclient.Request request = org.asynchttpclient.Dsl.get(requestUrl).build();
                    ListenableFuture<Response> responseFuture = asyncHttpClient.executeRequest(request);

                    Runnable callback = () -> {
                        try {
                            // 获取远程结果
                            String value = responseFuture.get().getResponseBody();

                            FullHttpResponse response = new DefaultFullHttpResponse(
                                    HTTP_1_1,
                                    OK,
                                    Unpooled.copiedBuffer(value, CharsetUtil.UTF_8)
                            );
                            response.headers().set(HttpHeaderNames.CONTENT_TYPE, "text/plain; charset=UTF-8");
                            boolean keepAlive = HttpUtil.isKeepAlive(req);
                            if (keepAlive) {
                                response.headers().setInt(HttpHeaderNames.CONTENT_LENGTH, response.content().readableBytes());
                                response.headers().set(HttpHeaderNames.CONNECTION, HttpHeaderValues.KEEP_ALIVE);
                                ctx.writeAndFlush(response);
                            } else {
                                ctx.writeAndFlush(response).addListener(ChannelFutureListener.CLOSE);
                            }
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    };
                    responseFuture.addListener(callback, null);
                    ////////////////////////////////////////////////////////////////////////////
                });

            }
        }else {
            FullHttpResponse response = new DefaultFullHttpResponse(
                    HTTP_1_1,
                    BAD_REQUEST
            );
            ctx.write(response).addListener(ChannelFutureListener.CLOSE);
            logger.info("Wrong response!");
        }
    }
    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws IOException {
        cause.printStackTrace();
        asyncHttpClient.close();
        ctx.close();
    }
}

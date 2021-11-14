package com.bigduu.zerocopy

import io.netty.bootstrap.Bootstrap
import io.netty.buffer.ByteBuf
import io.netty.channel.*
import io.netty.channel.nio.NioEventLoopGroup
import io.netty.channel.socket.SocketChannel
import io.netty.channel.socket.nio.NioSocketChannel
import io.netty.handler.codec.serialization.ObjectEncoder
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import org.slf4j.LoggerFactory
import java.io.RandomAccessFile
import java.net.InetSocketAddress

fun main() {
    runBlocking {
        launch(Dispatchers.IO) { fileClient() }
    }
}

fun fileClient() {
    val group = NioEventLoopGroup()
    try {
        val bootstrap = Bootstrap()
        bootstrap.group(group)
            .channel(NioSocketChannel::class.java)
            .remoteAddress(InetSocketAddress("localhost", 8888))
            .option(ChannelOption.TCP_NODELAY, true)
            .handler(object : ChannelInitializer<SocketChannel>() {
                override fun initChannel(ch: SocketChannel) {
                    val pipeline = ch.pipeline()
                    pipeline.addLast(
                        ObjectEncoder(),
//                        ObjectDecoder(Int.MAX_VALUE, ClassResolvers.weakCachingConcurrentResolver(this.javaClass.classLoader)),
                        FileClientHandler()
                    )
                }
            })
        val sync = bootstrap.connect().sync()
        println("connect to localhost success")
        sync.channel().closeFuture().sync()
    } finally {
        group.shutdownGracefully()
    }
}

class FileClientHandler : ChannelInboundHandlerAdapter() {
    private val logger = LoggerFactory.getLogger(javaClass)
    private val randomAccessFile =
        RandomAccessFile("/Users/bigduu/KeyGeneratorKt_4343_07.11.2021_16.38.28.hprof.bac", "rw")

    override fun channelActive(ctx: ChannelHandlerContext) {
        logger.info("enter channelActive")
        ctx.writeAndFlush(RequestFile("/Users/bigduu/KeyGeneratorKt_4343_07.11.2021_16.38.28.hprof"))
    }

    override fun channelRead(ctx: ChannelHandlerContext?, msg: Any?) {
        val byteBuf = msg as ByteBuf
        val get = byteBuf.getByte(0)
        if (get.toInt() != 0x12345678) {
            val byteArray = ByteArray(byteBuf.readableBytes())
            byteBuf.readBytes(byteArray)
            randomAccessFile.seek(randomAccessFile.filePointer)
            randomAccessFile.write(byteArray)
            byteBuf.release()
        }
    }

}

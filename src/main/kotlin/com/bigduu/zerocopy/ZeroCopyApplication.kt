package com.bigduu.zerocopy

import io.netty.bootstrap.ServerBootstrap
import io.netty.channel.*
import io.netty.channel.nio.NioEventLoopGroup
import io.netty.channel.socket.SocketChannel
import io.netty.channel.socket.nio.NioServerSocketChannel
import io.netty.handler.codec.LineBasedFrameDecoder
import io.netty.handler.codec.serialization.ClassResolvers
import io.netty.handler.codec.serialization.ObjectDecoder
import io.netty.handler.codec.serialization.ObjectEncoder
import io.netty.handler.codec.string.StringDecoder
import io.netty.handler.codec.string.StringEncoder
import io.netty.handler.logging.LogLevel
import io.netty.handler.logging.LoggingHandler
import io.netty.handler.ssl.SslHandler
import io.netty.handler.stream.ChunkedFile
import io.netty.handler.stream.ChunkedWriteHandler
import org.slf4j.LoggerFactory
import org.springframework.boot.CommandLineRunner
import org.springframework.boot.autoconfigure.SpringBootApplication
import java.io.RandomAccessFile
import java.nio.channels.FileChannel
import java.util.concurrent.atomic.AtomicInteger


@SpringBootApplication
class ZeroCopyApplication : CommandLineRunner {
    override fun run(vararg args: String?) {
    }
}

fun main(args: Array<String>) {
//    runApplication<ZeroCopyApplication>(*args)
    fileServer()
}


fun fileServer() {
    val bossGroup = NioEventLoopGroup(1)
    val workerGroup = NioEventLoopGroup()
    try {
        val serverBootstrap = ServerBootstrap()
        serverBootstrap
            .group(bossGroup, workerGroup)
            .channel(NioServerSocketChannel::class.java)
            .option(ChannelOption.SO_BACKLOG, 100)
            .handler(LoggingHandler(LogLevel.INFO))
            .childHandler(object : ChannelInitializer<SocketChannel>() {
                override fun initChannel(ch: SocketChannel) {
                    val pipeline = ch.pipeline()
                    pipeline.addLast(
//                        ObjectEncoder(),
                        ObjectDecoder(ClassResolvers.weakCachingConcurrentResolver(this.javaClass.classLoader)),
                        FileServerHandler()
                    )
                }
            })
        val sync = serverBootstrap.bind("127.0.0.1", 8888).sync()
        sync.channel().closeFuture().sync()
    } finally {
        bossGroup.shutdownGracefully()
        workerGroup.shutdownGracefully()
    }
}

class FileServerHandler : SimpleChannelInboundHandler<RequestFile>() {
    private val counter = AtomicInteger(0)
    private val logger = LoggerFactory.getLogger(javaClass)

    override fun channelRead0(ctx: ChannelHandlerContext?, msg: RequestFile?) {
        logger.info("$msg")
        val path = msg?.path ?: ""
        val randomAccessFile = RandomAccessFile(path, "r")
        ctx?.writeAndFlush(DefaultFileRegion(randomAccessFile.channel, 0, randomAccessFile.length()))
            ?.addListener {
                if (it.isSuccess){
                    logger.info("send finished")
                    ctx.close()
                }
            }
    }

}

/*
override fun channelActive(ctx: ChannelHandlerContext) {
    val msg = "/Users/bigduu/tmp.txt"
    logger.info("enter channelRead0 and msg is $msg, ${counter.getAndIncrement()}")
    var raf: RandomAccessFile? = null
    var length: Long = -1
    try {
        raf = RandomAccessFile(msg as String, "r")
        length = raf.length()
    } catch (e: Exception) {
        ctx!!.writeAndFlush(""" ERR: ${e.javaClass.simpleName}: ${e.message} """.trimIndent())
        return
    } finally {
        if (length < 0 && raf != null) {
            raf.close()
        }
    }

    ctx!!.write(""" OK: ${raf!!.length()} """.trimIndent())
    if (ctx.pipeline()[SslHandler::class.java] == null) {
        ctx.write(DefaultFileRegion(raf.channel, 0, length))
    } else {
        // SSL enabled - cannot use zero-copy file transfer.
        ctx.write(ChunkedFile(raf))
    }
    ctx.writeAndFlush("\n")
}
override fun channelRead(ctx: ChannelHandlerContext?, msg: Any?) {
    logger.info("enter channelRead0 and msg is $msg, ${counter.getAndIncrement()}")
    var raf: RandomAccessFile? = null
    var length: Long = -1
    try {
        raf = RandomAccessFile(msg as String, "r")
        length = raf.length()
    } catch (e: Exception) {
        ctx!!.writeAndFlush(""" ERR: ${e.javaClass.simpleName}: ${e.message} """.trimIndent())
        return
    } finally {
        if (length < 0 && raf != null) {
            raf.close()
        }
    }

    ctx!!.write(""" OK: ${raf!!.length()} """.trimIndent())
    if (ctx.pipeline()[SslHandler::class.java] == null) {
        ctx.write(DefaultFileRegion(raf.channel, 0, length))
    } else {
        // SSL enabled - cannot use zero-copy file transfer.
        ctx.write(ChunkedFile(raf))
    }
    ctx.writeAndFlush("\n")
}
*/

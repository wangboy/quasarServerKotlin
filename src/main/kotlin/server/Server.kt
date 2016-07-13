package server

import client.startClient
import co.paralleluniverse.actors.ActorRef
import co.paralleluniverse.actors.ActorRegistry
import co.paralleluniverse.fibers.Suspendable
import co.paralleluniverse.fibers.io.FiberServerSocketChannel
import co.paralleluniverse.fibers.io.FiberSocketChannel
import co.paralleluniverse.kotlin.Actor
import co.paralleluniverse.kotlin.fiber
import co.paralleluniverse.kotlin.register
import co.paralleluniverse.kotlin.spawn
import co.paralleluniverse.strands.channels.Channels
import co.paralleluniverse.strands.channels.IntChannel
import logger.initLogger
import message.*
import org.slf4j.LoggerFactory
import player.PlayerManager
import player.playerChannelMap
import java.net.InetSocketAddress
import java.nio.ByteBuffer


//TODO 跳过socket，直接actor
/**
 * Created by wangbo on 16/6/28.
 */
val SERVER_PORT = 1234

val sync: IntChannel = Channels.newIntChannel(1)

val slogger = LoggerFactory.getLogger("server")

fun main(args: Array<String>) {

    initLogger()

    slogger.info("==== server started ===")

//    System.getProperties().forEach {
//        slogger.info(" system : ${it.key} = ${it.value}")
//    }


    startServer()

    sync.receive()

    startClient(0)
}

val playerManagerRef: ActorRef<Any> = ActorRegistry.getActor(PlayerManager.name)
val serverActorRef: ActorRef<Any?>? = ActorRegistry.getActor(ServerActor.name)


fun startServer() {
    fiber {
        val socket: FiberServerSocketChannel =
                FiberServerSocketChannel.open().bind(InetSocketAddress(SERVER_PORT))
        slogger.info(" server bind $SERVER_PORT")

        sync.send(1)

        while (true) {
            var channel: FiberSocketChannel = socket.accept()

            slogger.info("client connected ${channel.remoteAddress}")

            fiber {
                var client = channel
                var buf: ByteBuffer = ByteBuffer.allocate(1024)
                while (true) {
                    if (!client.isOpen) {
                        playerManagerRef.send(DisconnectMessage(client))
                        try {
                            client.close()
                        } catch(e: Exception) {
                            e.printStackTrace()
                        }
                    }

                    ///decode
                    slogger.info(" decode from $buf and $client 1111")



                    /////////////
//                    buf.clear()
//                    client.read(buf)
//
//                    buf.flip()
//                    println(" buf receive ${buf.array()}")


                    //////////


                    var msg = decode(buf, client)
                    slogger.info("receive msg = ${msg?.content}")
                    ///dispatch
                    when (msg?.content) {
                    /// to manager
                        is LoginReq, is CreateRoleReq -> playerManagerRef.send(msg)
                        null -> slogger.info(" null msg ")
                        else -> playerChannelMap[client]?.send(msg)
                    }
                }
            }
        }
    }
}

object ServerActor : Actor() {
    init {
        this.name = ServerActor::class.java.simpleName
        register(this.name, this)
        spawn(this)
    }

    @Suspendable
    override fun doRun(): Any? {
        while (true) {
            receive {
                when (it) {
                    is NetMessage -> when (it.content) {
                        is LoginReq, is CreateRoleReq -> playerManagerRef.send(it)
                        null -> slogger.info(" null msg ")
                        else -> playerChannelMap[it.channel]?.send(it)
                    }
                    else -> unkownMsg(this, it)
                }
            }
        }
    }


}
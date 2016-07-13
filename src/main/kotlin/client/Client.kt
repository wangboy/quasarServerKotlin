package client

import co.paralleluniverse.fibers.Suspendable
import co.paralleluniverse.fibers.io.FiberSocketChannel
import co.paralleluniverse.kotlin.Actor
import co.paralleluniverse.kotlin.fiber
import co.paralleluniverse.kotlin.spawn
import common.LOGIN_NO_ACCOUNT
import logger.clogger
import message.*
import server.SERVER_PORT
import java.net.InetSocketAddress
import java.nio.ByteBuffer
import java.util.concurrent.TimeUnit

/**
 * Created by wangbo on 16/6/28.
 */
//val clientLogger = LoggerFactory.getLogger("client")


fun main(args: Array<String>) {
    clogger.info("startClient")
}

fun startClient(count: Int) {
    for (i in 0..count) {
        var cName = "Client_$i"
        fiber(cName) {
            var channel: FiberSocketChannel = FiberSocketChannel.open(InetSocketAddress(SERVER_PORT))

            ////////
//            var buf: ByteBuffer = ByteBuffer.allocate(1024)
//            buf.putLong(123142)
//            buf.flip()
//            channel.write(buf)


            ////////


            clogger.info(" connect to server ")
            spawn(Client(cName, channel))
        }
    }
}

enum class ClientState(val code: Int) {
    INIT(0),
    LOGEDIN(1)

}

class Client(val account: String, val channel: FiberSocketChannel) : Actor() {

    var state: ClientState = ClientState.INIT

    lateinit var data: PlayerDataInitResp

    @Suspendable
    override fun doRun(): Any? {

        fiber {
            clogger.info(" start client receive fiber ")
            var buf: ByteBuffer = ByteBuffer.allocate(1024)
            while (true) {
                var msg: NetMessage? = decode(buf, channel)

                clogger.info(" client ${this.account} receive msg $msg")

                msg?.run { this@Client.self().send(msg) }
            }
        }


        while (true) {
            clogger.info(" try receive ")

            try {
                receive(50, TimeUnit.MILLISECONDS) {
                    clogger.info(" receive sth $it")
                    when (it) {
                        is NetMessage -> handleNetMessage(it)
                        else -> {
                            when (state) {
                                ClientState.INIT -> login()
                                ClientState.LOGEDIN -> clientAction()
                                else -> clogger.info(" unkown state : ${state}")
                            }
                            null
                        }
                    }
                }
            } catch(e: Exception) {
                e.printStackTrace()
            }


            ////////////

//            var buf: ByteBuffer = ByteBuffer.allocate(1024)
//            buf.putLong(123142)
//            buf.flip()
//            channel.write(buf)
            ////////////
//TODO 这里不会运行

            clogger.info(" after receive ")



        }
    }

    fun handleNetMessage(msg: NetMessage): Unit? {
        return when (msg.content) {
            is LoginResp -> {
                when (msg.content.result) {
                    LOGIN_NO_ACCOUNT -> createRole()
                    else -> null
                }
            }
            is PlayerDataInitResp -> this.data = msg.content
            is ChatResp -> clogger.info(" receive chat msg \"${msg.content.chatMsg}\" from ${msg.content.playerName}")
            else -> null
        }
    }


    @Suspendable
    fun createRole() =
            this.channel.sendMessage(CreateRoleReq("${this.account}", "123456"))

    @Suspendable
    fun login() {
        this.channel.sendMessage(LoginReq("${this.account}", "123456"))
    }

    @Suspendable
    fun clientAction() {
        this.channel.sendMessage(ChatReq(" hello every , I'm ${data.name}"))
    }

}

//TODO action tree
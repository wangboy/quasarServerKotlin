/**
 * Created by wangbo on 16/6/27.
 */
package demo

import co.paralleluniverse.fibers.io.FiberServerSocketChannel
import co.paralleluniverse.fibers.io.FiberSocketChannel
import co.paralleluniverse.kotlin.fiber
import co.paralleluniverse.strands.Strand
import co.paralleluniverse.strands.channels.Channels
import co.paralleluniverse.strands.channels.IntChannel
import java.net.InetSocketAddress
import java.nio.ByteBuffer

val PORT = 1234
val CLIENT_COUNT = 100

fun clientConnect(ch: FiberSocketChannel) {
    fiber {
        var buf: ByteBuffer = ByteBuffer.allocate(1024)
        while (true) {
            with(buf) {
                clear()
                ch.read(buf)
                flip()
                var clientMsg: Long = long

                clear()
                clientMsg++
                putLong(clientMsg)
                flip()
                ch.write(buf)
            }
        }
    }
}

fun startClient(index: Int) {
    fiber("$index") {
        var client: FiberSocketChannel = FiberSocketChannel.open(InetSocketAddress(PORT))
        var buf: ByteBuffer = ByteBuffer.allocate(1024)
        var msgId: Long = 1

        var lastCheck = 0

        while (true) {
            buf.clear()
            buf.putLong(msgId)
            buf.flip()
            client.write(buf)

            buf.clear()
            client.read(buf)
            buf.flip()
            msgId = buf.long

            if (msgId - lastCheck > 10000) {
                lastCheck = msgId.toInt()
                println("client_$index got $msgId")
            }

            Strand.sleep(1)
        }
    }
}

fun doAll() {

    val sync: IntChannel = Channels.newIntChannel(0)

    fiber {
        val socket: FiberServerSocketChannel = FiberServerSocketChannel.open().bind(InetSocketAddress(PORT))
        println(" started")

        sync.send(1)

        var count = 0
        while (true) {
            var ch: FiberSocketChannel = socket.accept()
            println(" client connect ${count++} ")
            clientConnect(ch)
        }
    }

    var sig = sync.receive()

    for (i in 0..CLIENT_COUNT) {
        startClient(i)
        Strand.sleep(5)
    }

    Strand.sleep(Long.MAX_VALUE)
}

fun main(args: Array<String>) {
    doAll()
}
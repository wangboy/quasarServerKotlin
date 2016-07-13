package message

import co.paralleluniverse.actors.ActorRef
import co.paralleluniverse.fibers.Suspendable
import co.paralleluniverse.fibers.io.FiberSocketChannel
import com.alibaba.fastjson.JSON
import logger.mlogger
import logger.slogger
import service.CHAT_WORLD
import java.nio.ByteBuffer
import java.nio.charset.Charset

/**
 * Created by wangbo on 16/6/28.
 */

/**
 * Message Id
 */


/**
 * |---id---|---length---|---content---|
 */

@Suspendable
fun decode(buffer: ByteBuffer, channel: FiberSocketChannel): NetMessage? {
    mlogger.info(" decode from $buffer and $channel 22222")

    channel.read(buffer)

    mlogger.info("buffer size : ${buffer}")

    buffer.flip()

    mlogger.info("2222")


    var remain: Int = buffer.remaining()
    if (remain < 4 + 4) {
        buffer.position(remain)
        buffer.limit(buffer.capacity())
        return null
    }
    mlogger.info("3333")

    var msgId: Int = buffer.int
    var length: Int = buffer.int

    if (remain - 4 - 4 < length) {
        buffer.position(remain)
        buffer.limit(buffer.capacity())
        return null
    }

    mlogger.info("4444")


    var content: ByteArray = ByteArray(length)

    buffer.get(content)

    remain = buffer.remaining()

    mlogger.info("5555")

    var remainData: ByteArray = ByteArray(remain)
    buffer.get(remainData)
    buffer.clear()
    buffer.put(remainData)

    mlogger.info("6666")

    mlogger.info(" parse $msgId  to ${msgIdContentMap[msgId]}")

    return NetMessage(msgId, JSON.parseObject(String(content), msgIdContentMap[msgId]), channel)
}

@Suspendable
fun encode(buf: ByteBuffer, msg: Any): ByteBuffer {

    var msgId = msgIdContentMap.filter {
        it.value == msg.javaClass
    }.keys.first()
    buf.putInt(msgId)
    var json: String = JSON.toJSONString(msg)
    var jsonBytes: ByteArray = json.toByteArray(Charset.defaultCharset())
    buf.putInt(jsonBytes.size)
    buf.put(jsonBytes)
    buf.flip()
    return buf
}


/////////////////////////////////////////////

open class InternalMessage(open val from: ActorRef<Any?>?)

class PlayerChat(override val from: ActorRef<Any?>, val msg: String, val senderName: String) : InternalMessage(from)


//TODO from system actor
data class DisconnectMessage(val ch: FiberSocketChannel)

data class PlayerDisconnectMessage(var from: ActorRef<Any?>)

/////////////////////////////////////
open class NetMessage(val msgId: Int, val content: Any, val channel: FiberSocketChannel)

////////////////////////////

data class LoginReq(var account: String = "", var pw: String = "") {
    constructor() : this("", "")
}

data class LoginResp(val result: Int)

data class CreateRoleReq(val account: String, val pw: String)

data class PlayerDataInitResp(val id: Long, var name: String, var exp: Long, var lv: Int, var gold: Long) //TODO

data class ChatReq(val chatMsg: String, val type: Int = CHAT_WORLD, val toPlayerId: Long = -1)

data class ChatResp(val chatMsg: String, val playerName: String)

data class SelfActionReq(val type: Int, val param: Map<String, Any?>)

data class BuyGoodsReq(val goodsId: Int, val count: Int)

val msgIdContentMap: Map<Int, Class<out Any>> = mapOf(
        1001 to LoginReq::class.java,
        1002 to LoginResp::class.java,
        1003 to CreateRoleReq::class.java,
        1004 to PlayerDataInitResp::class.java,
        2001 to ChatReq::class.java,
        2002 to ChatResp::class.java,
        3001 to SelfActionReq::class.java,
        4001 to BuyGoodsReq::class.java
)

fun foo() {

}
//////////////////////////////////

@Suspendable
fun FiberSocketChannel.sendMessage(msg: Any) {

    mlogger.info(" [Message] send message $msg")

    var buf: ByteBuffer = ByteBuffer.allocate(1024)
    encode(buf, msg)
    write(buf)
}

//TODO duplicate code everywhere
fun unkownMsg(receiver: Any, msg: Any?): Unit? {
    slogger.info("$receiver receive unknown msg ${JSON.toJSONString(msg, true)}")
    return null
}


package player

import co.paralleluniverse.actors.ActorRef
import co.paralleluniverse.fibers.Suspendable
import co.paralleluniverse.fibers.io.FiberSocketChannel
import co.paralleluniverse.kotlin.Actor
import co.paralleluniverse.strands.Strand
import message.*
import server.slogger
import service.buyGoods
import service.playerChat
import service.selfAction
import kotlin.properties.Delegates

/**
 * Created by wangbo on 16/6/28.
 */

class Player(val id: Long, var name: String, var exp: Long, var lv: Int, var gold: Long, val channel: FiberSocketChannel) {
    var actorRef: ActorRef<Any?> by Delegates.notNull<ActorRef<Any?>>()
}

class PlayerActor(val p: Player) : Actor() {
    var run: Boolean = true
    @Suspendable
    override fun doRun(): Void? {
        name = p.name
        while (true) {
            receive {
                when (it) {
                    is PlayerDisconnectMessage -> onDisconnnect(it.from)
                    is InternalMessage -> handleInternalMsg(it)
                    is NetMessage -> handleClientMessage(it)
                    else -> unkownMsg(this, it)
                }
            }
        }
    }

    fun handleClientMessage(msg: NetMessage): Unit? {
        return when (msg.content) {
            is SelfActionReq -> selfAction(this.p, msg.content.type, msg.content.param)
            is ChatReq -> playerChat(this.p, msg.content.chatMsg, msg.content.type, msg.content.toPlayerId)
            is BuyGoodsReq -> buyGoods(this.p, msg.content.goodsId, msg.content.count)
            else -> unkownMsg(this, msg)
        }
    }

    fun handleInternalMsg(msg: InternalMessage): Unit? {
        return when (msg) {
            is PlayerChat -> this.p.sendMessage(ChatResp(msg.msg, msg.senderName))
            else -> unkownMsg(this, msg)
        }
    }

    fun onDisconnnect(watcher: ActorRef<Any?>) {
        //TODO save
        slogger.info(" player ${name}offline save ")
        Strand.sleep(10)

        throw PlayerOffLine
    }
}

fun Player.sendMessage(msg: Any) {
    this.channel.sendMessage(msg)
}

object PlayerOffLine : Throwable()


package player

import co.paralleluniverse.actors.ActorRef
import co.paralleluniverse.actors.BasicActor
import co.paralleluniverse.actors.ExitMessage
import co.paralleluniverse.actors.LifecycleMessage
import co.paralleluniverse.fibers.Suspendable
import co.paralleluniverse.fibers.io.FiberSocketChannel
import co.paralleluniverse.kotlin.register
import co.paralleluniverse.kotlin.spawn
import common.LOGIN_NO_ACCOUNT
import message.*
import server.slogger
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.ThreadLocalRandom

/**
 * Created by wangbo on 16/6/29.
 */


val playerRefMap: MutableMap<ActorRef<Any?>, PlayerActor> = ConcurrentHashMap()

val playerIdMap: MutableMap<Long, Player> = ConcurrentHashMap()

val playerChannelMap: MutableMap<FiberSocketChannel, ActorRef<Any?>> = ConcurrentHashMap()


object PlayerManager : BasicActor<Any, Void>() {
    init {
        name = PlayerManager.javaClass.simpleName
        register(name, this)
        spawn(this)
    }

    @Suspendable
    override fun doRun(): Void? {
        while (true) {
            receive {
                when (it) {
                    is NetMessage -> handleClientMessage(it)
                    is DisconnectMessage -> handleDisconnect(it)
                    is InternalMessage -> handleInterlaMessage(it)
                    else -> unkownMsg(this, it)
                }
            }
        }
    }

    private fun handleDisconnect(msg: DisconnectMessage) {
        var clientChannel = msg.ch
        var playerRef: ActorRef<Any?>? = playerChannelMap[clientChannel]
        playerRef?.send(PlayerDisconnectMessage(self()))

//        playerRef?.send(object{
//            fun run() {
//
//            }
//        })
    }

    @Suspendable
    fun handleClientMessage(msg: NetMessage): Unit? {

        var content: Any = msg.content
        return when (msg.content) {
            is LoginReq -> msg.channel.sendMessage(LoginResp(LOGIN_NO_ACCOUNT))
            is CreateRoleReq -> {
                var player = createRole(msg.content.account, msg.content.pw, msg.channel)
                spawnPlayer(player)
                player.sendMessage(PlayerDataInitResp(player.id, player.name, player.exp, player.lv, player.gold))
            }
            else -> null
        }
    }

    fun createRole(account: String, ps: String, channel: FiberSocketChannel): Player
            = Player(ThreadLocalRandom.current().nextLong(), account, 0, 1, 10000, channel)

    fun spawnPlayer(player: Player): ActorRef<Any?> {

        var playerActor = PlayerActor(player)
        var playerRef = spawn(playerActor)

        player.actorRef = playerRef

        playerRefMap.put(playerRef, playerActor)
        playerIdMap.put(player.id, player)
        playerChannelMap.put(player.channel, playerRef)

        watch(playerRef)
        return playerRef
    }

    fun handleInterlaMessage(msg: InternalMessage) {
        throw UnsupportedOperationException()
    }

    override fun handleLifecycleMessage(m: LifecycleMessage?): Any? {
        if (m is ExitMessage) {
            var actorRef: ActorRef<Any?> = m.actor
            var reason = m.cause

            when (reason) {
                is PlayerOffLine -> slogger.info(" player ${actorRef.name} off line !")
                else -> {
                    slogger.info(" other reason , try restart player ${actorRef.name}")
                    var player = playerRefMap[actorRef]?.p
                    playerRefMap.remove(actorRef)
                    player?.run { spawnPlayer(player) }
                }
            }
        }
        return super.handleLifecycleMessage(m)
    }
}


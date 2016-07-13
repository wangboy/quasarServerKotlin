package service

import message.ChatResp
import message.PlayerChat
import player.Player
import player.playerIdMap
import player.playerRefMap
import player.sendMessage

/**
 * Created by wangbo on 16/6/29.
 */

val CHAT_WORLD: Int = 1
val CHAT_PRIVATE: Int = 2

fun playerChat(player: Player, chatMsg: String, type: Int, toPlayer: Long) {
    when (type) {
        CHAT_WORLD -> worldChat(player, chatMsg)
        CHAT_PRIVATE -> playerIdMap[toPlayer]?.run { privateChat(player, playerIdMap[toPlayer]!!, chatMsg) }
    }
}

fun worldChat(player: Player, msg: String) {
    playerRefMap.keys.forEach {
        it.send(PlayerChat(player.actorRef, msg, player.name))
    }
}

fun privateChat(from: Player, to: Player, msg: String) {
    to.sendMessage(ChatResp(msg, from.name))
}
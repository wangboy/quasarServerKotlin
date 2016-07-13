package demo

import co.paralleluniverse.actors.ActorRef
import co.paralleluniverse.actors.BasicActor
import co.paralleluniverse.actors.ExitMessage
import co.paralleluniverse.actors.LifecycleMessage
import co.paralleluniverse.fibers.Suspendable
import co.paralleluniverse.strands.Strand
import java.util.concurrent.ThreadLocalRandom
import java.util.concurrent.TimeUnit


fun main(args: Array<String>) {
    NaiveActor("naive").spawn()
    Strand.sleep(Long.MAX_VALUE)
}

class BadActor : BasicActor<String, Void>() {

    var count: Int = 0

    @Suspendable
    override fun doRun(): Void? {
        println("(re)starting actor")
        while (true) {
            var m: String? = receive(300, TimeUnit.MILLISECONDS)
            m?.run {
                println(" got a message $m")
            }
            println("I am but a lowly actor that sometime fails : - ${count++}")

            if (ThreadLocalRandom.current().nextInt(30) == 0)
                return null
//                throw RuntimeException("darn")
            checkCodeSwap()
        }

    }

}

class NaiveActor(val n: String) : BasicActor<Void, Void>(n) {
    lateinit private var myBadActor: ActorRef<String>

    @Suspendable
    override fun doRun(): Void? {
        spawnBadActor()
        var count = 0
        while (true) {
            receive(500, TimeUnit.MILLISECONDS)
            myBadActor.send(" hi from ${self()} number ${count++}")
        }
    }

    private fun spawnBadActor() {
        myBadActor = BadActor().spawn()
        watch(myBadActor)
    }

    override fun handleLifecycleMessage(m: LifecycleMessage?): Void? {
        if (m is ExitMessage && m.getActor() == myBadActor) {
            println(" my bad actor has just died of ${m.getCause()}. Restarting")
            spawnBadActor()
        }
        return super.handleLifecycleMessage(m)

    }

}
/**
 * Created by wangbo on 16/6/27.
 */
package demo;

import co.paralleluniverse.kotlin.Receive
import co.paralleluniverse.kotlin.Send
import co.paralleluniverse.kotlin.fiber
import co.paralleluniverse.kotlin.select
import co.paralleluniverse.strands.Strand
import co.paralleluniverse.strands.channels.Channel
import co.paralleluniverse.strands.channels.Channels


fun main(args: Array<String>): Unit {
    println("=============== main start ==========")
//    fiberTest()
//    selectTest()

    seleTest2()
    println("=============== main end ==========")
}

fun selectTest(): Unit {
    val ch1 = Channels.newChannel<Int>(1)
    val ch2 = Channels.newChannel<Double>(1)

    val ret = fiber {
        select(Receive(ch1), Send(ch2, 2.0)) {
            when (it) {
                is Receive<*> -> println(" receive ${it.msg}")//dumpStack { it.msg }
                is Send<*> -> println("send 0")//dumpStack { 0 }
                else -> println("select null")//dumpStack { -1 }
            }
        }
    }.get()
    println(" select ret = ${ret}")
}

fun seleTest2(): Unit {
    val ch1 = Channels.newChannel<Int>(100)
    val ch2 = Channels.newChannel<Int>(100)

    fiber {
        for (i in 1..10) {
            var ch: Channel<Int> = if (i % 2 == 0) ch1 else ch2
            select(Receive(ch1), Send(ch, i)) {
                when (it) {
                    is Receive<*> -> println("recive ${i}")
                    is Send<*> -> println(" send ${i}")
                    else -> println(" null ")
                }
            }
        }
    }.get()
}

fun fiberTest(): Unit {
    println("start at ${System.currentTimeMillis()}")
    var fiberRet = fiber {
        println("start")
        dumpStack()
        Strand.sleep(100)
        println(" end ")
        1
    }.get()

    println(" fiber $fiberRet")

    println("end at ${System.currentTimeMillis()}")
}


fun <T> dumpStack(f: () -> T): T {
    Throwable().printStackTrace()
    return f()
}

fun dumpStack() = Throwable().printStackTrace()
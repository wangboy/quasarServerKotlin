package demo

import co.paralleluniverse.kotlin.fiber
import co.paralleluniverse.strands.Strand

/**
 * Created by wangbo on 16/6/28.
 */


fun main(args: Array<String>) {

    for (i in 0..50000) {
        fiber {
            Strand.sleep(Int.MAX_VALUE.toLong())
        }
        if(i % 1000 ==0) println(" fiber count $i")

    }
    Strand.sleep(Long.MAX_VALUE)

}
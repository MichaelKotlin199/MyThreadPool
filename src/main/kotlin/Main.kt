package org.example

import MyThreadPool
import java.lang.Thread.sleep

fun main() {
    val threadPool = MyThreadPool(3)

    for (i in 1..7) {
        threadPool.execute {
            println("$i")
            Thread.sleep(1000)
        }
    }
    sleep(4000)
    threadPool.shutdown()
}
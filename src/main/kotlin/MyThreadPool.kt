import java.util.LinkedList
import java.util.concurrent.Executor
import java.util.concurrent.RejectedExecutionException
import java.util.concurrent.locks.Condition
import java.util.concurrent.locks.ReentrantLock

class MyThreadPool(private val threadCount: Int) : Executor {
    private val tasks = LinkedList<Runnable>()
    private val threads: List<Thread>
    private val lock = ReentrantLock()
    private val condition: Condition = lock.newCondition()

    @Volatile
    private var isActive = true

    init {
        threads = List(threadCount) { index ->
            Thread {
                try {
                    while (true) {
                        val task: Runnable?

                        lock.lock()
                        try {
                            while (tasks.isEmpty() && isActive) {
                                condition.await()
                            }

                            if (tasks.isEmpty() && !isActive) {
                                break
                            }

                            task = tasks.removeFirst()
                        } finally {
                            lock.unlock()
                        }

                        try {
                            task?.run()
                        } catch (e: Exception) {
                            println("Исключение при выполнении задачи: ${e.message}")
                        }
                    }
                } catch (e: InterruptedException) {
                    println("Поток ${Thread.currentThread().name} был прерван.")
                }
            }.apply {
                name = "ThreadPool-Worker-$index"
                isDaemon = true
                start()
            }
        }
    }

    override fun execute(command: Runnable) {
        lock.lock()
        try {
            if (!isActive) {
                throw RejectedExecutionException()
            }
            tasks.addLast(command)
            condition.signal()
        } finally {
            lock.unlock()
        }
    }

    fun shutdown(wait: Boolean = false) {
        lock.lock()
        try {
            isActive = false
            condition.signalAll()
        } finally {
            lock.unlock()
        }

        if (wait) {
            threads.forEach {
                try {
                    it.join()
                } catch (e: InterruptedException) {
                    println("Ожидание потока ${it.name} прервано.")
                }
            }
        } else {
            threads.forEach { it.interrupt() }
        }
    }
}

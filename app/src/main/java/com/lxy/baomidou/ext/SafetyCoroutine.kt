package com.supermap.sinfcloud.basecomponent.ext

import kotlinx.coroutines.AbstractCoroutine
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.InternalCoroutinesApi
import kotlinx.coroutines.handleCoroutineException
import kotlin.coroutines.CoroutineContext
import kotlin.coroutines.cancellation.CancellationException

/**
 *
 * @Author：liuxy
 * @Date：2024/4/8 8:52
 * @Desc：
 *
 */
@OptIn(InternalCoroutinesApi::class)
class SafetyCoroutine<T>(
    parentContext: CoroutineContext
) : AbstractCoroutine<T>(parentContext + CoroutineExceptionHandler { _, error ->
    // 这里打印日志，想写就写
    error.printStackTrace()
}, initParentJob = true, active = true) {

    /**
     * 协程异常回调
     * （数组定义为0是为了节省内存，定义为0的情况下，初始状态下不会分配内存，添加数据后 ArrayList 扩容比较收敛，
     * 具体自己看源码
     * 这里添加的方法回调不会很多的，不需要扩容大量内存）
     */
    private var catchBlock = ArrayList<((Throwable) -> Unit)>(0)

    /**
     * 执行成功
     */
    private var successBlock = ArrayList<((T) -> Unit)>(0)

    /**
     * 执行取消
     * cancel
     */
    private var cancelBlock = ArrayList<((Throwable) -> Unit)>(0)

    /**
     * 执行完成
     */
    private var completeBlock = ArrayList<((T?) -> Unit)>(0)



    /**
     * 代码发生异常，才会执行此方法
     */
    override fun handleJobException(exception: Throwable): Boolean {
        handleCoroutineException(context, exception)
        if (exception !is CancellationException) { // CancellationException 的不处理
            catchBlock.forEach { it.invoke(exception) }
        }
        return true
    }

    /**
     * 只有代码正常执行完毕，才会执行此方法
     * （一定是成功后才会走，协程被取消情况不会走这里）
     */
    override fun onCompleted(value: T) {
        super.onCompleted(value)
        successBlock.forEach { it.invoke(value) }
        completeBlock.forEach { it.invoke(value) }
        removeCallbacks()
    }

    /**
     * 协程被取消，会执行此方法
     */
    override fun onCancelled(cause: Throwable, handled: Boolean) {
        super.onCancelled(cause, handled)
        cancelBlock.forEach { it.invoke(cause) }
        completeBlock.forEach { it.invoke(null) }
        removeCallbacks()
    }

    private fun removeCallbacks() {
        successBlock.clear()
        catchBlock.clear()
        cancelBlock.clear()
        completeBlock.clear()
    }

    fun onCatch(catch: (e: Throwable) -> Unit) = apply {
        catchBlock.add(catch)
    }

    fun onSuccess(success: (T) -> Unit) = apply {
        successBlock.add(success)
    }

    fun onCancel(cancel: (Throwable) -> Unit) = apply {
        cancelBlock.add(cancel)
    }

    fun onComplete(complete: (T?) -> Unit) = apply {
        completeBlock.add(complete)
    }
}

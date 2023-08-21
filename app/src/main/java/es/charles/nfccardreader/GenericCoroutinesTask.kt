package es.charles.nfccardreader

import android.content.Intent
import android.util.Log
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext


abstract class GenericCoroutinesTask<Params, Progress, Result>(val taskName: String) {
    private val TAG: String by lazy { GenericCoroutinesTask::class.java.simpleName }
    companion object { private var threadPoolExecutor: CoroutineDispatcher? = null }

    var status: Status = Status.PENDING
    var preJob: Job? = null
    var bgJob: Deferred<Result?>? = null
    abstract fun doInBackground(params: Intent?): Result?
    open fun onProgressUpdate(vararg values: Progress?) {}
    open fun onPostExecute(result: Result?) {}
    open fun onPreExecute() {}
    open fun onCancelled(result: Result?) {}
    private var isCancelled = false

    enum class Status {
        PENDING,
        RUNNING,
        FINISHED
    }

    @OptIn(DelicateCoroutinesApi::class)
    fun execute(dispatcher: CoroutineDispatcher, params: Intent) {

        if (status != Status.PENDING) {
            when (status) {
                Status.RUNNING -> throw IllegalStateException("Cannot execute task:" + " the task is already running.")
                Status.FINISHED -> throw IllegalStateException(
                    "Cannot execute task:"
                            + " the task has already been executed "
                            + "(a task can be executed only once)"
                )

                else -> {
                }
            }
        }

        status = Status.RUNNING

        // it can be used to setup UI - it should have access to Main Thread
        GlobalScope.launch(Dispatchers.Main) {
            preJob = launch(Dispatchers.Main) {
                Log.w(TAG, "$taskName onPreExecute started")
                onPreExecute()
                Log.w(TAG, "$taskName onPreExecute finished")
                bgJob = async(dispatcher) {
                    Log.w(TAG, "$taskName doInBackground started")
                    doInBackground(params)
                }
            }
            preJob!!.join()
            if (!isCancelled) {
                withContext(Dispatchers.Main) {
                    onPostExecute(bgJob!!.await())
                    Log.w(TAG, "$taskName doInBackground finished")
                    status = Status.FINISHED
                }
            }
        }
    }

    @OptIn(DelicateCoroutinesApi::class)
    fun cancel(mayInterruptIfRunning: Boolean) {
        if (preJob == null || bgJob == null) {
            Log.w(TAG, "$taskName has already been cancelled/finished/not yet started.")
            return
        }
        if (mayInterruptIfRunning || (!preJob!!.isActive && !bgJob!!.isActive)) {
            isCancelled = true
            status = Status.FINISHED
            if (bgJob!!.isCompleted) {
                GlobalScope.launch(Dispatchers.Main) {
                    onCancelled(bgJob!!.await())
                }
            }
            preJob?.cancel(CancellationException("PreExecute: Coroutine Task cancelled"))
            bgJob?.cancel(CancellationException("doInBackground: Coroutine Task cancelled"))
            Log.w(TAG, "$taskName has been cancelled.")
        }
    }

    @OptIn(DelicateCoroutinesApi::class)
    fun publishProgress(vararg progress: Progress) {
        //need to update main thread
        GlobalScope.launch(Dispatchers.Main) {
            if (!isCancelled) {
                onProgressUpdate(*progress)
            }
        }
    }
}

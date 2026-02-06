package com.sy.exception;

/**
 * 任务已被用户取消时抛出，用于落库等流程提前退出且不将状态改为 FAILED/COMPLETED
 */
public class TaskCancelledException extends RuntimeException {

    public TaskCancelledException(Long taskId) {
        super("Task cancelled: " + taskId);
    }
}

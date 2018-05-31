package com.alan.complete;

import org.flowable.engine.delegate.DelegateExecution;
import org.flowable.engine.delegate.JavaDelegate;

/**
 * @author acer
 */
public class SendRejectionMail implements JavaDelegate {

    @Override
    public void execute(DelegateExecution delegateExecution) {
        System.out.println("Approve rejected " + delegateExecution.getVariable("employee"));
    }
}

//
//  ========================================================================
//  Copyright (c) 1995-2012 Mort Bay Consulting Pty. Ltd.
//  ------------------------------------------------------------------------
//  All rights reserved. This program and the accompanying materials
//  are made available under the terms of the Eclipse Public License v1.0
//  and Apache License v2.0 which accompanies this distribution.
//
//      The Eclipse Public License is available at
//      http://www.eclipse.org/legal/epl-v10.html
//
//      The Apache License v2.0 is available at
//      http://www.opensource.org/licenses/apache2.0.php
//
//  You may elect to redistribute this code under either of these licenses.
//  ========================================================================
//

package org.eclipse.jetty.websocket.common.io;

import java.nio.ByteBuffer;
import java.nio.channels.InterruptedByTimeoutException;

import javax.net.websocket.SendResult;

import org.eclipse.jetty.io.EofException;
import org.eclipse.jetty.util.FutureCallback;
import org.eclipse.jetty.util.log.Log;
import org.eclipse.jetty.util.log.Logger;
import org.eclipse.jetty.util.thread.Scheduler;
import org.eclipse.jetty.websocket.common.WebSocketFrame;

public abstract class FrameBytes extends FutureCallback<SendResult> implements Runnable
{
    private final static Logger LOG = Log.getLogger(FrameBytes.class);
    protected final AbstractWebSocketConnection connection;
    protected final WebSocketFrame frame;
    // Task used to timeout the bytes
    protected volatile Scheduler.Task task;

    protected FrameBytes(AbstractWebSocketConnection connection, WebSocketFrame frame)
    {
        this.connection = connection;
        this.frame = frame;
    }

    private void cancelTask()
    {
        Scheduler.Task task = this.task;
        if (task != null)
        {
            task.cancel();
        }
    }

    @Override
    public void completed(SendResult v)
    {
        super.completed(v);
        if (LOG.isDebugEnabled())
        {
            LOG.debug("completed({}) - {}",v,this.getClass().getName());
        }
        cancelTask();
        connection.complete(this);
        frame.notifySendHandler();
    }

    @Override
    public void failed(SendResult v, Throwable x)
    {
        super.failed(v,x);
        if (x instanceof EofException)
        {
            // Abbreviate the EofException
            LOG.warn("failed(" + v + ") - " + EofException.class);
        }
        else
        {
            LOG.warn("failed(" + v + ")",x);
        }
        cancelTask();
        frame.notifySendHandler(x);
    }

    public abstract ByteBuffer getByteBuffer();

    @Override
    public void run()
    {
        // If this occurs we had a timeout!
        connection.close();
        failed(null,new InterruptedByTimeoutException());
    }

    @Override
    public String toString()
    {
        return frame.toString();
    }
}
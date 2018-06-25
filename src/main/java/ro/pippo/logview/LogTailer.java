/*
 * Copyright (C) 2018 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package ro.pippo.logview;

import org.apache.commons.io.input.Tailer;
import org.apache.commons.io.input.TailerListenerAdapter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ro.pippo.core.websocket.WebSocketContext;

import java.io.File;
import java.io.IOException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * @author Decebal Suiu
 */
public class LogTailer extends TailerListenerAdapter {

    private static final Logger log = LoggerFactory.getLogger(LogTailer.class);

    private final File logFile;
    private final WebSocketContext webSocketContext;
    private long delayMillis = 1000;
    private boolean end;

    private Tailer tailer;
    private ExecutorService executorService;

    public LogTailer(File file, WebSocketContext webSocketContext) {
        this(file, webSocketContext, Executors.newCachedThreadPool());
    }

    public LogTailer(File file, WebSocketContext webSocketContext, ExecutorService executorService) {
        this.logFile = file;
        this.webSocketContext = webSocketContext;
        this.executorService = executorService;
    }

    public void setDelayMillis(long delayMillis) {
        this.delayMillis = delayMillis;
    }

    public void setEnd(boolean end) {
        this.end = end;
    }

    public void start() {
        tailer = new Tailer(logFile, this, delayMillis, end);
        executorService.execute(tailer);
    }

    public void stop() {
        tailer.stop();
        executorService.shutdown();
    }

    @Override
    public void handle(String line) {
        try {
            webSocketContext.sendMessage(line);
        } catch (IOException e) {
            log.error(e.getMessage(), e);
        }
    }

    @Override
    public void handle(Exception ex) {
        try {
            webSocketContext.sendMessage(ex.toString());
        } catch (IOException e) {
            log.error(e.getMessage(), e);
        }
    }

}
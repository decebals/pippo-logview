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

import ro.pippo.core.Application;
import ro.pippo.core.util.IoUtils;
import ro.pippo.core.websocket.WebSocketContext;
import ro.pippo.core.websocket.WebSocketHandler;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;

/**
 * @author Decebal Suiu
 */
public class LogViewApplication extends Application {

    @Override
    protected void onInit() {
        // add routes for static content
        addPublicResourceRoute();
        addWebjarsResourceRoute();

        getRouter().ignorePaths("/favicon.ico");

        // add routes (serve an html that contains the javascript -> websocket client)
        GET("/", routeContext -> {
            try {
                String html = getResourceAsString("/index.html");
                // replace "host" placeholder
                html = html.replace("__HOST__", getPippoSettings().getString("server.host", "localhost"));
                // replace "post" placeholder
                html = html.replace("__PORT__", getPippoSettings().getString("server.port", "8338"));

                routeContext.send(html);
            } catch (IOException e) {
                e.printStackTrace();
            }
        });

        addWebSocket("/log", new WebSocketHandler() {

            private LogTailer logTailer;

            @Override
            public void onOpen(WebSocketContext webSocketContext) {
                // check if the log file exists
                File logFile = new File(getPippoSettings().getRequiredString("logview.tailer.file"));
                if (!logFile.exists() || !logFile.isFile()) {
                    log.error("File '{}' not found", logFile);
                    return;
                }

                // create the log tailer
                logTailer = new LogTailer(logFile, webSocketContext);

                // set some tailer's properties read from pippo settings
                logTailer.setDelayMillis(getPippoSettings().getLong("logview.tailer.delayMillis", 1000));
                logTailer.setEnd(getPippoSettings().getBoolean("logview.tailer.end", false));

                try {
                    webSocketContext.sendMessage(getLogviewSettingsAsJson());
                    logTailer.start();
                } catch (IOException e) {
                    log.error(e.getMessage(), e);
                }
            }

            @Override
            public void onClose(WebSocketContext webSocketContext, int closeCode, String message) {
                if (logTailer != null) {
                    logTailer.stop();
                }
            }

            @Override
            public void onMessage(WebSocketContext webSocketContext, String message) {
                // do nothing
            }

        });

        // the idea with this route is to write lines in log file
        GET("/test", routeContext -> routeContext.send("Test"));
    }

    private String getLogviewSettingsAsJson() throws IOException {
        return new StringBuilder()
            .append('{')
            .append("\"lines\": \"").append(getPippoSettings().getInteger("logview.lines", 5000)).append("\"")
            .append(", ")
            .append("\"noindent\": \"").append(getPippoSettings().getBoolean("logview.noindent", false)).append("\"")
            .append(", ")
            .append("\"highlight\": ").append(getResourceAsString("/conf/logview-highlight.json"))
            .append('}')
            .toString();
    }

    private String getResourceAsString(String resource) throws IOException {
        try (InputStream inputStream = getClass().getResourceAsStream(resource)) {
            return IoUtils.toString(inputStream);
        }
    }

}

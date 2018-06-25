This project create a tiny web log view using [Pippo](http://www.pippo.ro) and WebSocket.  
A secondary goal of this project is to test the modularity in Pippo, to see if it's possible to create reusable components.
  
![Upload](screenshots/logview.png?raw=true)
  
The code is trivial because it's very easy to add a web socket functionality with Pippo.
All important code is available in [LogViewApplication.java](https://github.com/decebals/pippo-logview/blob/master/src/main/java/ro/pippo/logview/LogViewApplication.java).

```java
public class LogViewApplication extends Application {

    @Override
    protected void onInit() {
        // add routes for static content
        addPublicResourceRoute();
        addWebjarsResourceRoute();

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

        });

        // the idea with this route is to write lines in log file
        GET("/test", routeContext -> routeContext.send("Test"));
    }

}
```

The [boostrap](https://getbootstrap.com) support is added via webjars (I added boostrap to show you how easy is to add static web artifacts).
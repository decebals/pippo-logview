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

Related to modularity, my idea is to deliver pippo-logview as a reusable component that is easily integrated into a Pippo application.
So, this component (that any web component) contains:
- some `css` static resources (`logview.css` available in `resources\css`)
- some `javascript` static resources (`logview.js` available in `resources\js`)
- some Pippo's routes available in `LogViewApplication`; here we can come with a (LogView)`RouteGroup` 
- some `json` static resources (for example `logview-highlight.json` that is available in `conf` - I am not sure that is the correct folder)
- some properties (embedded in this project directly in `application.properties` from `conf` )
```properties
logview.tailer.file = app.log
#logview.tailer.delayMillis = 1000
#logview.tailer.end = false
#logview.lines = 5000
#logview.noindent = false
``` 

package org.facboy.engineio.jsapi;

import org.facboy.engineio.EngineIo;
import org.facboy.engineio.event.ConnectionEvent;
import org.facboy.engineio.event.EngineIoEventListener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.core.MessageSendingOperations;
import org.springframework.stereotype.Controller;

/**
 * @author Christopher Ng
 */
@Controller
public class ConnectionController {
    @Autowired
    public ConnectionController(final MessageSendingOperations<String> messagingTemplate, EngineIo engineIo) {
        engineIo.addConnectionEventListener(new EngineIoEventListener<ConnectionEvent>() {
            @Override
            public void onEvent(ConnectionEvent event) {
                messagingTemplate.convertAndSend("/topic/connections", event);
            }
        });
    }
}

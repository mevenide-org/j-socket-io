package org.facboy.engineio;

import java.io.IOException;

import javax.websocket.Endpoint;
import javax.websocket.EndpointConfig;
import javax.websocket.MessageHandler.Whole;
import javax.websocket.Session;

/**
 * @author Christopher Ng
 */
public class EngineIoEndpoint extends Endpoint {
    @Override
    public void onOpen(Session session, EndpointConfig config) {
        session.addMessageHandler(new Whole<String>() {
            @Override
            public void onMessage(String message) {
                System.currentTimeMillis();
            }
        });
        try {
            session.getBasicRemote().sendText("FEEEH");
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }


}

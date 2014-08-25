package org.facboy.engineio.jsapi;

import java.util.Map;

import org.facboy.engineio.EngineIo;
import org.facboy.engineio.event.EngineIoEventListener;
import org.facboy.engineio.event.MessageEvent;
import org.facboy.engineio.protocol.BinaryPacket;
import org.facboy.engineio.protocol.Packet.Type;
import org.facboy.engineio.protocol.StringPacket;
import org.facboy.engineio.session.Session;
import org.facboy.engineio.session.SessionRegistry;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.core.MessageSendingOperations;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Controller;

/**
 * Represents engine.io's internal 'socket' interface (used by the tests to control the behaviour of the server socket
 * listeners).
 *
 * @author Christopher Ng
 */
@Controller
public class SocketController {
    private final SessionRegistry sessionRegistry;

    @Autowired
    public SocketController(final MessageSendingOperations<String> messagingTemplate, EngineIo engineIo,
            SessionRegistry sessionRegistry) {
        this.sessionRegistry = sessionRegistry;

        engineIo.addMessageEventListener(new EngineIoEventListener<MessageEvent>() {
            @Override
            public void onEvent(MessageEvent event) {
                Object data;
                if (event.getMessage() instanceof StringPacket) {
                    data = ((StringPacket) event.getMessage()).getData();
                } else if (event.getMessage() instanceof BinaryPacket) {
                    BinaryPacket packet = (BinaryPacket) event.getMessage();
                    data = new byte[packet.getLength()];
                    //noinspection SuspiciousSystemArraycopy
                    System.arraycopy(packet.getData(), packet.getOffset(), data, 0, packet.getLength());
                } else {
                    throw new IllegalArgumentException("Unexpected message class: " + event.getMessage().getClass());
                }
                messagingTemplate.convertAndSend("/topic/sockets/" + event.getSession().getId() + "/messages", data);
            }
        });
    }

    @MessageMapping("sockets/{sessionId}")
    public void receiveMessage(@DestinationVariable String sessionId, @Payload Map<String, Object> message) {
        Session session = sessionRegistry.getSession(sessionId);
        if (session != null) {
            session.send(new StringPacket(Type.MESSAGE, (String) message.get("data")));
        }
    }
}

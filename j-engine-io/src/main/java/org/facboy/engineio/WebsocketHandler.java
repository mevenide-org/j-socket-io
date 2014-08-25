//  Copyright (c) 2014 Christopher Ng
//
//  isUpgradeRequest() method derived from Jetty 9, copyright notice reproduced below:
//
//    ========================================================================
//    Copyright (c) 1995-2014 Mort Bay Consulting Pty. Ltd.
//    ------------------------------------------------------------------------
//    All rights reserved. This program and the accompanying materials
//    are made available under the terms of the Eclipse Public License v1.0
//    and Apache License v2.0 which accompanies this distribution.
//
//        The Eclipse Public License is available at
//        http://www.eclipse.org/legal/epl-v10.html
//
//        The Apache License v2.0 is available at
//        http://www.opensource.org/licenses/apache2.0.php
//
//    You may elect to redistribute this code under either of these licenses.
//    ========================================================================
//
package org.facboy.engineio;

import javax.servlet.http.HttpServletRequest;

import com.google.common.base.Splitter;
import com.google.inject.Singleton;

/**
 * @author Christopher Ng
 */
@Singleton
public class WebsocketHandler {
    private static final Splitter commaSplitter = Splitter.on(',');

    /**
     * Adapted from Jetty 9's org.eclipse.jetty.websocket.serverWebSocketServerFactory.  Rearranged
     * for no good reason to try and put more specific tests first and less specific (ie lots of requests
     * will pass them) tests last.
     *
     * @param request HttpServletRequest
     * @return {@code true} if this is a websocket upgrade request.
     */
    public boolean isUpgradeRequest(HttpServletRequest request) {
        String upgrade = request.getHeader("Upgrade");
        if (upgrade == null) {
            // no "Upgrade: websocket" header present.
            return false;
        }
        if (!"websocket".equalsIgnoreCase(upgrade)) {
            return false;
        }

        String connection = request.getHeader("connection");
        if (connection == null) {
            // no "Connection: upgrade" header present.
            return false;
        }

        // Test for "Upgrade" token last as it is slower
        boolean foundUpgradeToken = false;
        for (String token : commaSplitter.split(connection)) {
            if ("upgrade".equalsIgnoreCase(token)) {
                foundUpgradeToken = true;
                break;
            }
        }
        if (!foundUpgradeToken)
        {
            return false;
        }

        if (!"GET".equals(request.getMethod())) {
            // not a "GET" request (not a websocket upgrade)
            return false;
        }
        if (!"HTTP/1.1".equals(request.getProtocol())) {
            return false;
        }

        return true;
    }
}

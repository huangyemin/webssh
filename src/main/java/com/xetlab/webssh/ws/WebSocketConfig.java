package com.xetlab.webssh.ws;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.socket.config.annotation.EnableWebSocket;
import org.springframework.web.socket.config.annotation.WebSocketConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;

@EnableWebSocket
@Configuration
public class WebSocketConfig implements WebSocketConfigurer {

    @Autowired
    private TerminalHandshakeInterceptor terminalHandshakeInterceptor;
    @Autowired
    private TerminalWebSocketHandler terminalWebSocketHandler;

    @Override
    public void registerWebSocketHandlers(WebSocketHandlerRegistry webSocketHandlerRegistry) {
        webSocketHandlerRegistry.addHandler(terminalWebSocketHandler, "/websocket")
                .addInterceptors(terminalHandshakeInterceptor)
                .setAllowedOrigins("*");
        webSocketHandlerRegistry.addHandler(terminalWebSocketHandler, "/sockjs/websocket")
                .addInterceptors(terminalHandshakeInterceptor)
                .withSockJS();
    }

}

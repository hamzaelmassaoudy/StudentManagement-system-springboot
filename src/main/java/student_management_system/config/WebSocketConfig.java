package student_management_system.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;

/**
 * Configures WebSocket messaging with STOMP for real-time communication (chat).
 * Updated to include user destination configuration for private messaging.
 */
@Configuration
@EnableWebSocketMessageBroker
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {

    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        // Endpoint for clients to connect
        registry.addEndpoint("/ws").withSockJS();
    }

    @Override
    public void configureMessageBroker(MessageBrokerRegistry registry) {
        // Prefix for messages bound for @MessageMapping methods (controllers)
        registry.setApplicationDestinationPrefixes("/app");

        // Prefix for general broadcast topics (like class chat)
        // AND prefix for user-specific queues (private chat)
        // The broker routes messages starting with /topic or /user
        registry.enableSimpleBroker("/topic", "/queue"); // Enable queue for user destinations

        // --- NEW: Define prefix for user-specific destinations ---
        // Messages sent to destinations like "/user/{username}/queue/private"
        // will be routed correctly by Spring if this prefix is set.
        // The SimpMessagingTemplate.convertAndSendToUser() method uses this.
        registry.setUserDestinationPrefix("/user");
        // --- END NEW ---
    }

    // Optional: Configure WebSocket message size limits, security, etc.
    // @Override
    // public void configureWebSocketTransport(WebSocketTransportRegistration registration) {
    //     registration.setMessageSizeLimit(128 * 1024); // example: 128KB
    // }
}

package com.stitch.app.config;

import com.stitch.app.security.JwtService;
import lombok.RequiredArgsConstructor;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.MessageHeaderAccessor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class WebSocketAuthInterceptor implements ChannelInterceptor {

    private final JwtService jwtService;
    private final UserDetailsService userDetailsService;

    @Override
    public Message<?> preSend(Message<?> message, MessageChannel channel) {
        StompHeaderAccessor accessor = MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor.class);

        if (accessor != null && StompCommand.CONNECT.equals(accessor.getCommand())) {
            String authToken = accessor.getFirstNativeHeader("Authorization");

            UsernamePasswordAuthenticationToken authentication = null;

            if (authToken != null && authToken.startsWith("Bearer ")) {
                String jwt = authToken.substring(7);
                String username = jwtService.extractUsername(jwt);

                if (username != null) {
                    UserDetails userDetails = userDetailsService.loadUserByUsername(username);

                    if (jwtService.isTokenValid(jwt, userDetails)) {
                        authentication = new UsernamePasswordAuthenticationToken(
                            userDetails,
                            null,
                            userDetails.getAuthorities()
                        );

                        // set SecurityContext for other security checks
                        SecurityContextHolder.getContext().setAuthentication(authentication);
                    }

                    // For STOMP user destination mapping, set the accessor user to a Principal whose name is the user's id
                    try {
                        // userDetails is actually our User entity which has getId()
                        Object principalObj = userDetails;
                        java.lang.reflect.Method getIdMethod = principalObj.getClass().getMethod("getId");
                        Object idObj = getIdMethod.invoke(principalObj);
                        String idStr = idObj != null ? String.valueOf(idObj) : username;
                        WebSocketPrincipal wsPrincipal = new WebSocketPrincipal(idStr);
                        accessor.setUser(wsPrincipal);
                    } catch (Exception e) {
                        // fallback: set the authenticated UserDetails as accessor user which will use username (email)
                        if (authentication != null) {
                            accessor.setUser(authentication);
                        } else {
                            // final fallback: set a simple Principal with the username
                            final String nameFallback = username;
                            accessor.setUser(new java.security.Principal() {
                                @Override
                                public String getName() {
                                    return nameFallback != null ? nameFallback : "unknown";
                                }
                            });
                        }
                    }
                }
            }
        }

        return message;
    }
}

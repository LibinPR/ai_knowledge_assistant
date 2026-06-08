package com.yourorg.aiknowledgeassistant.config;

import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;
import jakarta.servlet.*;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.time.Duration;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

@Component
public class RateLimitFilter extends OncePerRequestFilter {

  private final Map<String , Bucket> buckets = new ConcurrentHashMap<>();

  private Bucket newBucket() {
    return Bucket.builder()
            .addLimit(Bandwidth.builder()
                .capacity(40)
                .refillGreedy(40 , Duration.ofMinutes(1))
                .build())
            .build();
  }

  @Override
  public void doFilterInternal(HttpServletRequest request , HttpServletResponse response , FilterChain chain) throws IOException , ServletException {

    String path = request.getRequestURI();
    if(!path.startsWith("/api/rag/")) {
      chain.doFilter(request , response);
      return;
    }

    // String key = request.getRemoteAddr();
    Authentication authentication = 
          SecurityContextHolder
                  .getContext()
                  .getAuthentication();
    String key;

    if(authentication != null
            && authentication.getPrincipal() instanceof Long userId
    ) {
      key = userId.toString();
    } else {
      key = request.getRemoteAddr();
    }
    Bucket bucket = buckets.computeIfAbsent(key , k -> newBucket());

    if(bucket.tryConsume(1)) {
      chain.doFilter(request, response);
    } else {
      response.setStatus(429);
      response.setContentType("application/json");
      response.getWriter().write("{\"error\":\"Too many requsets. Try again in a minute.\",\"status\":429}");
    }
  }
}
package com.inventory.backend.auth;

import com.inventory.backend.exception.ApiException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerInterceptor;

/**
 * Requires a logged-in user on every /api endpoint unless it is marked
 * {@link PublicEndpoint}, and the ADMIN role where it is marked {@link AdminOnly}.
 */
@Component
public class AuthInterceptor implements HandlerInterceptor {

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        if (!(handler instanceof HandlerMethod method) || method.hasMethodAnnotation(PublicEndpoint.class)) {
            return true;
        }

        SessionUser user = SessionUser.from(request);
        if (user == null) {
            throw new ApiException(HttpStatus.UNAUTHORIZED, "Sesion expirada. Inicia sesion de nuevo.");
        }

        boolean adminOnly = method.hasMethodAnnotation(AdminOnly.class)
                || method.getBeanType().isAnnotationPresent(AdminOnly.class);
        if (adminOnly && !user.isAdmin()) {
            throw new ApiException(HttpStatus.FORBIDDEN, "No tienes permiso para esta accion");
        }
        return true;
    }
}

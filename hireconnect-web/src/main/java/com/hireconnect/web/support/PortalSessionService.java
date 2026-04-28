package com.hireconnect.web.support;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.ModelAndView;

import com.hireconnect.web.dto.PortalSession;

import jakarta.servlet.http.HttpSession;

@Service
public class PortalSessionService {

    private final String attributeName;

    public PortalSessionService(@Value("${app.session.attribute}") String attributeName) {
        this.attributeName = attributeName;
    }

    public PortalSession getCurrentSession(HttpSession session) {
        Object value = session.getAttribute(attributeName);
        return value instanceof PortalSession portalSession ? portalSession : null;
    }

    public PortalSession requireAuthenticated(HttpSession session) {
        PortalSession portalSession = getCurrentSession(session);
        if (portalSession == null) {
            throw new PortalException(org.springframework.http.HttpStatus.UNAUTHORIZED, "Please log in first");
        }
        return portalSession;
    }

    public PortalSession requireRole(HttpSession session, String role) {
        PortalSession portalSession = requireAuthenticated(session);
        if (!role.equalsIgnoreCase(portalSession.role())) {
            throw new PortalException(org.springframework.http.HttpStatus.FORBIDDEN, "Access denied");
        }
        return portalSession;
    }

    public void store(HttpSession session, PortalSession portalSession) {
        session.setAttribute(attributeName, portalSession);
    }

    public void clear(HttpSession session) {
        session.removeAttribute(attributeName);
    }

    public ModelAndView redirectToLogin(String message) {
        ModelAndView modelAndView = new ModelAndView("redirect:/login");
        modelAndView.addObject("error", message);
        return modelAndView;
    }
}

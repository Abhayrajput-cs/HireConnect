package com.hireconnect.web.controller;

import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.servlet.ModelAndView;

import com.hireconnect.web.support.PortalException;

@ControllerAdvice
public class PortalErrorAdvice {

    @ExceptionHandler(PortalException.class)
    public ModelAndView handlePortalException(PortalException ex) {
        ModelAndView modelAndView = new ModelAndView("error");
        modelAndView.addObject("status", ex.getStatusCode().value());
        modelAndView.addObject("message", ex.getMessage());
        return modelAndView;
    }

    @ExceptionHandler(Exception.class)
    public ModelAndView handleGenericException(Exception ex) {
        ModelAndView modelAndView = new ModelAndView("error");
        modelAndView.addObject("status", 500);
        modelAndView.addObject("message", ex.getMessage());
        return modelAndView;
    }
}

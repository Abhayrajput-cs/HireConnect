package com.hireconnect.web.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.web.ErrorResponse;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.servlet.ModelAndView;

import com.hireconnect.web.support.PortalException;

@ControllerAdvice
public class PortalErrorAdvice {

    @ExceptionHandler(PortalException.class)
    public ModelAndView handlePortalException(PortalException ex) {
        ModelAndView modelAndView = new ModelAndView("error");
        modelAndView.setStatus(ex.getStatusCode());
        modelAndView.addObject("status", ex.getStatusCode().value());
        modelAndView.addObject("message", ex.getMessage());
        return modelAndView;
    }

    @ExceptionHandler(Exception.class)
    public ModelAndView handleGenericException(Exception ex) {
        HttpStatusCode status = HttpStatus.INTERNAL_SERVER_ERROR;
        if (ex instanceof ErrorResponse errorResponse) {
            status = errorResponse.getStatusCode();
        }
        ModelAndView modelAndView = new ModelAndView("error");
        modelAndView.setStatus(status);
        modelAndView.addObject("status", status.value());
        modelAndView.addObject("message", ex.getMessage());
        return modelAndView;
    }
}

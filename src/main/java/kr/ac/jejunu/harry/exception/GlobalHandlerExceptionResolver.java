package kr.ac.jejunu.harry.exception;

import kr.ac.jejunu.harry.response.ResponseBuilder;
import kr.ac.jejunu.harry.response.ResponseCode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.servlet.HandlerExceptionResolver;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.mvc.multiaction.NoSuchRequestHandlingMethodException;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

/**
 * Created by jhkang on 2016-06-15.
 */
public class GlobalHandlerExceptionResolver implements HandlerExceptionResolver {
    private final static Logger logger = LoggerFactory.getLogger(GlobalHandlerExceptionResolver.class);

    @Override
    public ModelAndView resolveException(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) {
        ResponseBuilder builder = new ResponseBuilder(request);

        if(ex instanceof NoSuchRequestHandlingMethodException) {
            logger.info("NoSuchRequestHandlingMethodException : " + request.getServletPath());
            builder.setStatusCode(ResponseCode.NOT_FOUND, "Page Not Found");
        } else if(ex instanceof AuthorizationException) {
            logger.info("AuthorizationException: " + request.getServletPath());
            builder.setStatusCode(ResponseCode.UNAUTHORIZED, "Need Authorization");
        } else if(ex instanceof MissingServletRequestParameterException) {
            logger.info("MissingServletRequestParameterException: " + request.getServletPath());
            MissingServletRequestParameterException exception = (MissingServletRequestParameterException) ex;
            builder.setStatusCode(ResponseCode.BAD_REQUEST, "Please Check Parameter : " + exception.getParameterName());
        } else if(ex instanceof SessionAccountNotFoundException) {
            logger.info("SessionAccountNotFoundException: " + request.getServletPath());
            HttpSession session = request.getSession();
            if(session != null) {
                session.invalidate();
            }
            builder.setStatusCode(ResponseCode.FORBIDDEN, "Session is Not Collect!");
        } else if(ex instanceof BadRequestException) {
            logger.info("BadRequestException: " + request.getServletPath());
            BadRequestException exception = (BadRequestException) ex;
            builder.setStatusCode(ResponseCode.BAD_REQUEST, exception.getReason());
        } else {
            logger.info("default Exception : " + request.getServletPath());
            builder.setStatusCode(ResponseCode.SERVER_ERROR, ex.getMessage());
        }
        return builder.buildToModelAndView();
    }
}

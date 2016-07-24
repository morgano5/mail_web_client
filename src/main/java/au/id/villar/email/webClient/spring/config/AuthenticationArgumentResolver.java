package au.id.villar.email.webClient.spring.config;

import au.id.villar.email.webClient.tokens.UserPasswordHolder;
import org.springframework.core.MethodParameter;
import org.springframework.web.bind.support.WebDataBinderFactory;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.method.support.ModelAndViewContainer;

public class AuthenticationArgumentResolver implements HandlerMethodArgumentResolver {

    @Override
    public boolean supportsParameter(MethodParameter methodParameter) {
        return methodParameter.getParameterType() == UserPasswordHolder.class;
    }

    @Override
    public Object resolveArgument(MethodParameter methodParameter, ModelAndViewContainer modelAndViewContainer,
            NativeWebRequest nativeWebRequest, WebDataBinderFactory webDataBinderFactory) throws Exception {
        Object userPassword = nativeWebRequest.getAttribute(Constants.USER_PASSWORD_ATTR_NAME,
                NativeWebRequest.SCOPE_REQUEST);
        if(userPassword != null) modelAndViewContainer.addAttribute(userPassword);
        return userPassword;
    }

}

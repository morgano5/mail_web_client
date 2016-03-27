package au.id.villar.email.webClient.tokens;

import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurationSupport;
import org.springframework.web.servlet.handler.HandlerInterceptorAdapter;
import org.springframework.web.servlet.mvc.method.RequestMappingInfo;

import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.UnsupportedEncodingException;
import java.lang.annotation.Annotation;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public class AuthenticationHandlerInterceptor extends HandlerInterceptorAdapter {

    private static final Pattern BASIC_AUTH_VALUE_PATTERN =
            Pattern.compile("[Bb][Aa][Ss][Ii][Cc] +([0-9A-Za-z+/=.-_:]+)");

    private static final String CHARSET_NAME = "UTF-8";
    private static final String TOKEN_NAME = "X-VillarMailToken";

    private final TokenService tokenService;
    private final PermissionsResolver permissionsResolver;
    private final Class<? extends Annotation> permissionsAnnotationClass;
    private final String permissionsAttribute;
    private final WebMvcConfigurationSupport servletAppConfig;

    private Map<Method, AuthInfo> permissionsInfoMap;
    private Map<Method, AuthInfo> loginLogoutInfoMap;

    public AuthenticationHandlerInterceptor(TokenService tokenService, PermissionsResolver permissionsResolver,
            Class<? extends Annotation> permissionsAnnotationClass, String permissionsAttribute,
            WebMvcConfigurationSupport servletAppConfig) {

        this.tokenService = tokenService;
        this.permissionsResolver = permissionsResolver;
        this.permissionsAnnotationClass = permissionsAnnotationClass;
        this.permissionsAttribute = permissionsAttribute;
        this.servletAppConfig = servletAppConfig;
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler)
            throws Exception {

        if(!(handler instanceof HandlerMethod)) return true;

        if(permissionsInfoMap == null) createInfoMap();

        AuthInfo info = permissionsInfoMap.get(((HandlerMethod)handler).getMethod());

        return info == null || handlePermissions(request, response, ((AuthorizationInfo)info).roles);
    }

    @Override
    public void postHandle(HttpServletRequest request, HttpServletResponse response, Object handler,
                           ModelAndView modelAndView) throws Exception {

        if(!(handler instanceof HandlerMethod)) return;

        AuthInfo info = loginLogoutInfoMap.get(((HandlerMethod)handler).getMethod());
        if(info instanceof LoginInfo) handleLogin(response, modelAndView);
        if(info instanceof LogoutInfo) handleLogout(request, response, modelAndView);
    }

    private void createInfoMap() {

        final Map<Method, AuthInfo> permissionsInfoMap = new HashMap<>();
        final Map<Method, AuthInfo> loginLogoutInfoMap = new HashMap<>();
        Map<RequestMappingInfo, HandlerMethod> handlerMethods =
                servletAppConfig.requestMappingHandlerMapping().getHandlerMethods();

        for(HandlerMethod handlerMethod: handlerMethods.values()) {
            scanForAnnotations(handlerMethod, permissionsInfoMap, loginLogoutInfoMap);
        }

        this.permissionsInfoMap = permissionsInfoMap;
        this.loginLogoutInfoMap = loginLogoutInfoMap;

    }

    private void scanForAnnotations(HandlerMethod handlerMethod, Map<Method, AuthInfo> permissionsInfoMap,
            Map<Method, AuthInfo> loginLogoutInfoMap) {

        for(Annotation annotation: handlerMethod.getMethod().getAnnotations()) {
            if(annotation instanceof Login) {
                checkNonExistance(handlerMethod, permissionsInfoMap);
                loginLogoutInfoMap.put(handlerMethod.getMethod(), new LoginInfo());
                return;
            }
            if(annotation instanceof Logout) {
                checkNonExistance(handlerMethod, permissionsInfoMap);
                loginLogoutInfoMap.put(handlerMethod.getMethod(), new LogoutInfo());
                return;
            }
            if(permissionsAnnotationClass.isAssignableFrom(annotation.getClass())) {
                checkNonExistance(handlerMethod, permissionsInfoMap);
                permissionsInfoMap.put(handlerMethod.getMethod(), new AuthorizationInfo(getRoles(annotation)));
            }
        }
    }

    private void checkNonExistance(HandlerMethod handlerMethod, Map<Method, AuthInfo> permissionsInfoMap) {
        if(permissionsInfoMap.containsKey(handlerMethod.getMethod()))
            throw new RuntimeException("Another permission for same HandlerMethod was detected: "
                    + handlerMethod.getBeanType().getName() + "." + handlerMethod.getMethod().getName());

    }

    private Collection<String> getRoles(Annotation annotation) {

        Method attribute = null;
        for(Method method: annotation.getClass().getMethods()) {
            if(method.getName().equals(permissionsAttribute)) {
                attribute = method;
                break;
            }
        }
        if(attribute == null) {
            throw new RuntimeException("Attribute '" + permissionsAttribute + "' not found in annotation '"
                    + annotation.getClass().getName() + "'");
        }

        try {

            return Arrays.stream((Object[])attribute.invoke(annotation))
                    .map(Object::toString)
                    .collect(Collectors.toList());

        } catch (IllegalAccessException | InvocationTargetException | ClassCastException | NullPointerException e) {
            throw new RuntimeException("Value for attribute '" + permissionsAttribute
                    + "' is not the correct type or there is no access to it", e);
        }
    }

    private void handleLogin(HttpServletResponse response, ModelAndView modelAndView) {

        UserPasswordHolder userPassword = null;
        for(Object param: modelAndView.getModel().values()) {
            if(param instanceof UserPasswordHolder) {
                userPassword = (UserPasswordHolder)param;
                break;
            }
        }

        modelAndView.clear();

        Set<String> roles;
        if(userPassword == null ||
                (roles = permissionsResolver.resolve(userPassword.getUsername(), userPassword.getPassword())) == null) {
            setStatus401Unauthorized(response);
            return;
        }

        addCookie(tokenService.createToken(userPassword.getUsername(), userPassword.getPassword(), roles), response);
    }

    private void handleLogout(HttpServletRequest request, HttpServletResponse response, ModelAndView modelAndView) {
        String token = getToken(request);
        if(token != null) tokenService.removeToken(token);
        removeCookie(response);
        modelAndView.clear();
    }

    private boolean handlePermissions(HttpServletRequest request, HttpServletResponse response,
            Collection<String> roles) {

        TokenInfo tokenInfo = generateTokenInfo(request);
        if(tokenInfo == null) {
            setStatus401Unauthorized(response);
            return false;
        }

        boolean authorized = tokenInfo.containsAtLeastOne(roles);
        if(!authorized) {
            setStatus401Unauthorized(response);
            return false;
        }

        addCookie(tokenInfo, response);
        return true;
    }

    private TokenInfo generateTokenInfo(HttpServletRequest request) {

        String token = getToken(request);
        if(token != null) return tokenService.getTokenInfo(token);

        String authHeader = request.getHeader("Authorization");
        if(authHeader == null) return null;

        try {
            Matcher matcher = BASIC_AUTH_VALUE_PATTERN.matcher(authHeader);
            if(!matcher.find()) throw new RuntimeException(
                    "Unexpected header value for Authentication: " + authHeader);
            authHeader = matcher.group(1);
            authHeader = new String(Base64.getDecoder().decode(authHeader), CHARSET_NAME);
        } catch (UnsupportedEncodingException ignore) {
            throw new RuntimeException("Unknown CHARSET: " + CHARSET_NAME, ignore);
        }

        String username = authHeader.substring(0, authHeader.indexOf(':'));
        String password = authHeader.substring(authHeader.indexOf(':') + 1);
        Set<String> roles = permissionsResolver.resolve(username, password);

        return roles == null? null: tokenService.createToken(username, password, roles);
    }

    private String getToken(HttpServletRequest request) {
        Cookie[] cookies = request.getCookies();
        if(cookies != null) {
            for (Cookie cookie : cookies) {
                if (cookie.getName().equals(TOKEN_NAME)) {
                    return cookie.getValue();
                }
            }
        }
        return null;
    }

    private void setStatus401Unauthorized(HttpServletResponse response) {
        removeCookie(response);
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        response.addHeader("WWW-Authenticate", "Basic realm=\"mailClient\", charset=\"" + CHARSET_NAME + "\"");
    }

    private void addCookie(TokenInfo tokenInfo, HttpServletResponse response) {
        Cookie tokenCookie = new Cookie(TOKEN_NAME, tokenInfo.getToken());
        int maxAge = (int)((tokenInfo.getCreationTime() - System.currentTimeMillis()
                + tokenService.getExpiryTimeMillis()) / 1000);
        tokenCookie.setMaxAge(maxAge);
        tokenCookie.setHttpOnly(true);
        response.addCookie(tokenCookie);
    }

    private void removeCookie(HttpServletResponse response) {
        Cookie tokenCookie = new Cookie(TOKEN_NAME, "");
        tokenCookie.setMaxAge(0);
        tokenCookie.setHttpOnly(true);
        response.addCookie(tokenCookie);
    }

    private abstract class AuthInfo {}

    private class LoginInfo extends AuthInfo {}

    private class LogoutInfo extends AuthInfo {}

    private class AuthorizationInfo extends AuthInfo {
        private final Collection<String> roles;

        AuthorizationInfo(Collection<String> roles) {
            this.roles = roles;
        }

    }

}

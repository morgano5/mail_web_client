package au.id.villar.email.webClient.web;

import au.id.villar.email.webClient.domain.Role;
import au.id.villar.email.webClient.domain.User;
import au.id.villar.email.webClient.service.UserService;
import au.id.villar.email.webClient.spring.ServletAppConfig;
import au.id.villar.email.webClient.tokens.*;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.handler.HandlerInterceptorAdapter;
import org.springframework.web.servlet.mvc.method.RequestMappingInfo;

import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.UnsupportedEncodingException;
import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class AuthenticationHandlerInterceptor extends HandlerInterceptorAdapter {

    private static final Pattern BASIC_AUTH_VALUE_PATTERN =
            Pattern.compile("[Bb][Aa][Ss][Ii][Cc] +([0-9A-Za-z+/=.-_:]+)");

    private static final String CHARSET_NAME = "UTF-8";
    private static final String TOKEN_NAME = "X-VillarMailToken";

    private final TokenService tokenService;
    private final UserService userService;
    private final ServletAppConfig servletAppConfig;

    private Map<Method, AuthInfo> permissionsInfoMap;

    public AuthenticationHandlerInterceptor(TokenService tokenService, UserService userService,
            ServletAppConfig servletAppConfig) {

        this.tokenService = tokenService;
        this.userService = userService;
        this.servletAppConfig = servletAppConfig;
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler)
            throws Exception {

        if(!(handler instanceof HandlerMethod)) return true;

        if(permissionsInfoMap == null) createInfoMap();

        AuthInfo info = permissionsInfoMap.get(((HandlerMethod)handler).getMethod());

        if(info == null) return true;
        if(info instanceof LoginInfo) return handleLogin(request);
        if(info instanceof LogoutInfo) return handleLogout(request, response);
        return handlePermissions(request, response, ((AuthorizationInfo)info).roles);
    }

    @Override
    public void postHandle(HttpServletRequest request, HttpServletResponse response, Object handler,
                           ModelAndView modelAndView) throws Exception {

        if(!(handler instanceof HandlerMethod)) return;

        if(request.getAttribute("logout") != null) {
            modelAndView.clear();
            return;
        }

        if(request.getAttribute("login") == null) return;

        User user = (User)request.getAttribute("user");
        if(user != null) {
            String password = request.getAttribute("password").toString();
            addCookie(tokenService.createToken(user.getUsername(), password, user.getRoles()), response);
        } else {
            setStatus401Unauthorized(response);
        }

        modelAndView.clear();
    }

    private void createInfoMap() {

        final Map<Method, AuthInfo> permissionsInfoMap = new HashMap<>();

        Map<RequestMappingInfo, HandlerMethod> handlerMethods =
                servletAppConfig.requestMappingHandlerMapping().getHandlerMethods();

        for(HandlerMethod handlerMethod: handlerMethods.values()) scanForAnnotations(handlerMethod, permissionsInfoMap);

        this.permissionsInfoMap = permissionsInfoMap;

    }

    private void scanForAnnotations(HandlerMethod handlerMethod, Map<Method, AuthInfo> permissionsInfoMap) {

        for(Annotation annotation: handlerMethod.getMethod().getAnnotations()) {
            if(annotation instanceof Login) {
                checkNonExistance(handlerMethod);
                permissionsInfoMap.put(handlerMethod.getMethod(), new LoginInfo());
                return;
            }
            if(annotation instanceof Logout) {
                checkNonExistance(handlerMethod);
                permissionsInfoMap.put(handlerMethod.getMethod(), new LogoutInfo());
                return;
            }
            if(annotation instanceof Permissions) {
                checkNonExistance(handlerMethod);
                Role[] roles = ((Permissions)annotation).value();
                Collection<Role> roleCollection = roles != null? Arrays.asList(roles): Collections.emptyList();
                permissionsInfoMap.put(handlerMethod.getMethod(), new AuthorizationInfo(roleCollection));
            }
        }
    }

    private void checkNonExistance(HandlerMethod handlerMethod) {
        if(permissionsInfoMap.containsKey(handlerMethod.getMethod()))
            throw new RuntimeException("Another permission for same HandlerMethod was detected: "
                    + handlerMethod.getBeanType().getName() + "." + handlerMethod.getMethod().getName());

    }

//    private

    private boolean handleLogin(HttpServletRequest request) {
        request.setAttribute("login", "true");
        return true;
    }

    private boolean handleLogout(HttpServletRequest request, HttpServletResponse response) {
        request.setAttribute("logout", "true");
        String token = getToken(request);
        if(token != null) tokenService.removeToken(token);
        removeCookie(response);
        return true;
    }

    private boolean handlePermissions(HttpServletRequest request, HttpServletResponse response,
            Collection<Role> roles) {

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
        User user = userService.find(username, password);
        if(user == null) return null;

        return tokenService.createToken(username, password, user.getRoles());
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
        private final Collection<Role> roles;

        AuthorizationInfo(Collection<Role> roles) {
            this.roles = roles;
        }

    }

}

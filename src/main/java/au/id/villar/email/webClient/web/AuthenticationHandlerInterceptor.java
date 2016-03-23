package au.id.villar.email.webClient.web;

import au.id.villar.email.webClient.domain.Role;
import au.id.villar.email.webClient.domain.User;
import au.id.villar.email.webClient.service.UserService;
import au.id.villar.email.webClient.spring.ServletAppConfig;
import au.id.villar.email.webClient.tokens.*;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.context.annotation.ClassPathScanningCandidateComponentProvider;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.core.type.filter.AnnotationTypeFilter;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.handler.HandlerInterceptorAdapter;
import org.springframework.web.servlet.mvc.method.RequestMappingInfo;
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerMapping;

import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.UnsupportedEncodingException;
import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.util.*;
import java.util.stream.Collectors;

public class AuthenticationHandlerInterceptor extends HandlerInterceptorAdapter {

    private static final String CHARSET_NAME = "UTF-8";
    private static final String TOKEN_NAME = "X-VillarMailToken";

    private final Map<String, Map<RequestMethod, PermissionsInfo>> permissionsInfoMap;
    private final TokenService tokenService;
    private final UserService userService;

    ServletAppConfig servletAppConfig;

    public AuthenticationHandlerInterceptor(TokenService tokenService, UserService userService, Class<?> configClass, ServletAppConfig servletAppConfig) {

        this.tokenService = tokenService;
        this.userService = userService;
        this.servletAppConfig = servletAppConfig;

        final Map<String, Map<RequestMethod, PermissionsInfo>> permissionsInfoMap = new HashMap<>();

        Arrays.stream(configClass.getAnnotations())
                .filter(annotation -> annotation instanceof ComponentScan)
                .flatMap(annotation -> Arrays.stream(((ComponentScan)annotation).basePackages()))
                .forEach(packageName -> scanPackage(packageName, permissionsInfoMap));

        Map<String, Map<RequestMethod, PermissionsInfo>> immutableRequests = new HashMap<>();
        for(Map.Entry<String, Map<RequestMethod, PermissionsInfo>> entry: permissionsInfoMap.entrySet()) {
            immutableRequests.put(entry.getKey(), Collections.unmodifiableMap(entry.getValue()));
        }

        this.permissionsInfoMap = Collections.unmodifiableMap(immutableRequests);
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler)
            throws Exception {

        Map<RequestMappingInfo, HandlerMethod> m = servletAppConfig.requestMappingHandlerMapping().getHandlerMethods();

        for(Map.Entry entry: m.entrySet()) {
            System.out.println(">>> >>> >>> " + entry.getKey() + " --- " + entry.getValue());
        }









        String path = request.getPathInfo();
        RequestMethod method = RequestMethod.valueOf(request.getMethod());
        PermissionsInfo info = getPermissions(path, method);

        if(info == null) return true;
        if(info.login) return handleLogin(request);
        if(info.logout) return handleLogout(request, response, handler);
        return handlePermissions(request, response, handler, info.roles);
    }

    @Override
    public void postHandle(HttpServletRequest request, HttpServletResponse response, Object handler,
                           ModelAndView modelAndView) throws Exception {
//        if(request.getAttribute("login") != null) {
//            String username = (String)request.getAttribute("username");
//            String password = (String)request.getAttribute("password");
//            String[] permissions = (String[])request.getAttribute("permissions");
//            TokenInfo tokenInfo = tokenService.createToken(username, password, permissions);
////            Cookie tokenCookie = new Cookie(TOKEN_NAME, tokenInfo.getToken());
////            tokenCookie.setHttpOnly(true);
////            response.addCookie(tokenCookie);
////            response.addHeader(TOKEN_NAME, tokenInfo.getToken());
//            response.setStatus(404);
//        }
    }

    private boolean handleLogin(HttpServletRequest request) {
        request.setAttribute("login", "true");
        return true;
    }

    private boolean handleLogout(HttpServletRequest request, HttpServletResponse response, Object handler) {
        // TODO
        return true;
    }

    private boolean handlePermissions(HttpServletRequest request, HttpServletResponse response, Object handler,
            Role[] roles) {

        TokenInfo tokenInfo;

        String token = null;
        for(Cookie cookie: request.getCookies()) {
            if(cookie.isHttpOnly() && cookie.getName().equals(TOKEN_NAME)) {
                token = cookie.getValue();
                break;
            }
        }

        if(token == null) {
            String authHeader = request.getHeader("Authorization");
            if(authHeader == null) {
                response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                response.addHeader("WWW-Authenticate", "Basic realm=\"mailClient\", charset=\"" + CHARSET_NAME + "\"");
                return false;
            }
            try {
                authHeader = new String(Base64.getDecoder().decode(authHeader), CHARSET_NAME);
            } catch (UnsupportedEncodingException ignore) {
                throw new RuntimeException("Unknown CHARSET: " + CHARSET_NAME, ignore);
            }
            String username = authHeader.substring(0, authHeader.indexOf(':'));
            String password = authHeader.substring(authHeader.indexOf(':') + 1);
            User user = userService.find(username, password);
            if(user == null) {
                response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                response.addHeader("WWW-Authenticate", "Basic realm=\"mailClient\", charset=\"" + CHARSET_NAME + "\"");
                return false;
            }
            List<String> roleList = user.getRoles().stream().map(Role::toString).collect(Collectors.toList());
            tokenInfo = tokenService.createToken(username, password, roleList.toArray(new String[roleList.size()]));
        } else {
            tokenInfo = tokenService.getTokenInfo(token);
        }

        if(tokenInfo == null) {
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            response.addHeader("WWW-Authenticate", "Basic realm=\"mailClient\", charset=\"" + CHARSET_NAME + "\"");
            return false;
        }

        List<String> roleList = Arrays.stream(roles).map(Role::toString).collect(Collectors.toList());
        boolean authorized = tokenInfo.containsPermission(roleList.toArray(new String[roleList.size()]));

        if(!authorized) {
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            response.addHeader("WWW-Authenticate", "Basic realm=\"mailClient\", charset=\"" + CHARSET_NAME + "\"");
            return false;
        }

        return true;
    }

    private PermissionsInfo getPermissions(String path, RequestMethod method) {
        Map<RequestMethod, PermissionsInfo> methodsForPath = permissionsInfoMap.get(path);
        if(methodsForPath == null) return null;
        return methodsForPath.get(method);
    }

    private void scanPackage(String packageName, Map<String, Map<RequestMethod, PermissionsInfo>> permissionsInfoMap) {

        ClassPathScanningCandidateComponentProvider scanner = new ClassPathScanningCandidateComponentProvider(true);

        scanner.addIncludeFilter(new AnnotationTypeFilter(Login.class));
        scanner.addIncludeFilter(new AnnotationTypeFilter(Logout.class));
        scanner.addIncludeFilter(new AnnotationTypeFilter(Permissions.class));

        for(BeanDefinition bean: scanner.findCandidateComponents(packageName)) {
            scanClass(bean.getBeanClassName(), permissionsInfoMap);
        }
    }

    private void scanClass(String className, Map<String, Map<RequestMethod, PermissionsInfo>> permissionsInfoMap) {
        try {
            String pathPrefix = null;
            Class<?> type = Class.forName(className);
            for(Annotation annotation: type.getAnnotations()) {
                if(annotation instanceof Controller) {
                    pathPrefix = ((Controller)annotation).value();
                    break;
                }
            }
            for(Method method: type.getMethods()) scanMethod(method, pathPrefix, permissionsInfoMap);
        } catch (ClassNotFoundException e) {
            throw new RuntimeException(
                    "Unlikely error, if this exception is thrown something is wrong with the Spring libraries", e);
        }
    }

    private void scanMethod(Method method, String pathPrefix,
               Map<String, Map<RequestMethod, PermissionsInfo>> permissionsInfoMap) {

        boolean isLogin = false;
        boolean isLogout =false;
        Role[] roles = null;
        String[] paths = null;
        RequestMethod[] reqMethods = null;

        for(Annotation annotation: method.getAnnotations()) {
            if(annotation instanceof Login) isLogin = true;
            if(annotation instanceof Logout) isLogout = true;
            if(annotation instanceof Permissions) roles = ((Permissions)annotation).value();
            if(annotation instanceof RequestMapping) {
                paths = ((RequestMapping)annotation).value().clone();
                for(int i = 0; i < paths.length; i++) paths[i] = getPath(pathPrefix, paths[i]);
                reqMethods = ((RequestMapping)annotation).method();
            }
        }

        if((isLogin || isLogout || roles != null) && reqMethods != null) {
            if(paths == null) paths = new String[] { pathPrefix };
            for(RequestMethod reqMethod : reqMethods) for(String path: paths) {
                Map<RequestMethod, PermissionsInfo> methodsInPath = permissionsInfoMap.get(path);
                if(methodsInPath == null) {
                    methodsInPath = new HashMap<>();
                    permissionsInfoMap.put(path, methodsInPath);
                }
                if(methodsInPath.containsKey(reqMethod))
                    throw new RuntimeException("Another permission for same path and http method was detected: "
                            + method.getDeclaringClass().getName() + "." + method.getName());
                methodsInPath.put(reqMethod, new PermissionsInfo(isLogin, isLogout, roles));
            }

        }
    }

    private String getPath(String prefix, String path) {
        if(prefix == null) return path != null? path: "/";
        if(path == null) return prefix;
        if(prefix.endsWith("/")) return prefix + (path.startsWith("/")? path.substring(1): path);
        return prefix + (path.startsWith("/")? path: '/' + path);
    }

    private class PermissionsInfo {
        private final boolean login;
        private final boolean logout;
        private final Role[] roles;

        PermissionsInfo(boolean login, boolean logout, Role[] roles) {
            this.login = login;
            this.logout = logout;
            this.roles = roles;
        }

    }

}

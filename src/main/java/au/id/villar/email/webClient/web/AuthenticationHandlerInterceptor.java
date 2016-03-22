package au.id.villar.email.webClient.web;

import au.id.villar.email.webClient.domain.Role;
import au.id.villar.email.webClient.tokens.Login;
import au.id.villar.email.webClient.tokens.Logout;
import au.id.villar.email.webClient.tokens.Permissions;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.context.annotation.ClassPathScanningCandidateComponentProvider;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.core.type.filter.AnnotationTypeFilter;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.servlet.handler.HandlerInterceptorAdapter;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

public class AuthenticationHandlerInterceptor extends HandlerInterceptorAdapter {

    public void init(Class<?> configClass) {

        List<PermissionsInfo> permissionsInfoList = Arrays.stream(configClass.getAnnotations())
                .filter(annotation -> annotation instanceof ComponentScan)
                .flatMap(annotation -> Arrays.stream(((ComponentScan)annotation).basePackages()))
                .map(this::scanPackage)
                .flatMap(Collection::stream)
                .collect(Collectors.toList());

        permissionsInfoList = permissionsInfoList;
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler)
            throws Exception {
        return super.preHandle(request, response, handler);
    }

    private List<PermissionsInfo> scanPackage(String packageName) {
        ClassPathScanningCandidateComponentProvider scanner =
                new ClassPathScanningCandidateComponentProvider(true);

        scanner.addIncludeFilter(new AnnotationTypeFilter(Login.class));
        scanner.addIncludeFilter(new AnnotationTypeFilter(Logout.class));
        scanner.addIncludeFilter(new AnnotationTypeFilter(Permissions.class));

        return scanner.findCandidateComponents(packageName).stream()
                .map(beanDef -> scanClass(beanDef.getBeanClassName()))
                .flatMap(Collection::stream)
                .collect(Collectors.toList());
    }

    private List<PermissionsInfo> scanClass(String className) {
        try {
            class Ref { private String pathPrefix; }
            final Ref ref = new Ref();

            Class<?> type = Class.forName(className);
            for(Annotation annotation: type.getAnnotations()) {
                if(annotation instanceof Controller) {
                    ref.pathPrefix = ((Controller)annotation).value();
                    break;
                }
            }
            return Arrays.stream(type.getMethods()).map(method -> scanMethod(method, ref.pathPrefix))
                    .filter(p -> p != null).collect(Collectors.toList());
        } catch (ClassNotFoundException e) {
            throw new RuntimeException(
                    "Unlikely error, if this exception is thrown something is wrong with the Spring libraries", e);
        }
    }

    private PermissionsInfo scanMethod(Method method, String pathPrefix) {
        PermissionsInfo info = null;
        boolean isLogin = false;
        boolean isLogout =false;
        Role[] roles = null;
        String path = null;
        RequestMethod[] reqMethods = null;
        for(Annotation annotation: method.getAnnotations()) {
            if(annotation instanceof Login) isLogin = true;
            if(annotation instanceof Logout) isLogout = true;
            if(annotation instanceof Permissions) roles = ((Permissions)annotation).value();
            if(annotation instanceof RequestMapping) {
                path = getPath(pathPrefix, path);
                reqMethods = ((RequestMapping)annotation).method();
            }
        }
        if(isLogin || isLogout || roles != null) {
            if(path == null) path = pathPrefix;
            info = new PermissionsInfo(path, reqMethods, isLogin, isLogout, roles);
        }
        return info;
    }

    private String getPath(String prefix, String path) {
        if(prefix == null) return path != null? path: "/";
        if(path == null) return prefix;
        if(prefix.endsWith("/")) return prefix + (path.startsWith("/")? path.substring(1): path);
        return prefix + (path.startsWith("/")? path: '/' + path);
    }

    private class PermissionsInfo {
        private String path;
        private RequestMethod[] methods;
        private boolean login;
        private boolean logout;
        private Role[] roles;

        public PermissionsInfo(String path, RequestMethod[] methods, boolean login, boolean logout, Role[] roles) {
            this.path = path;
            this.methods = methods;
            this.login = login;
            this.logout = logout;
            this.roles = roles;
        }
    }

}

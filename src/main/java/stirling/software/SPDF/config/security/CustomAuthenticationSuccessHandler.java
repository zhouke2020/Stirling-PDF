package stirling.software.SPDF.config.security;

import java.io.IOException;

import org.springframework.security.core.Authentication;
import org.springframework.security.web.authentication.SavedRequestAwareAuthenticationSuccessHandler;
import org.springframework.security.web.savedrequest.SavedRequest;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import lombok.extern.slf4j.Slf4j;
import stirling.software.SPDF.utils.RequestUriUtils;

@Slf4j
public class CustomAuthenticationSuccessHandler
        extends SavedRequestAwareAuthenticationSuccessHandler {

    private LoginAttemptService loginAttemptService;
    private UserService userService;

    public CustomAuthenticationSuccessHandler(
            LoginAttemptService loginAttemptService, UserService userService) {
        this.loginAttemptService = loginAttemptService;
        this.userService = userService;
    }

    @Override
    public void onAuthenticationSuccess(
            HttpServletRequest request, HttpServletResponse response, Authentication authentication)
            throws ServletException, IOException {

        String userName = request.getParameter("username");
        if (userService.isUserDisabled(userName)) {
            getRedirectStrategy().sendRedirect(request, response, "/logout?userIsDisabled=true");
            return;
        }
        loginAttemptService.loginSucceeded(userName);

        // Get the saved request
        HttpSession session = request.getSession(false);
        SavedRequest savedRequest =
                (session != null)
                        ? (SavedRequest) session.getAttribute("SPRING_SECURITY_SAVED_REQUEST")
                        : null;

        // Check for the stored previous page URL
        String previousPageUrl = (session != null) ? (String) session.getAttribute("PREVIOUS_PAGE_URL") : null;

        if (previousPageUrl != null) {
            // Redirect to the stored previous page URL
            getRedirectStrategy().sendRedirect(request, response, previousPageUrl);
            // Remove the stored previous page URL from the session
            session.removeAttribute("PREVIOUS_PAGE_URL");
        } else if (savedRequest != null
                && !RequestUriUtils.isStaticResource(
                        request.getContextPath(), savedRequest.getRedirectUrl())) {
            // Redirect to the original destination
            super.onAuthenticationSuccess(request, response, authentication);
        } else {
            // Redirect to the root URL (considering context path)
            getRedirectStrategy().sendRedirect(request, response, "/");
        }

        // super.onAuthenticationSuccess(request, response, authentication);
    }
}

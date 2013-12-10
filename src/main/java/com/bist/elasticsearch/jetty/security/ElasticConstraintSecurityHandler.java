/*
 * Copyright 2013 Borsa Istanbul A.S.
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.bist.elasticsearch.jetty.security;

import org.eclipse.jetty.http.HttpStatus;
import org.eclipse.jetty.security.RoleInfo;
import org.eclipse.jetty.security.SecurityHandler;
import org.eclipse.jetty.security.UserDataConstraint;
import org.eclipse.jetty.server.*;
import org.eclipse.jetty.util.log.Log;
import org.eclipse.jetty.util.log.Logger;

import java.io.IOException;
import java.security.Principal;

/**
 * Created with IntelliJ IDEA.
 * User: zeldal.ozdemir
 * Date: 13.11.2013
 * Time: 20:09
 * To change this template use File | Settings | File Templates.
 */
public class ElasticConstraintSecurityHandler extends SecurityHandler {
    private static final Logger LOG = Log.getLogger(ElasticConstraintSecurityHandler.class);

    private AuthorizationConfigurationWatcher authorizationConfigurationWatcher;

    public ElasticConstraintSecurityHandler() {

/*        publicQueries.put("/kibana/img*//*", "OK");
        publicQueries.put("/kibana/css*//*", "OK");
        publicQueries.put("/kibana/css/Login.html", "OK");
        publicQueries.put("/kibana/css/LoginError.html", "OK");
        publicQueries.put("/j_security_check", "OK");
        authenticatedQueries.put("/j_logout", "OK");
        authenticatedQueries.put("/_all/_mapping", "OK");
        authenticatedQueries.put("/_nodes", "OK");
        authenticatedQueries.put("/_aliases", "OK");
        authenticatedQueries.put("/kibana*//*", "OK");
        authenticatedQueries.put("/kibana-int*//*", "OK");

        ArrayList<String> regexes = new ArrayList<>();
        regexes.add("^/local-\\d{4}\\.\\d{2}\\.\\d{2}/.*");
        userAuthenticatedQueries.put("zeldal.ozdemir", regexes);*/
    }

    @Override
    protected void doStart() throws Exception {
        super.doStart();
        authorizationConfigurationWatcher.startWatching();
    }

    @Override
    protected void doStop() throws Exception {
        super.doStop();
        authorizationConfigurationWatcher.stop();
    }

    @Override
    protected RoleInfo prepareConstraintInfo(String pathInContext, Request request) {
        RoleInfo roleInfo = new RoleInfo();
        roleInfo.setUserDataConstraint(UserDataConstraint.None);
        roleInfo.addRole(pathInContext);
        return roleInfo;
    }


    @Override
    protected boolean isAuthMandatory(Request baseRequest, Response base_response, Object constraintInfo) {

        String role = getPath((RoleInfo) constraintInfo);
        QueryConstraints queryConstraints = authorizationConfigurationWatcher.getQueryConstraints();

        return !queryConstraints.isPublic(role);
    }

    private String getPath(RoleInfo constraintInfo) {
        return (String) constraintInfo.getRoles().toArray()[0];
    }

    @Override
    protected boolean checkWebResourcePermissions(String pathInContext, Request request,
                                                  Response response, Object constraintInfo,
                                                  UserIdentity userIdentity) throws IOException {

        QueryConstraints queryConstraints = authorizationConfigurationWatcher.getQueryConstraints();

        if (queryConstraints.isPublic(pathInContext))
            return true;

        if (queryConstraints.isAuthenticatedOnly(pathInContext))
            return true;

        LOG.debug("Checking Permission for :" + pathInContext + " User:" + userIdentity.getUserPrincipal().getName());


        Principal userPrincipal = userIdentity.getUserPrincipal();

        String[] indices = parseIndices(pathInContext);

        for (String indice : indices) {
            if(indice.trim().equals(""))
                continue;
            if (!queryConstraints.checkForIndice(indice, userPrincipal)){    // if user fails in any return fail.
                LOG.warn("**** No Permission: pathInContext:" + pathInContext + " User:" + userIdentity.getUserPrincipal().getName()+" for Indice:"+indice);
                return false;
            }
        }
        return true;
    }

    private String[] parseIndices(String pathInContext) {
        // yea hell of exception but works as miracle...
        String indiceToken = pathInContext;
        if(indiceToken.startsWith("/"))
            indiceToken = indiceToken.substring(1);
        if(indiceToken.contains("/"))
            indiceToken = indiceToken.substring(0,indiceToken.indexOf("/"));
        return indiceToken.split(",");
    }


    @Override
    protected boolean checkUserDataPermissions(String pathInContext, Request request, Response response, RoleInfo roleInfo) throws IOException {
//        LOG.info("pathInContext:" + pathInContext + " RoleInfo:" + roleInfo);

        if (roleInfo == null)
            return true;

        if (roleInfo.isForbidden())
            return false;

        UserDataConstraint dataConstraint = roleInfo.getUserDataConstraint();
        if (dataConstraint == null || dataConstraint == UserDataConstraint.None)
            return true;

        HttpConfiguration httpConfig = HttpChannel.getCurrentHttpChannel().getHttpConfiguration();


        if (dataConstraint == UserDataConstraint.Confidential || dataConstraint == UserDataConstraint.Integral) {
            if (request.isSecure())
                return true;

            if (httpConfig.getSecurePort() > 0) {
                String scheme = httpConfig.getSecureScheme();
                int port = httpConfig.getSecurePort();
                String url = ("https".equalsIgnoreCase(scheme) && port == 443)
                        ? "https://" + request.getServerName() + request.getRequestURI()
                        : scheme + "://" + request.getServerName() + ":" + port + request.getRequestURI();
                if (request.getQueryString() != null)
                    url += "?" + request.getQueryString();
                response.setContentLength(0);
                response.sendRedirect(url);
            } else
                response.sendError(HttpStatus.FORBIDDEN_403, "!Secure");

            request.setHandled(true);
            return false;
        } else {
            throw new IllegalArgumentException("Invalid dataConstraint value: " + dataConstraint);
        }
    }

    public void setAuthorizationConfigurationWatcher(AuthorizationConfigurationWatcher authorizationConfigurationWatcher) {
        this.authorizationConfigurationWatcher = authorizationConfigurationWatcher;
    }
}

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

import org.eclipse.jetty.http.PathMap;
import org.eclipse.jetty.jaas.JAASRole;
import org.eclipse.jetty.jaas.JAASUserPrincipal;
import org.eclipse.jetty.security.RoleInfo;
import org.eclipse.jetty.server.UserIdentity;
import org.eclipse.jetty.util.log.Log;
import org.eclipse.jetty.util.log.Logger;

import java.security.Principal;
import java.util.*;

/**
 * Created with IntelliJ IDEA.
 * User: zeldal.ozdemir
 * Date: 20.11.2013
 * Time: 14:57
 * To change this template use File | Settings | File Templates.
 */
public class QueryConstraints {
    private static final Logger LOG = Log.getLogger(QueryConstraints.class);

    private PathMap<String> publicQueries = new PathMap<>();
    private PathMap<String> authenticatedQueries = new PathMap<>();
    private Map<String, List<String>> userAuthenticatedQueries = new HashMap<>();
    private Map<String, List<String>> groupAuthenticatedQueries = new HashMap<>();

    private Map<String, Boolean> authenticationCache = new HashMap<>(1000); // do it later

    public QueryConstraints() {

    }

    public QueryConstraints(PathMap<String> publicQueries,
                            PathMap<String> authenticatedQueries,
                            Map<String, List<String>> userAuthenticatedQueries,
                            Map<String, List<String>> groupAuthenticatedQueries) {
        this.publicQueries = publicQueries;
        this.authenticatedQueries = authenticatedQueries;
        this.userAuthenticatedQueries = userAuthenticatedQueries;
        this.groupAuthenticatedQueries = groupAuthenticatedQueries;
    }

    public boolean isPublic(String path) {
        return publicQueries.containsMatch(path);
    }

    public boolean isAuthenticatedOnly(String path) {
        return authenticatedQueries.containsMatch(path);
    }

    public boolean checkForIndice(String pathInContext, Principal principal) {

        if (checkUserHasPermission(pathInContext, principal))
            return true;

        if (checkGroupHasPermission(pathInContext, principal))
            return true;

        return false;
    }

    protected boolean checkGroupHasPermission(String pathInContext, Principal userPrincipal) {
        if (!(userPrincipal instanceof JAASUserPrincipal))
            return false;

        for (Principal principal : ((JAASUserPrincipal) userPrincipal).getSubject().getPrincipals()) {
            if (!(principal instanceof JAASRole))
                continue;
            List<String> groupAuthenticatedPaths = groupAuthenticatedQueries.get(principal.getName());
            if (groupAuthenticatedPaths == null)
                continue;
            for (String groupAuthenticatedPath : groupAuthenticatedPaths) {
                if (pathInContext.matches(groupAuthenticatedPath)) {
                    LOG.info("Permission Group_OK: pathInContext:" + pathInContext + " User:" + userPrincipal.getName());
                    return true;         // has permission
                }
            }

        }
        return false;
    }

    protected boolean checkUserHasPermission(String pathInContext, Principal userPrincipal) {
        List<String> userAuthenticatedPaths = userAuthenticatedQueries.get(userPrincipal.getName());
        if (userAuthenticatedPaths != null) {
            for (String userAuthenticatedPath : userAuthenticatedPaths) {
//            if (pathInContext.matches("^/(local|secret)-\\d{4}\\.\\d{2}\\.\\d{2}/.*"))
                if (pathInContext.matches(userAuthenticatedPath)) {
                    LOG.info("Permission User_OK: pathInContext:" + pathInContext + " User:" + userPrincipal.getName());
                    return true;
                }
            }
        }
        return false;
    }

    void addPublicQuery(String query) {
        publicQueries.put(query, "OK");
    }

    void addAuthenticatedQuery(String query) {
        authenticatedQueries.put(query, "OK");
    }

    void addUserIndiceQuery(String userName, String regex) {
        if (userAuthenticatedQueries.get(userName) == null)
            userAuthenticatedQueries.put(userName, new ArrayList<String>());
        userAuthenticatedQueries.get(userName).add(regex);
    }

    void addGroupIndiceQuery(String groupName, String regex) {
        if (groupAuthenticatedQueries.get(groupName) == null) {
            groupAuthenticatedQueries.put(groupName, new ArrayList<String>());
        }
        groupAuthenticatedQueries.get(groupName).add(regex);
    }
}

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

import org.apache.commons.configuration.HierarchicalINIConfiguration;
import org.apache.commons.configuration.SubnodeConfiguration;
import org.eclipse.jetty.http.PathMap;
import org.eclipse.jetty.util.log.Log;
import org.eclipse.jetty.util.log.Logger;

import java.net.URL;
import java.util.*;

/**
 * Created with IntelliJ IDEA.
 * User: zeldal.ozdemir
 * Date: 15.11.2013
 * Time: 11:55
 * To change this template use File | Settings | File Templates.
 */
public class AuthorizationConfigurationLoader {
    private static final Logger LOG = Log.getLogger(AuthorizationConfigurationLoader.class);



    private String authorizationMappingFile;



    public QueryConstraints load(URL url) {
        try {
            LOG.info("Loading AuthorizationMappingFile:"+url.toString());
            HierarchicalINIConfiguration iniConfiguration = new HierarchicalINIConfiguration(url);
            QueryConstraints queryConstraints = new QueryConstraints();

            return loadIni(iniConfiguration, queryConstraints);

        } catch (Exception e) {
            LOG.warn("Error while Reading File" + e.getMessage(), e);
            return null;

        }
    }

    protected QueryConstraints loadIni(HierarchicalINIConfiguration iniConfiguration, QueryConstraints queryConstraints) {
        for (String section : iniConfiguration.getSections()) {
            SubnodeConfiguration subnodeConfiguration = iniConfiguration.getSection(section);
            switch (section) {
                case "Public":
                    for (String query : subnodeConfiguration.getStringArray("Queries")) {
                        queryConstraints.addPublicQuery(query);
                    }
                    break;
                case "Authenticated":
                    for (String query : subnodeConfiguration.getStringArray("Queries")) {
                        queryConstraints.addAuthenticatedQuery(query);
                    }
                    break;
                default:
                    loadIndiceAuthZ(queryConstraints, section, subnodeConfiguration);
                    break;
            }
        }
        {   // make it atomic
            return queryConstraints;

        }
    }

    protected void loadIndiceAuthZ(QueryConstraints queryConstraints, String section, SubnodeConfiguration subnodeConfiguration) {
        Iterator<String> iterator = subnodeConfiguration.getKeys();
        while (iterator.hasNext()) {
            String key = iterator.next();
            String regex;
            if(section.startsWith("_"))    // special like _all _plugin etc
                regex = "^"+section+"$";
            else
                regex = "^" + section + "-\\d{4}\\.\\d{2}\\.\\d{2}$";
            if (key.equals("users")) {
                for (String userName : subnodeConfiguration.getString(key).split(",")) {
                    queryConstraints.addUserIndiceQuery(userName,regex);

                }
            } else {
                String groupName = subnodeConfiguration.getString(key);
                queryConstraints.addGroupIndiceQuery(groupName, regex);
            }
        }
    }

    public String getAuthorizationMappingFile() {
        return authorizationMappingFile;
    }

    public void setAuthorizationMappingFile(String authorizationMappingFile) {
        this.authorizationMappingFile = authorizationMappingFile;
    }
}

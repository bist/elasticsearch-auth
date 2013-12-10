
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

package com.bist.elasticsearch.jetty.handler;

import org.eclipse.jetty.http.HttpHeader;
import org.eclipse.jetty.server.Handler;
import org.eclipse.jetty.server.Request;
import org.eclipse.jetty.server.handler.HandlerCollection;

import javax.servlet.ServletException;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

/**
 * User: zeldal.ozdemir
 * Date: 13.11.2013
 * Time: 17:14
 * To change this template use File | Settings | File Templates.
 */
public class ElasticHandler extends HandlerCollection {
    private Handler elasticHandler;
    private Handler kibanaHandler;

    public ElasticHandler(Handler kibanaHandler, Handler elasticHandler) {
        this.kibanaHandler = kibanaHandler;
        this.elasticHandler = elasticHandler;
        addHandler(kibanaHandler);
        addHandler(elasticHandler);
    }

    @Override
    public void handle(String target, Request baseRequest, HttpServletRequest request, HttpServletResponse response) throws IOException, ServletException {

        if(isKibanaRequest(target))
            kibanaHandler.handle(target, baseRequest, request, response);
        else if( isCookiePresent(request) )
            kibanaHandler.handle(target, baseRequest, request, response);
        else
            elasticHandler.handle(target, baseRequest, request, response);

/*        else if( isBasicAuthHeaderPresent(request) )
            elasticHandler.handle(target, baseRequest, request, response);
        else
            kibanaHandler.handle(target, baseRequest, request, response);*/

    }

    private boolean isCookiePresent(HttpServletRequest request) {
        Cookie[] cookies = request.getCookies();
        return cookies != null && cookies.length > 0;
    }

    private boolean isKibanaRequest(String target) {
        return target.equals("/") || target.startsWith("/kibana/") || target.startsWith("/favicon.ico") ;
    }

    private boolean isBasicAuthHeaderPresent(HttpServletRequest request) {
        String credentials = request.getHeader(HttpHeader.AUTHORIZATION.asString());
        return  credentials != null && !credentials.trim().equalsIgnoreCase("") ;
    }

    public Handler getElasticHandler() {
        return elasticHandler;
    }

    public void setElasticHandler(Handler elasticHandler) {
        this.elasticHandler = elasticHandler;
    }

    public Handler getKibanaHandler() {
        return kibanaHandler;
    }

    public void setKibanaHandler(Handler kibanaHandler) {
        this.kibanaHandler = kibanaHandler;
    }
}

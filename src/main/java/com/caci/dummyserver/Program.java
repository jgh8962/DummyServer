/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.caci.dummyserver;

import io.undertow.*;
import io.undertow.util.*;
import io.undertow.server.*;
import java.util.ArrayList;
import java.util.List;

/**
 *
 * @author jhutchins
 */
public class Program {
    private ArrayList<EndpointDefinition> endpoints = new ArrayList<EndpointDefinition>();
    
    public Program() throws Exception {
        initializeEndpoints();
    }
    
    private void initializeEndpoints() {
        endpoints.add(new ResetDataEndpointDefinition("/api/v1/resetdata"));
        endpoints.add(new ListEndpointDefinition("/api/v1/programs", "program"));
        endpoints.add(new InstanceEndpointDefinition("/api/v1/programs/:program", "program"));
        endpoints.add(new ListEndpointDefinition("/api/v1/programs/:program/synonyms", "program_synonym"));
        endpoints.add(new InstanceEndpointDefinition("/api/v1/programs/:program/synonyms/:program_synonym", "program_synonym"));
        endpoints.add(new ListEndpointDefinition("/api/v1/synonyms", "program_synonym"));
        endpoints.add(new InstanceEndpointDefinition("/api/v1/synonyms/:program_synonym", "program_synonym"));
    }
    
    public void handleRequest(final HttpServerExchange exchange) throws Exception {
        List<Pair<String, String>> match = null;
        for (EndpointDefinition endpoint : endpoints) {
            match = endpoint.getPathMatch(exchange.getRelativePath());
            
            if (match != null) {
                endpoint.handleRequest(exchange, match);
                break;
            }
        }
        
        if (match == null)
        {
            exchange.setResponseCode(404);
            exchange.getResponseHeaders().put(Headers.CONTENT_TYPE, "text/plain");
            exchange.getResponseSender().send("Unknown endpoint.");
        }
    }
    
    public static void main(String[] args) throws Exception {
        final Program program = new Program();
        
        Undertow server = Undertow.builder()
                .addHttpListener(8080, "localhost")
                .setHandler(new HttpHandler() {
                    @Override
                    public void handleRequest(final HttpServerExchange exchange) throws Exception {
                        program.handleRequest(exchange);
                    }
                }).build();
        server.start();
        System.out.println("Dummy Server is Running.  Press Enter to exit.");
        System.in.read();
        server.stop();
    }
}

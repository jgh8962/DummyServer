/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.caci.dummyserver;

import io.undertow.server.HttpServerExchange;
import io.undertow.util.Headers;
import io.undertow.util.HttpString;
import io.undertow.util.Methods;
import io.undertow.util.StatusCodes;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Collection;

import java.util.Deque;
import java.util.Map;
import java.util.List;

/**
 *
 * @author jhutchins
 */
public class ListEndpointDefinition extends EndpointDefinition {
    public ListEndpointDefinition(String path, String table) {
        super(path);
        this.setTable(table);
    }
    
    private String table;
    public String getTable() { return table; }
    public void setTable(String table) { this.table = table; }
    
    @Override
    public void handleRequest(final HttpServerExchange exchange, List<Pair<String, String>> pathMatch) throws Exception {
        HttpString method = exchange.getRequestMethod();
        if (method.equals(Methods.GET)) {
            handleGetRequest(exchange, pathMatch);
        } else if (method.equals(Methods.POST)) {
            handlePostRequest(exchange, pathMatch);
        } else {
            exchange.setResponseCode(405);
            exchange.getResponseSender().send("Method not supported.");
        }
    }
    
    public Collection<Pair<String, Collection<String>>> getFilters(Map<String, Deque<String>> parameters) {
        ArrayList<Pair<String, Collection<String>>> result = new ArrayList<>();
        for(String key : parameters.keySet()) {
            Deque<String> deque = parameters.get(key);
            
            ArrayList<String> list = new ArrayList<>();
            while (!deque.isEmpty()) {
                list.add(deque.pop());
            }
            
            result.add(new Pair<>(key, (Collection<String>)list));
        }
        return result;
    }
    
    public String getMetaParams(Collection<String> fields, Collection<Pair<String, Collection<String>>> filters, Collection<String> sort, Integer limit, Integer offset) {
        StringBuilder result = new StringBuilder();
        
        if (fields != null && !fields.isEmpty()) {
            if (result.length() == 0) {
                result.append("\"params\": { ");
            } else {
                result.append(", ");
            }
            result.append("\"fields\":" + convertCollectionToCsvJson(fields));
        }

        if (sort != null && !sort.isEmpty()) {
            if (result.length() == 0) {
                result.append("\"params\": { ");
            } else {
                result.append(", ");
            }
            result.append("\"sort\":" + convertCollectionToCsvJson(sort));
        }
        
        if (limit != null) {
            if (result.length() == 0) {
                result.append("\"params\": { ");
            } else {
                result.append(", ");
            }
            result.append("\"limit\":" + limit.toString());
        }

        if (offset != null) {
            if (result.length() == 0) {
                result.append("\"params\": { ");
            } else {
                result.append(", ");
            }
            result.append("\"offset\":" + offset.toString());
        }
        
        if (filters != null && !filters.isEmpty()) {
            for (Pair<String, Collection<String>> filter : filters) {
                if (result.length() == 0) {
                    result.append("\"params\": { ");
                } else {
                    result.append(", ");
                }
                result.append("\"" + filter.getKey() + "\":" + convertCollectionToCsvJson(filter.getValue()));
            }
        }
        
        if (result.length() > 0) {
            result.append("}");
        }
        
        return result.length() == 0 ? null : result.toString();
    }
    
    public void handleGetRequest(final HttpServerExchange exchange, List<Pair<String, String>> pathMatch) throws Exception {
        Map<String, Deque<String>> parameters = exchange.getQueryParameters();
        
        Collection<String> fields = getCsvParameter(parameters, "fields", true);
        Collection<String> sort = getCsvParameter(parameters, "sort", true);
        Integer limit = getIntParameter(parameters, "limit", true);
        Integer offset = getIntParameter(parameters, "offset", true);
        Collection<Pair<String, Collection<String>>> filters = getFilters(parameters);
        
        MongoRepository repo = new MongoRepository("localhost");
        GetObjectsResult matches = repo.getObjects(table, pathMatch, fields, filters, sort, offset, limit);
        
        String metaParams = getMetaParams(fields, filters, sort, limit, offset);
        
        exchange.getResponseHeaders().put(Headers.CONTENT_TYPE, "application/json");
        exchange.getResponseSender().send(
                "{\n" +
                "  \"meta\": { " + (metaParams != null ? metaParams + "," : "") + "\"count\":" + matches.getResults().size() + ", \"total_count\":" + matches.getTotal().toString() + " },\n" +
                "  \"data\":" + convertJsonListToJsonArray(matches.getResults()) + " \n" +
                "}");
    }

    public void handlePostRequest(final HttpServerExchange exchange, List<Pair<String, String>> pathMatch) throws Exception {
        BufferedReader reader = null;
        StringBuilder builder = new StringBuilder( );

        try {
            exchange.startBlocking( );
            reader = new BufferedReader( new InputStreamReader( exchange.getInputStream( ) ) );

            String line;
            while( ( line = reader.readLine( ) ) != null ) {
                builder.append( line );
            }
        } catch( IOException e ) {
            e.printStackTrace( );
        } finally {
            if( reader != null ) {
                try {
                    reader.close( );
                } catch( IOException e ) {
                    e.printStackTrace( );
                }
            }
        }

        String json = builder.toString( );
        
        MongoRepository repo = new MongoRepository("localhost");
        String id = repo.insertObject(table, pathMatch, json);
        exchange.setResponseCode(StatusCodes.CREATED);
        exchange.getResponseHeaders().add(Headers.LOCATION, exchange.getRequestURL() + "/" + id);
        exchange.endExchange();
    }
}

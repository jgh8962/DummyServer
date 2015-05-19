/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.caci.dummyserver;

import com.mongodb.DBObject;
import com.mongodb.util.JSON;
import io.undertow.server.HttpServerExchange;
import io.undertow.util.Headers;
import io.undertow.util.HttpString;
import io.undertow.util.Methods;
import io.undertow.util.StatusCodes;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.Collection;
import java.util.List;


/**
 *
 * @author jhutchins
 */
public class InstanceEndpointDefinition extends EndpointDefinition {
    public InstanceEndpointDefinition(String path, String table) {
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
        } else if (method.equals(Methods.PUT)) {
            handlePutRequest(exchange, pathMatch);
        } else if (method.equals(Methods.DELETE)) {
            handleDeleteRequest(exchange, pathMatch);
        } else {
            exchange.setResponseCode(405);
            exchange.getResponseSender().send("Method not supported.");
        }
    }
    
    private void handleGetRequest(final HttpServerExchange exchange, List<Pair<String, String>> pathMatch) throws Exception {
        MongoRepository repo = new MongoRepository("localhost");
        
        Collection<String> fields = getCsvParameter(exchange.getQueryParameters(), "fields", Boolean.TRUE);
        String json = repo.getObject(table, pathMatch, fields);
        
        if (null == json || json.equals("")) {
            exchange.setResponseCode(404);
            exchange.getResponseSender().send("404:  No soup for you!  ");
        } else {
            exchange.getResponseHeaders().put(Headers.CONTENT_TYPE, "application/json");
            exchange.getResponseSender().send(json);
        }
    }
    
    private Boolean doIdsMatch(List<Pair<String, String>> keys, String json, String table) {
        DBObject obj = json != null && !json.equals("") ? (DBObject)JSON.parse(json) : null;
        
        if (obj == null) {
            return false;
        }
        
        Collection<String> jsonFields = obj.keySet();
        for (Pair<String, String> key : keys) {
            String keyName = key.getKey();
            String jsonField = keyName.equals(":" + table) ? "id" : keyName.substring(1);
            
            if (!jsonFields.contains(jsonField) || !key.getValue().equals(obj.get(jsonField).toString())) {
                return false;
            }
            break;
        }
        
        return true;
    }
    
    private void handlePutRequest(final HttpServerExchange exchange, List<Pair<String, String>> pathMatch) throws Exception {
        BufferedReader reader = null;
        StringBuilder builder = new StringBuilder( );

        try {
            exchange.startBlocking();
            InputStream inputStream = exchange.getInputStream();
            reader = new BufferedReader(new InputStreamReader(inputStream));

            int size = inputStream.available();
            while (size > 0) {
                char[] cbuf = new char[inputStream.available()];
                size -= reader.read(cbuf, 0, inputStream.available());
                builder.append(cbuf);
            }
        } catch(IOException e) {
            e.printStackTrace();
        } finally {
            if(reader != null) {
                try {
                    reader.close();
                } catch(IOException e) {
                    e.printStackTrace();
                }
            }
        }

        String json = builder.toString( );
        
        if (doIdsMatch(pathMatch, json, table)) {
            MongoRepository repo = new MongoRepository("localhost");
            repo.updateObject(table, pathMatch, json);
            exchange.setResponseCode(StatusCodes.OK);
            exchange.endExchange();
        } else {
            exchange.setResponseCode(StatusCodes.BAD_REQUEST);
            exchange.getResponseSender().send("ID mismatch between URL and json data");
        }
    }

    private void handleDeleteRequest(final HttpServerExchange exchange, List<Pair<String, String>> pathMatch) throws Exception {
        MongoRepository repo = new MongoRepository("localhost");
        
        // todo: add fields
        repo.deleteObject(table, pathMatch);
        exchange.setResponseCode(StatusCodes.OK);
    }
}
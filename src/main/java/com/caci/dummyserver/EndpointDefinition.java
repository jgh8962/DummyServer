/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.caci.dummyserver;

import io.undertow.server.HttpServerExchange;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Deque;
import java.util.List;
import java.util.Map;

/**
 *
 * @author jhutchins
 */
public abstract class EndpointDefinition {
    public EndpointDefinition(String path) {
        setPath(path);
    }
    
    private String path;
    private String[] pathSplit;
    public String getPath() { return path; }
    public void setPath(String path) { 
        this.path = path; 
        this.pathSplit = path.split("/");
    }
    
    public List<Pair<String, String>> getPathMatch(String relativePath) {
        String[] relSplit = relativePath.split("/");
        
        ArrayList<Pair<String, String>> parameters = new ArrayList<Pair<String, String>>();
        
        if (relSplit.length != pathSplit.length) {
            return null;
        }
        
        for (int i = 0; i < pathSplit.length; ++i) {
            if (pathSplit[i].startsWith(":"))
            {
                try {
                    Integer.parseUnsignedInt(relSplit[i].toLowerCase(), 16);
                    parameters.add(new Pair<String, String>(pathSplit[i], relSplit[i]));
                }
                catch (Exception ex) { return null; }
            } else if (!relSplit[i].equals(pathSplit[i])) {
                return null;
            }
        }
        
        return parameters;
    }
    
    protected Integer getIntParameter(Map<String, Deque<String>> parameters, String parameterName, Boolean remove) {
        if (parameters.containsKey(parameterName)) {
            Deque<String> deque = parameters.get(parameterName);
            
            if (remove) {
                parameters.remove(parameterName);
            }

            if (!deque.isEmpty()) {
                Integer val = Integer.parseInt(deque.pop());
                
                // ensure that there is only one.
                if (deque.isEmpty()) {
                    return val;
                }
            }
        }
        
        return null;
    }
    
    protected Boolean getBoolParameter(Map<String, Deque<String>> parameters, String parameterName, Boolean remove) {
        if (parameters.containsKey(parameterName)) {
            Deque<String> deque = parameters.get(parameterName);
            
            if (remove) {
                parameters.remove(parameterName);
            }

            if (!deque.isEmpty()) {
                Boolean val = deque.pop().toLowerCase().equals("true");
                
                // ensure that there is only one.
                if (deque.isEmpty()) {
                    return val;
                }
            }
        }
        
        return null;
    }
    
    protected Deque<String> getDequeParameter(Map<String, Deque<String>> parameters, String parameterName, Boolean remove) {
        if (parameters.containsKey(parameterName)) {
            Deque<String> result = parameters.get(parameterName);
            
            if (remove) {
                parameters.remove(parameterName);
            }
            
            return result;
        }
        
        return null;
    }
    
    protected Collection<String> getCsvParameter(Map<String, Deque<String>>parameters, String parameterName, Boolean remove) {
        ArrayList<String> result = new ArrayList<>();
        
        Deque<String> param = getDequeParameter(parameters, parameterName, remove);
        while (param != null && !param.isEmpty()) {
            String item = param.pop();
            
            for (String v : item.split(",")) {
                result.add(v);
            }
        }
        
        return result;
    }
    
    protected String convertCollectionToCsv(Collection<String> collection) {
        if (null == collection) {
            return null;
        }
        
        StringBuilder builder = new StringBuilder();
        for (String item : collection) {
            if (builder.length() != 0) {
                builder.append(",");
            }
            builder.append(item);
        }
        return builder.toString();
    }
    
    protected String convertCollectionToCsvJson(Collection<String> collection) {
        String result = convertCollectionToCsv(collection);
        
        if (null == result) {
            result = "null";
        } else {
            result = "\"" + result + "\"";
        }
        
        return result;
    }
    
    protected String convertJsonListToJsonArray(List<String> list) {
        StringBuilder builder = new StringBuilder();
        for(String item : list) {
            if (builder.length() > 0) {
                builder.append(",");
            }
            builder.append(item);
        }
        
        return builder.length() == 0 ? "null" : "[" + builder.toString() + "]";
    }
    
    public abstract void handleRequest(final HttpServerExchange exchange, List<Pair<String, String>> pathMatch) throws Exception;
}

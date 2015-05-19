/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.caci.dummyserver;

import java.util.ArrayList;
import java.util.List;

/**
 *
 * @author jhutchins
 */
public class GetObjectsResult {
    public GetObjectsResult() {
        this.results = new ArrayList<>();
    }
    
    public GetObjectsResult(List<String> results, Integer total) {
        this.results = results;
        this.total = total;
    }
    
    List<String> results;
    public List<String> getResults() { return results; }
    public void setResults(List<String> results) { this.results = results; }
    
    Integer total;
    public Integer getTotal() { return total; }
    public void setTotal(Integer total) { this.total = total; }
}

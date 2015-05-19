/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.caci.dummyserver;

import io.undertow.server.HttpServerExchange;
import java.util.ArrayList;
import java.util.List;

/**
 *
 * @author jhutchins
 */
public class ResetDataEndpointDefinition extends EndpointDefinition {
    public ResetDataEndpointDefinition(String path) {
        super(path);
    }

    @Override
    public void handleRequest(HttpServerExchange exchange, List<Pair<String, String>> pathMatch) throws Exception {
        MongoRepository repo = new MongoRepository("localhost");

        repo.clear("program");
        String h1Id = repo.insertObject("program", null, "{'name':'H-1','pno':'101'}");
        String v22Id = repo.insertObject("program", null, "{'name':'V-22','pno':'212'}");
        repo.insertObject("program", null, "{'name':'Navstar GPS','pno':'166'}");

        ArrayList<Pair<String, String>> keys = new ArrayList<>();

        repo.clear("program_synonym");
        keys.add(new Pair<>(":program", h1Id));
        repo.insertObject("program_synonym", keys, "{'program':'" + h1Id + "','name':'H-2 Upgrades'}");
        keys.clear();
        keys.add(new Pair<>(":program", v22Id));
        repo.insertObject("program_synonym", keys, "{'program':'" + v22Id + "','name':'V-23'}");
    }
}

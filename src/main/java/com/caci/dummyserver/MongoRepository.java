/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.caci.dummyserver;

import com.mongodb.BasicDBList;
import com.mongodb.MongoClient;
import com.mongodb.DB;
import com.mongodb.DBObject;
import com.mongodb.BasicDBObject;
import com.mongodb.DBCollection;
import com.mongodb.DBCursor;
import com.mongodb.util.JSON;
import java.util.ArrayList;
import java.util.Collection;

import java.util.List;
import java.util.Random;

/**
 *
 * @author jhutchins
 */
public class MongoRepository {
    public MongoRepository(String serverName) throws Exception {
        if (mongo == null) {
            mongo = new MongoClient(serverName);
        }
    }
    
    private static MongoClient mongo = null;
    private static Random random = new Random();
        
    private DBObject makeQueryObject(Collection<Pair<String, String>>keys, Collection<Pair<String, Collection<String>>> filters) {
        DBObject result = new BasicDBObject();

        if (keys != null) {
            for (Pair<String, String> key : keys) {
                result.put("key." + key.getKey(), key.getValue());
            }
        }
        
        if (filters != null) {
            for (Pair<String, Collection<String>> filter : filters) {
                int valueCount = filter.getValue().size();
                if (valueCount == 1) {
                    String filterValue = null;
                    for (String v : filter.getValue()) {
                        filterValue = v;
                    }
                    result.put("data." + filter.getKey(), filterValue);
                } else if (valueCount > 1) {
                    BasicDBList dbList = new BasicDBList();
                    dbList.addAll(filter.getValue());

                    BasicDBObject dbObj = new BasicDBObject();
                    dbObj.put("$in", dbList);
                    result.put("data." + filter.getKey(), dbObj);
                }
            }
        }
        
        return result;
    }
    
    private DBObject makeDBObject(Collection<Pair<String, String>> keys, String json) {
        DBObject result = new BasicDBObject();

        if (keys != null) {
            DBObject keyObject = new BasicDBObject();
            for (Pair<String, String> key : keys) {
                keyObject.put(key.getKey(), key.getValue());
            }
            result.put("key", keyObject);
        }
        
        if (json != null && !json.equals("")) {
            result.put("data", (DBObject)JSON.parse(json));
        }
        
        return result;
    }
    
    private DBObject getData(DBObject obj) {
        return obj != null && obj.containsField("data") ? (DBObject)((DBObject)obj).get("data") : null;
    }
    
    private DBObject pruneDBObject(DBObject obj, Collection<String> fields) {
        if (fields != null && fields.size() > 0) {
            ArrayList<String> fieldList = new ArrayList<String>(obj.keySet());
            for (String field : fieldList) {
                if (!fields.contains(field)) {
                    obj.removeField(field);
                }
            }
        }
        return obj;
    }
    
    public GetObjectsResult getObjects(String table, Collection<Pair<String, String>> keys, Collection<String> fields, Collection<Pair<String, Collection<String>>> filters, Collection<String> sort, Integer offset, Integer limit) {
        DB db = mongo.getDB("Objects");
        DBCollection col = db.getCollection(table);

        GetObjectsResult result = new GetObjectsResult();

        DBObject queryObject = makeQueryObject(keys, filters);
        DBCursor cursor = col.find(queryObject);
        try
        {
            if (sort != null && !sort.isEmpty()) {
                DBObject sortObj = new BasicDBObject();
                for (String fieldName : sort) {
                    if (fieldName.startsWith("-")) {
                        sortObj.put("data." + fieldName.substring(1), -1);
                    } else {
                        sortObj.put("data." + fieldName, 1);
                    }
                }
                cursor.sort(sortObj);
            }
            
            result.setTotal(cursor.count());

            if (offset != null && offset > 0) {
            	cursor.skip(offset);
            }
            if (limit != null && limit > 0) {
            	cursor.limit(limit);
            }

            while (cursor.hasNext()) {
                DBObject obj = cursor.next();
                obj = pruneDBObject(getData(obj), fields);
                result.getResults().add(obj.toString());
            }
        } finally {
            cursor.close();
        }

        return result;
    }
    
    public String getObject(String table, Collection<Pair<String, String>> keys, Collection<String> fields) throws Exception {
        GetObjectsResult results = getObjects(table, keys, fields, null, null, null, null);
        List<String> resultList = results.getResults();
        
        String result = null;
        if (resultList.size() == 1) {
            result = resultList.get(0);
        } else if (resultList.size() > 1) {
            throw new Exception("Error: Multiple objects found.");
        }
        
        return result;
    }
    
    public String getObject(String table, String id, Collection<String> fields) throws Exception {
        ArrayList<Pair<String, String>> keys = new ArrayList<>();
        keys.add(new Pair<>(":" + table, id));
        return getObject(table, keys, fields);
    }
    
    public void updateObject(String table, Collection<Pair<String, String>> keys, String json) {
        DB db = mongo.getDB("Objects");
        DBCollection col = db.getCollection(table);
        
        DBObject queryObject = makeQueryObject(keys, null);
        DBObject updateObject = new BasicDBObject();
        updateObject.put("$set", makeDBObject(keys, json));
        
        col.update(queryObject, updateObject);
    }
    
    private String generateDAVEID() {
        String result = Integer.toHexString(random.nextInt()).toUpperCase();
        while (result.length() < 8) {
            result = "0" + result;
        }
        return result;
    }
    
    /**
     * Insert a new object into the specified collection.
     * @param table The name of the Mongo collection to insert into.
     * @param keys The list of key/value pairs.  These are the path parameters.
     * @param json The json for the document to insert.
     * @return Returns the DAVE ID of the object.
     */
    public String insertObject(String table, Collection<Pair<String, String>> keys, String json) throws Exception {
        DB db = mongo.getDB("Objects");
        DBCollection col = db.getCollection(table);
        
        String daveId = generateDAVEID();
        while (getObject(table, daveId, null) != null) {
            daveId = generateDAVEID();
        }
        
        DBObject data = (DBObject)JSON.parse(json);
        data.put("id", daveId);
        json = data.toString();
        if (keys == null) {
            keys = new ArrayList<>();
        }
        keys.add(new Pair<>(":" + table, daveId));
        
        DBObject obj = makeDBObject(keys, json);
        col.insert(obj);
        
        return daveId;
    }
    
    public void deleteObject(String table, Collection<Pair<String, String>> keys) {
        DB db = mongo.getDB("Objects");
        DBCollection col = db.getCollection(table);
        DBObject queryObject = makeQueryObject(keys, null);
        col.remove(queryObject);
    }
    
    public void clear(String table) {
        DB db = mongo.getDB("Objects");
        DBCollection col = db.getCollection(table);
        col.drop();
    }
}

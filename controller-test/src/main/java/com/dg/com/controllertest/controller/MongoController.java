package com.dg.com.controllertest.controller;

import com.mongodb.DB;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Set;

/**
 * Created by jkong on 8/2/18.
 */

@RestController
public class MongoController {

    @Autowired
    MongoTemplate mongoTemplate;

    @RequestMapping("/mongo")
    public String mongo(){
        DB db = mongoTemplate.getDb();

        return db.getName();
    }
    @RequestMapping("/speed")
    public String speed(@RequestParam String time,
                        @RequestParam String log ){
        mongoTemplate.insert(new logData(time, log));

        return null;
    }
    @RequestMapping("/speedlog")
    public String speedlog(){
        Set<String> result  = mongoTemplate.getCollectionNames();
        return result.toString();
    }

    private class logData{
        String timeStamp;
        String value;
        logData(String time, String value){
            this.timeStamp = time;
            this.value = value;
        }
    }
}
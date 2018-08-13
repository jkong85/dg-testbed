package com.dg.com.controllercore.Controllers;

import com.dg.com.controllercore.ControllerCoreApplication;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
public class LogController {
    private static final Logger logger = LogManager.getLogger(LogController.class);
    private static Map<String, List<String>> DGCurLogMap = new HashMap<>();
    private static Map<String, List<String>> DGHistoryLogMap = new HashMap<>();

    public LogController(){
        DGCurLogMap = new HashMap<>();
        DGHistoryLogMap = new HashMap<>();
    }

    @RequestMapping(value = "/logwrite")
    public String logWrite(@RequestParam String sender,
                           @RequestParam String log){
        logger.debug("Write log of " + sender);
        return writeLog(sender, log);
    }
    @RequestMapping(value = "/bklogwrite")
    public String bklogWrite(@RequestParam String bksender,
                           @RequestParam String log){
        String sender = getRealSender(bksender);
        logger.debug("Write log of " + sender);
        return writeLog(sender, log);
    }

    @RequestMapping(value = "/bklogclean")
    public String bklogClean(@RequestParam String bksender){
        String sender = getRealSender(bksender);
        logger.debug("Clean log of " + sender);
        return cleanLog(sender);
    }
    @RequestMapping(value = "/logclean")
    public String logClean(@RequestParam String sender){
        logger.debug("Clean log of " + sender);
        return cleanLog(sender);
    }

    @RequestMapping(value = "/log")
    public String log(@RequestParam String sender){
        logger.debug("Get log of " + sender);
        return getLog(sender);
    }
    @RequestMapping(value = "/bklog")
    public String bklog(@RequestParam String bksender){
        String sender = getRealSender(bksender);
        logger.debug("Get log of " + sender);
        return getLog(sender);
    }

    @RequestMapping(value = "/loghistory")
    public String logHistory(@RequestParam String sender) {
        logger.debug("Get log history of " + sender);
        return getLogHistory(sender);
    }
    @RequestMapping(value = "/bkloghistory")
    public String bkLogHistory(@RequestParam String bksender) {
        String sender = getRealSender(bksender);
        logger.debug("Get log history of " + sender);
        return getLogHistory(sender);
    }

    private static String getRealSender(String bksender){
        logger.debug("Get bksender: " + bksender + " sender name. " );
        if(ControllerCoreApplication.bkServiceNameMap==null || !ControllerCoreApplication.bkServiceNameMap.containsKey(bksender) ){
            String sender = ControllerCoreApplication.bkServiceNameMap.get(bksender).imoName;
            logger.debug("Get bksender: " + bksender + " sender name is:  " + sender);
            return sender;
        }
        return null;
    }

    public static String writeLog(String sender, String log){
        if(sender == null){
            logger.warn("Failed to write log from " + sender);
            return "Failed to wirte log from " + sender;
        }
        if(!DGCurLogMap.containsKey(sender)){
            List<String> logList = new ArrayList<>();
            logList.add("Start to show log <br/>");
            DGCurLogMap.put(sender, logList);
        }

        if(!DGHistoryLogMap.containsKey(sender)){
            List<String> logList = new ArrayList<>();
            logList.add("Start to show log <br/>");
            DGHistoryLogMap.put(sender, logList);
        }

        DGCurLogMap.get(sender).add(0, log);
        DGHistoryLogMap.get(sender).add(0, log);

        return "write log successfully!";
    }
    private String cleanLog(String sender){
        if(sender == null){
            logger.warn("Failed to find log from " + sender);
            return "Failed to find log from " + sender;
        }
        if(DGCurLogMap!=null && DGCurLogMap.containsKey(sender)){
            DGCurLogMap.remove(sender);
        }
        if(DGHistoryLogMap!=null && DGHistoryLogMap.containsKey(sender)){
            DGHistoryLogMap.remove(sender);
        }
        return "clean log successfully!";
    }
     public String getLog(String sender){
        if(sender == null){
            logger.warn("Failed to find log from " + sender);
            return "Failed to find log from " + sender;
        }
        if(! DGCurLogMap.containsKey(sender) && DGHistoryLogMap.containsKey(sender)){
            return "No log for " + sender + ", it is deleted!";
        }else if(! DGCurLogMap.containsKey(sender) && ! DGHistoryLogMap.containsKey(sender)){
            return "No log for " + sender + ", it is not ready yet!";
        }
        if(DGCurLogMap.get(sender).isEmpty()) {
            return "No log";
        }
        return DGCurLogMap.get(sender).get(0);
    }
    public String getLogHistory(String sender) {
        if(sender == null){
            logger.warn("Failed to find log from " + sender);
            return "Failed to find log from " + sender;
        }
        if(! DGHistoryLogMap.containsKey(sender)){
            return "No log for " + sender + ", it is not ready yet!";
        }
        if( DGHistoryLogMap.get(sender).isEmpty()) {
            return "No log";
        }
        StringBuilder sb = new StringBuilder();
        for(String str : DGHistoryLogMap.get(sender)){
            sb.append(" From " + sender + " : " + str);
            sb.append("<br/>");
        }
        return sb.toString();
    }

}

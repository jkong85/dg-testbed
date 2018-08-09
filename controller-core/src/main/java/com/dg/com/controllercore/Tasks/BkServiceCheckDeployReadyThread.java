package com.dg.com.controllercore.Tasks;

import com.dg.com.controllercore.ControllerCoreApplication;
import com.dg.com.controllercore.IMOs.BackupService;
import com.dg.kj.dgcommons.Http;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClientException;

import java.util.HashSet;
import java.util.Set;

//Check whether the new created BkService is ready
// By send API request to each deployment's API : /ready
public class BkServiceCheckDeployReadyThread implements Runnable{
    private static final Logger logger = LogManager.getLogger(BkServiceCheckDeployReadyThread.class);

    private static final Integer PORT_EUREKA_CHECK_SERVICE = 30003;
    private static final Integer PORT_ZUUL_CHECK_SERVICE = 30004;
    private Thread t;

    public BkServiceCheckDeployReadyThread() {
    }
    // Keep that there are at least BACK_LIMIT available BackupService for each type on each node
    public void run() {
        try { Thread.sleep(120000); } catch (InterruptedException ie) { }
        logger.info("Running BkServiceCheckAvailNumberThread to guarantee that a certain number of BackupServices are available!");
        while(true) {
            for(String node : ControllerCoreApplication.NODE_LIST){
                for(String type : ControllerCoreApplication.IMO_TYPE){
                    Integer cnt = 5;
                    while(cnt-- > 0) {
                        logger.info(" wait for " + cnt + " seconds");
                        try { Thread.sleep(1000); } catch (InterruptedException ie) { }
                    }
                    logger.debug("check current bkservice for node : " + node + ", type : " + type);
                    String nodetype = node + "+" + type;
                    if(ControllerCoreApplication.bkServiceNotReadyPoolMap.get(nodetype).isEmpty()) {
                        continue;
                    }
                    //The new not ready bkservice is put at the end of list, so here we check the head of the list firstly
                    BackupService backupService = ControllerCoreApplication.bkServiceNotReadyPoolMap.get(nodetype).get(0);
                    if(isReady(backupService)){
                        logger.debug("The new BackupService : " + backupService.toString() + " is ready now!");
                        logger.debug("Before moving the new bkService,  bkServiceNotReadyPoolMap of " + nodetype + " is: " + ControllerCoreApplication.bkServiceNotReadyPoolMap.get(nodetype).toString());
                        logger.debug("Before moving the new bkService,  bkServiceReadyPoolMap of " + nodetype + " is: " + ControllerCoreApplication.bkServiceReadyPoolMap.get(nodetype).toString());
                        //TODO: lock here?
                        ControllerCoreApplication.bkServiceNotReadyPoolMap.get(nodetype).remove(0);
                        backupService.status = ControllerCoreApplication.BK_SERVICE_STATUS_AVAILABLE;
                        ControllerCoreApplication.bkServiceReadyPoolMap.get(nodetype).add(backupService);
                        logger.debug("After moving the new bkService,  bkServiceNotReadyPoolMap of " + nodetype + " is: " + ControllerCoreApplication.bkServiceNotReadyPoolMap.get(nodetype).toString());
                        logger.debug("After moving the new bkService,  bkServiceReadyPoolMap of " + nodetype + " is: " + ControllerCoreApplication.bkServiceReadyPoolMap.get(nodetype).toString());
                    }else{
                        logger.debug("The new BackupService : " + backupService.toString() + " is not ready yet!");
                        //TODO: lock here?
                        //move it to the end
                        BackupService tmpBkService = ControllerCoreApplication.bkServiceNotReadyPoolMap.get(nodetype).remove(0);
                        ControllerCoreApplication.bkServiceNotReadyPoolMap.get(nodetype).add(tmpBkService);
                    }
                }
            }
        }
    }
    private boolean isReady(BackupService backupService){
        logger.info("Check the BackupService ready: " + backupService.toString());
        String k8sServiceName = "ready-test-service";
        //Integer node_port_eureka = ControllerCoreApplication.nodePortsPool.pop();
        //Integer node_port_zuul = node_port_eureka + 1;
        Integer node_port_eureka = PORT_EUREKA_CHECK_SERVICE;
        Integer node_port_zuul = PORT_ZUUL_CHECK_SERVICE;
        ApiServerCmd apiServerCmd = new ApiServerCmd();
        try {
            logger.info("try to delete the test service if existed!");
            apiServerCmd.deleteService(k8sServiceName, node_port_eureka);
        }catch (HttpClientErrorException e){
            logger.warn("Test service is not existed, ignore it!");
        }
        apiServerCmd.CreateService(k8sServiceName, backupService.selector, node_port_eureka.toString(), node_port_zuul.toString());
        // wait some time
        try { Thread.sleep(15000); } catch (InterruptedException ie) { }

        String nodeIP = ControllerCoreApplication.nodeIpMap.get(backupService.node);
        logger.info("curr node is: " + backupService.node + " it is ip is: " + nodeIP );

        String[] urlList = new String[backupService.deploymentsList.size()-2];
        String ipPrefix = "http://" + nodeIP + ":" + node_port_zuul + "/";
        String ipPostfix = "/ready";
        Set<String> basicDeploySet = new HashSet<>();
        basicDeploySet.add("eureka");
        basicDeploySet.add("zuul");
        for(int i=0,j=0; i<urlList.length; i++){
            String curDeploy = backupService.deploymentsList.get(i).serviceType;
            if(!basicDeploySet.contains(curDeploy)) {
                urlList[j++] = ipPrefix + curDeploy + ipPostfix;
            }
        }

        if(isAllDeploymentReady(urlList)){
            logger.info("The backup service is ready, delete the k8sService test service : " + k8sServiceName);
            apiServerCmd.deleteService(k8sServiceName, node_port_eureka);
            return true;
        }
        logger.info("The backup service is Not ready, delete the k8sService test service : " + k8sServiceName);
        String res = apiServerCmd.deleteService(k8sServiceName, node_port_eureka);
        logger.info(res);
        try { Thread.sleep(5000); } catch (InterruptedException ie) { }
        return false;
    }

    private static boolean isAllDeploymentReady(String[] urlList){
        logger.info("URLs of ready checking are: " + urlList.toString());
        for(String url : urlList){
            logger.info("curr URL of ready checking is : " + url );
            boolean flag = false;
            int i = 5;
            while(i-- > 0){
                try {
                    Http.httpGet(url);
                    logger.info("Deployment is ready of URL: " + url);
                    flag = true;
                    break;
                } catch (RestClientException re) {
                    logger.info("Deployment is Not ready! ");
                }
            }
            if(!flag){
                return false;
            }
        }
        logger.info("All deployment is ready! ");
        return true;
    }

    public void start () {
        if (t == null) {
            t = new Thread (this);
            t.start ();
        }
    }
}

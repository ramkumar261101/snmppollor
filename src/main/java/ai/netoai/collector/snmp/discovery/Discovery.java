/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package ai.netoai.collector.snmp.discovery;

import ai.netoai.collector.model.DiscoveryTask;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public abstract class Discovery {
    
    private static final int DISCOVERY_THREADS = 100;
    protected static ExecutorService discoveryTaskExecutors;
    protected ExecutorService nodeDiscoveryExecutors;
    
    protected DiscoveryTask dt;
    
    public Discovery(DiscoveryTask dt) {
        this.dt = dt;
        if ( discoveryTaskExecutors == null ) {
            discoveryTaskExecutors = Executors.newFixedThreadPool(DISCOVERY_THREADS);
        }
        
        this.nodeDiscoveryExecutors = Executors.newFixedThreadPool(DISCOVERY_THREADS);
    }
    
    public abstract void start();
    
}

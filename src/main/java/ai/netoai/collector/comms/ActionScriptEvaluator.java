/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package ai.netoai.collector.comms;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ActionScriptEvaluator {

    private static final Logger log = LoggerFactory.getLogger(ActionScriptEvaluator.class);
    private static final String tracePrefix = "[" + ActionScriptEvaluator.class.getSimpleName() + "]: ";

    private static final String DEFAULT_SCRIPT_ENGINE = "nashorn";
    private static ActionScriptEvaluator instance;

    private ScriptEngine engine;
    private Map<String,Object> resultObj;
    public Map getResultObj(){
    return resultObj;
}
    private ActionScriptEvaluator() {
        ScriptEngineManager manager = new ScriptEngineManager();
        engine = manager.getEngineByName(DEFAULT_SCRIPT_ENGINE);
        System.out.println("Script engine initialized: " + engine);
    }

    public synchronized static ActionScriptEvaluator getInstance() {
        if (instance == null) {
            instance = new ActionScriptEvaluator();
        }
        return instance;
    }

    public Object evaluateScript(String script) throws javax.script.ScriptException {
        return evaluateScript(script, null);
    }

    public Object evaluateScript(String script, Map<String, Object> ctx) throws javax.script.ScriptException {
      return evaluateScript(script,ctx,false);
    }
    public Object evaluateScript(String script, Map<String, Object> ctx,boolean compile) throws javax.script.ScriptException {
        log.info(tracePrefix + "Executing the action script: "+script);
        if ( ctx == null ) {
            ctx = new HashMap<>();
        }
        ctx.put("compile", compile);
        if (script != null) {
            ctx.put("scriptName", script);
        }
        Object val = engine.eval(script);
    
        log.info(tracePrefix + "Done executing the script: " + script);
        return val;
    }

    public static String readTheScript(String path) {
        List<String> lines = new ArrayList<>();
        BufferedReader reader = null;
        try {
            reader = new BufferedReader(new FileReader(path));
            String line = reader.readLine();
            while (line != null) {
                lines.add(line);
                line = reader.readLine();
            }
        } catch (FileNotFoundException ex) {
            log.error("Failed", ex);
        } catch (IOException ex) {
            log.error("Failed", ex);
        }
        return String.join("\n", lines);
    }


}
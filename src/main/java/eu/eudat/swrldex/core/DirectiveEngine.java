package eu.eudat.swrldex.core;


import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import org.semanticweb.owlapi.model.OWLOntologyCreationException;
import org.slf4j.LoggerFactory;

import java.nio.file.Paths;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;

// TODO: generative rules: create new service invocations
// TODO: performance: reuse ontology: recursively remove input+output individuals after event

public class DirectiveEngine {
    private static final org.slf4j.Logger log = LoggerFactory.getLogger(DirectiveEngine.class);

    public static int ONTOLOGY_POOL_SIZE = 10;

    private Gson gson = new GsonBuilder().setPrettyPrinting().create();

    private Queue<OntologyHelper> pool = new ConcurrentLinkedQueue();
    private Object poolMon = new Object();

    public DirectiveEngine() {
        new Thread(() -> {
            while (true) {
                try {
                    if (pool.size() >= ONTOLOGY_POOL_SIZE) {
                        synchronized (poolMon) { poolMon.wait(); }
                    } else {
                        OntologyHelper oh = new OntologyHelper("dex:", "http://eudat.eu/ns/dex#",
                                Paths.get("eventOntology.xml"));
                        pool.add(oh);
                        log.info("pool: new ontology added, now " + pool.size());
                    }
                } catch (OWLOntologyCreationException e) {
                    log.error("pool: ontology creation error", e);
                } catch (InterruptedException e) {
                    // ignore it
                }
            }
        }).start();
    }

    private OntologyHelper newOntologyHelper() throws OWLOntologyCreationException {
        if (!pool.isEmpty()) {
            OntologyHelper oh = pool.poll();
            synchronized (poolMon) { poolMon.notify(); }
            return oh;
        } else {
            return new OntologyHelper("dex:", "http://eudat.eu/ns/dex#",
                    Paths.get("eventOntology.xml"));
        }
    }

    public JsonObject event(JsonObject jsonEvent) {
        try {
            OntologyHelper oh = newOntologyHelper();
            OntologyHelper.OClass inputCls = oh.cls("INPUT");
            OntologyHelper.OClass outputCls = oh.cls("OUTPUT");

            // root individual for all incoming data
            OntologyHelper.OIndividual input = oh.ind("input");
            input.addType(inputCls);

            // root individual for all outgoing data
            OntologyHelper.OIndividual output = oh.ind("output");
            output.addType(outputCls);

            new JsonLoader(oh).load(input, jsonEvent, output);

            // oh.print();
            // oh.printAsXML();
            // oh.saveAsXML(Paths.get("event.out.xml"));

            // System.out.println("--- dumped input:");
            // System.out.println(gson.toJson(new JsonDumper(oh).dump(input)));
            // System.out.println("");

            oh.execRulesFromDir(Paths.get("rules"));

            // System.out.println("--- dumped output:");
            // System.out.println(gson.toJson(new JsonDumper(oh).dump(output)));
            // System.out.println("");

            JsonObject jsonEventOutput = new JsonDumper(oh).dump(output);
            return jsonEventOutput;
        } catch (OWLOntologyCreationException e) {
            log.error("Ontology creation error: ", e);
        } catch (org.swrlapi.parser.SWRLParseException e) {
            log.error("Parse error: " + e.getMessage());
        } catch (Exception e) {
            log.error("Error: ", e);
        }
        return null;
    }
}
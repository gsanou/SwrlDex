package eu.eudat.swrldex.core;

import org.checkerframework.checker.nullness.qual.NonNull;
import org.semanticweb.owlapi.apibinding.OWLManager;
import org.semanticweb.owlapi.io.FileDocumentTarget;
import org.semanticweb.owlapi.io.StringDocumentTarget;
import org.semanticweb.owlapi.model.*;
import org.semanticweb.owlapi.util.DefaultPrefixManager;
import org.swrlapi.core.IRIResolver;
import org.swrlapi.core.SWRLRuleEngine;
import org.swrlapi.exceptions.SWRLBuiltInException;
import org.swrlapi.factory.SWRLAPIFactory;
import org.swrlapi.parser.SWRLParseException;
import org.swrlapi.sqwrl.SQWRLQueryEngine;
import org.swrlapi.sqwrl.SQWRLResult;
import org.swrlapi.sqwrl.exceptions.SQWRLException;

import javax.annotation.Nonnull;
import java.nio.file.Path;
import java.util.Optional;
import java.util.Set;

class OntologyHelper {
    PrefixManager pm = new DefaultPrefixManager();
    IRIResolver resolver = SWRLAPIFactory.createIRIResolver();
    OWLOntologyManager ontologyManager;
    OWLDataFactory df;
    OWLOntology ontology;
    IRIResolver printerIriResolver = new IRIResolver() {
        @Override
        public void reset() {
            System.out.println("iri reset");
            resolver.reset();
        }

        @Override
        public @NonNull Optional<IRI> prefixedName2IRI(@NonNull String prefixedName) {
            Optional<IRI> ret = resolver.prefixedName2IRI(prefixedName);
            System.out.println("iri prefixedName2IRI " + prefixedName + " -> " + ret);
            return ret;
        }

        @Override
        public @NonNull Optional<IRI> variableName2IRI(@NonNull String variableName) {
            Optional<IRI> ret = resolver.variableName2IRI(variableName);
            System.out.println("iri variableName2IRI " + variableName + " -> " + ret);
            return ret;
        }

        @Override
        public @NonNull Optional<String> iri2PrefixedName(@NonNull IRI iri) {
            Optional<String> ret = resolver.iri2PrefixedName(iri);
            System.out.println("iri iri2prefixedname " + iri + " -> " + ret);
            return ret;
        }

        @Override
        public @NonNull Optional<String> iri2VariableName(@NonNull IRI iri) {
            Optional<String> ret = resolver.iri2VariableName(iri);
            System.out.println("iri iri2varname " + iri + " -> " + ret);
            return ret;
        }

        @Override
        public @NonNull Optional<String> iri2ShortForm(@NonNull IRI iri) {
            Optional<String> ret = resolver.iri2ShortForm(iri);
            System.out.println("iri iri2shortform " + iri + " -> " + ret);
            return ret;
        }

        @Override
        public void updatePrefixes(@NonNull OWLOntology ontology) {
            System.out.println("iri updateprefixes ");
            resolver.updatePrefixes(ontology);
        }

        @Override
        public void setPrefix(@NonNull String prefix, @NonNull String namespace) {
            System.out.println("iri setprefix " + prefix + " : " + namespace);
            resolver.setPrefix(prefix, namespace);
        }

        @Override
        public @NonNull IRI generateIRI() {
            IRI ret = resolver.generateIRI();
            System.out.println("iri generateIRI " + " -> " + ret);
            return ret;
        }

        @Override
        public @NonNull String render(@Nonnull OWLObject owlObject) {
            String ret = resolver.render(owlObject);
            System.out.println("iri render " + owlObject + " -> " + ret);
            return ret;
        }
    };


    public OntologyHelper(String prefix, String namespaceIRI) throws OWLOntologyCreationException {
        pm.setPrefix(prefix, namespaceIRI);
        resolver.setPrefix(prefix, namespaceIRI);
        ontologyManager = OWLManager.createOWLOntologyManager();
        df = OWLManager.getOWLDataFactory();
        ontology = ontologyManager.createOntology();
    }

    public void addSubClass(String parent, String child) {
        add(subClassAx(toClass(parent), toClass(child)));
    }

    public void addType(String parent, String child) {
        add(typeAx(toClass(parent), toInd(child)));
    }

    public void addProp(String parent, String prop, String child) {
        add(propAx(toProp(prop), toInd(parent), toInd(child)));
    }

    public void addDataProp(String parent, String prop, String child) {
        add(dataPropAx(toDataProp(prop), toInd(parent), child));
    }

    public void addDataProp(String parent, String prop, double child) {
        add(dataPropAx(toDataProp(prop), toInd(parent), child));
    }

    public void addDataProp(String parent, String prop, boolean child) {
        add(dataPropAx(toDataProp(prop), toInd(parent), child));
    }

    public void addRule(String id, String ruleText) throws SWRLBuiltInException, SWRLParseException {
        SWRLRuleEngine ruleEngine = SWRLAPIFactory.createSWRLRuleEngine(ontology);
        ruleEngine.createSWRLRule(id, ruleText);
    }

    public SQWRLResult runSQWRL(String id, String queryText) throws SQWRLException, SWRLParseException {
        IRIResolver iriResolver = resolver;
        SQWRLQueryEngine queryEngine = SWRLAPIFactory.createSQWRLQueryEngine(ontology, iriResolver);
//        List<String> builtins = queryEngine.getSWRLBuiltInIRIs()
//                .stream()
//                .map(iri -> iri.toString())
//                .sorted()
//                .distinct()
//                .collect(Collectors.toList());
//        for (String s: builtins) {
//            System.out.println("    builtin: " + s);
//        }
//        for (String s: queryEngine.getSQWRLQueryNames()) {
//            System.out.println("    query name: " + s);
//        }
        return queryEngine.runSQWRLQuery(id, queryText);
    }

    public void printSQWRL(String id, String queryText) throws SQWRLException, SWRLParseException {
        SQWRLResult result = runSQWRL(id, queryText);
        System.out.println("---- query results["+id+"]:");
        System.out.println(result.getColumnNames());
        while (result.next()) {
            System.out.println(result.getRow());
        }
        System.out.println("---- ");
    }

    public void print() throws OWLOntologyStorageException {
        StringDocumentTarget target = new StringDocumentTarget();
        ontologyManager.saveOntology(ontology, target);
//        System.out.println("---- " + rootIRI);
        System.out.println("---- " + pm.getDefaultPrefix());
        System.out.println(target.toString());
        System.out.println("----");
    }

    public void saveAsXML(Path path) throws OWLOntologyStorageException {
        FileDocumentTarget target = new FileDocumentTarget(path.toFile());
        ontologyManager.saveOntology(ontology, target);
    }

    OWLClass toClass(String name) {
//        return df.getOWLClass(IRI.create(rootIRI + "#" + name));
        return df.getOWLClass(name, pm);
    };

    OWLNamedIndividual toInd(String name) {
//        return df.getOWLNamedIndividual(IRI.create(rootIRI + "#" + name));
        return df.getOWLNamedIndividual(name, pm);
    };

    OWLObjectProperty toProp(String name) {
//        return df.getOWLObjectProperty(IRI.create(rootIRI + "#" + name));
        return df.getOWLObjectProperty(name, pm);
    };

    OWLDataProperty toDataProp(String name) {
//        return df.getOWLObjectProperty(IRI.create(rootIRI + "#" + name));
        return df.getOWLDataProperty(name, pm);
    };

    OWLAxiom subClassAx(OWLClass parent, OWLClass child) {
        return df.getOWLSubClassOfAxiom(child, parent);
    }

    OWLAxiom typeAx(OWLClass class_, OWLNamedIndividual individual) {
        return df.getOWLClassAssertionAxiom(class_, individual);
    }

    OWLAxiom propAx(OWLObjectProperty p, OWLNamedIndividual parent, OWLNamedIndividual child) {
        return df.getOWLObjectPropertyAssertionAxiom(p, parent, child);
    }

    OWLAxiom dataPropAx(OWLDataProperty p, OWLNamedIndividual parent, double d) {
        return df.getOWLDataPropertyAssertionAxiom(p, parent, d);
    }

    OWLAxiom dataPropAx(OWLDataProperty p, OWLNamedIndividual parent, boolean b) {
        return df.getOWLDataPropertyAssertionAxiom(p, parent, b);
    }

    OWLAxiom dataPropAx(OWLDataProperty p, OWLNamedIndividual parent, String s) {
        return df.getOWLDataPropertyAssertionAxiom(p, parent, s);
    }

    SWRLRule ruleAx(Set<SWRLAtom> body, Set<SWRLAtom> head) {
        return df.getSWRLRule(body, head);
    }

    void add(OWLAxiom axiom) {
        AddAxiom addAxiom = new AddAxiom(ontology, axiom);
        ontologyManager.applyChange(addAxiom);
    }
}

package org.snomed.otf.owltoolkit;

import org.semanticweb.elk.owlapi.ElkReasonerFactory;
import org.semanticweb.owlapi.apibinding.OWLManager;
import org.semanticweb.owlapi.formats.FunctionalSyntaxDocumentFormat;
import org.semanticweb.owlapi.model.*;
import org.semanticweb.owlapi.model.parameters.ChangeApplied;
import org.semanticweb.owlapi.reasoner.*;
import org.snomed.otf.owltoolkit.constants.Concepts;
import org.snomed.otf.owltoolkit.ontology.render.SnomedFunctionalSyntaxStorerFactory;
import org.snomed.otf.owltoolkit.taxonomy.AxiomDeserialiser;
import org.snomed.otf.owltoolkit.util.TimerUtil;

import java.io.*;
import java.util.HashSet;
import java.util.Set;

import static org.snomed.otf.owltoolkit.ontology.OntologyService.SNOMED_INTERNATIONAL_EDITION_URI;
import static org.snomed.otf.owltoolkit.ontology.OntologyService.getFunctionalSyntaxDocumentFormat;

/**
 * Hacky application to test expression subsumption with minimal time and memory.
 */
public class TinyFastApplication {

	private final OWLOntologyManager manager;
	private final OWLOntology ontology;
	private TimerUtil timer;
	private AxiomDeserialiser axiomDeserialiser;
	private OWLReasoner reasoner;

	public static void main(String[] args) throws OWLOntologyCreationException, IOException, OWLOntologyStorageException, InterruptedException {
		final TinyFastApplication app = new TinyFastApplication("snomed-releases/SnomedCT_InternationalRF2_PRODUCTION_20200731T120000Z" +
				"/Snapshot/Terminology/sct2_sRefset_OWLExpressionSnapshot_INT_20200731.txt");

		app.classifyOwlExpressions(
				"SubClassOf(\n" +
				"	:1010000010\n" +
				"	ObjectIntersectionOf(\n" +
				"		:404684003 |Clinical finding (finding)|\n" +
				"		ObjectSomeValuesFrom(\n" +
				"			:609096000 |Role group (attribute)|\n" +
				"			ObjectSomeValuesFrom(\n" +
				"				:116676008 |Associated morphology (attribute)|\n" +
				"				:50960005 |Hemorrhage (morphologic abnormality)|\n" +
				"			)\n" +
				"		)\n" +
				"	)\n" +
				")",
				"EquivalentClasses(\n" +
						"	:1020000010\n" +
						"	ObjectIntersectionOf(\n" +
						"		:281647001 |Adverse reaction (disorder)|\n" +
						"		ObjectSomeValuesFrom(\n" +
						"			:609096000 |Role group (attribute)|\n" +
						"			ObjectSomeValuesFrom(\n" +
						"				:246075003 |Causative agent (attribute)|\n" +
						"				:758665008 | Caffeine hydrate (substance) |\n\n" +
						"			)\n" +
						"		)\n" +
						"	)\n" +
						")",
				"EquivalentClasses(\n" +
						"	:1030000010\n" +
						"	ObjectIntersectionOf(\n" +
						"		:281647001 |Adverse reaction (disorder)|\n" +
						"		ObjectSomeValuesFrom(\n" +
						"			:609096000 |Role group (attribute)|\n" +
						"			ObjectSomeValuesFrom(\n" +
						"				:246075003 |Causative agent (attribute)|\n" +
						"				:770965008 | Pegvaliase (substance) |\n\n" +
						"			)\n" +
						"		)\n" +
						"	)\n" +
						")",				"EquivalentClasses(\n" +
						"	:1040000010\n" +
						"	ObjectIntersectionOf(\n" +
						"		:281647001 |Adverse reaction (disorder)|\n" +
						"		ObjectSomeValuesFrom(\n" +
						"			:609096000 |Role group (attribute)|\n" +
						"			ObjectSomeValuesFrom(\n" +
						"				:246075003 |Causative agent (attribute)|\n" +
						"				:361000220103 | Nalmefene hydrochloride dihydrate (substance) |\n\n" +
						"			)\n" +
						"		)\n" +
						"	)\n" +
						")",
				"EquivalentClasses(\n" +
						"	:1050000010\n" +
						"	ObjectIntersectionOf(\n" +
						"		:281647001 |Adverse reaction (disorder)|\n" +
						"		ObjectSomeValuesFrom(\n" +
						"			:609096000 |Role group (attribute)|\n" +
						"			ObjectSomeValuesFrom(\n" +
						"				:246075003 |Causative agent (attribute)|\n" +
						"				:381000220107 | Nicotine bitartrate dihydrate (substance) |\n\n" +
						"			)\n" +
						"		)\n" +
						"	)\n" +
						")"
		);

	}

	public TinyFastApplication(String axiomRefsetFilePath) throws IOException, OWLOntologyCreationException, OWLOntologyStorageException {
		timer = new TimerUtil("");

		manager = OWLManager.createOWLOntologyManager();
		ontology = createOntology(new FileInputStream(axiomRefsetFilePath));
		timer.checkpoint("Create ontology");

		precompute(ontology);
		dumpOntology(ontology);
		timer.checkpoint("Classify ontology");
	}

	private void precompute(OWLOntology ontology) {
		final OWLReasonerConfiguration configuration = new SimpleConfiguration(new ConsoleProgressMonitor());
		reasoner = new ElkReasonerFactory().createReasoner(ontology, configuration);
		reasoner.flush();
		reasoner.precomputeInferences(InferenceType.CLASS_HIERARCHY);
	}

	private OWLOntology createOntology(InputStream axiomRefsetStream) throws OWLOntologyCreationException, IOException {
		OWLOntology ontology = manager.createOntology(IRI.create(SNOMED_INTERNATIONAL_EDITION_URI));

		axiomDeserialiser = new AxiomDeserialiser();
		try (final BufferedReader reader = new BufferedReader(new InputStreamReader(axiomRefsetStream))) {
			final String header = reader.readLine();
			if (!"id	effectiveTime	active	moduleId	refsetId	referencedComponentId	owlExpression".equals(header)) {
				throw new IllegalArgumentException("The file provided does not have the expected header for an RF2 OWL Axiom reference set.");
			}
			String line;
			String[] values;
			Set<OWLAxiom> axioms = new HashSet<>();
			while ((line = reader.readLine()) != null) {
				values = line.split("	");
				// 0	1				2		3			4			5						6
				// id	effectiveTime	active	moduleId	refsetId	referencedComponentId	owlExpression
				if (values[2].equals("1")) {
					if (values[4].equals(Concepts.OWL_AXIOM_REFERENCE_SET)) {
						final OWLAxiom owlAxiom = axiomDeserialiser.deserialiseAxiom(values[6], values[0]);
						axioms.add(owlAxiom);
					}
				}
			}
			manager.addAxioms(ontology, axioms);
		}

		manager.setOntologyFormat(ontology, getFunctionalSyntaxDocumentFormat());
		return ontology;
	}

	private void classifyOwlExpressions(String... owlExpressions) throws OWLOntologyCreationException {
		timer = new TimerUtil("Classify expressions (" + owlExpressions.length + ")");

		Set<OWLClassAxiom> owlClassAxioms = new HashSet<>();
		for (String owlExpression : owlExpressions) {
			owlExpression = owlExpression.replaceAll("\\|[^|]*\\|", "");
			OWLClassAxiom owlAxiom = (OWLClassAxiom) axiomDeserialiser.deserialiseAxiom(owlExpression, "expression");
			owlClassAxioms.add(owlAxiom);
		}

		final ChangeApplied changeApplied = manager.addAxioms(ontology, owlClassAxioms);
		if (changeApplied != ChangeApplied.SUCCESSFULLY) {
			throw new IllegalStateException("Axioms not added to ontology. ChangeApplied:" + changeApplied);
		}

		reasoner.flush();
		reasoner.precomputeInferences(InferenceType.CLASS_HIERARCHY);
		timer.finish();

		printSuperClasses(owlClassAxioms);

		manager.removeAxioms(ontology, owlClassAxioms);
	}

	private void printSuperClasses(Set<OWLClassAxiom> owlClassAxioms) {
		for (OWLClassAxiom owlAxiom : owlClassAxioms) {
			OWLClass owlClass;
			if (owlAxiom instanceof OWLSubClassOfAxiom) {
				OWLSubClassOfAxiom subClass = (OWLSubClassOfAxiom) owlAxiom;
				owlClass = subClass.getSubClass().getClassesInSignature().iterator().next();
			} else {
				OWLEquivalentClassesAxiom equivalentClass = (OWLEquivalentClassesAxiom) owlAxiom;
				owlClass = equivalentClass.getNamedClasses().iterator().next();
			}

			final NodeSet<OWLClass> superClasses = reasoner.getSuperClasses(owlClass, true);

			System.out.println();
			System.out.println("Inferred superclasses for " + owlClass.toString() + ":");
			for (Node<OWLClass> superClass : superClasses) {
				System.out.print("- ");
				System.out.println(superClass);
			}
		}
	}


	private void dumpOntology(OWLOntology ontology) throws FileNotFoundException, OWLOntologyStorageException {
		manager.getOntologyStorers().add(new SnomedFunctionalSyntaxStorerFactory());

		FunctionalSyntaxDocumentFormat owlDocumentFormat = getFunctionalSyntaxDocumentFormat();
		ontology.getOWLOntologyManager().setOntologyFormat(ontology, owlDocumentFormat);
		ontology.saveOntology(owlDocumentFormat, new FileOutputStream("ontology-dump.owl"));
	}
}

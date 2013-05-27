package ixa.kaflib;

import org.jdom2.Document;
import org.jdom2.Element;
import org.jdom2.Comment;
import org.jdom2.Namespace;
import org.jdom2.output.XMLOutputter;
import org.jdom2.output.Format;
import org.jdom2.input.SAXBuilder;
import org.jdom2.JDOMException;
import org.jdom2.xpath.XPathExpression;
import java.util.List;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.io.File;
import java.io.Writer;
import java.io.Reader;
import java.io.BufferedWriter;
import java.io.OutputStreamWriter;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.charset.Charset;

/** Reads XML files in KAF format and loads the content in a KAFDocument object, and writes the content into XML files. */
class ReadWriteManager {
    
    /** Loads the content of a KAF file into the given KAFDocument object */
    static KAFDocument load(File file) throws IOException, JDOMException {
	SAXBuilder builder = new SAXBuilder();
	try {
	    Document document = (Document) builder.build(file);
	    Element rootElem = document.getRootElement();
	    return DOMToKAF(document);
	} catch (IOException io) {
	    System.out.println(io.getMessage());
	    throw io;
	} catch (JDOMException jdomex) {
	    System.out.println(jdomex.getMessage());
	    throw jdomex;
	}
    }

    /** Loads the content of a String in KAF format into the given KAFDocument object */
    static KAFDocument load(Reader stream) throws IOException, JDOMException {
	SAXBuilder builder = new SAXBuilder();
	try {
	    Document document = (Document) builder.build(stream);
	    Element rootElem = document.getRootElement();
	    return DOMToKAF(document);
	} catch (IOException io) {
	    System.out.println(io.getMessage());
	    throw io;
	} catch (JDOMException jdomex) {
	    System.out.println(jdomex.getMessage());
	    throw jdomex;
	}
    }

    /** Writes the content of a given KAFDocument to a file. */
    static void save(KAFDocument kaf, String filename) {
	try {
	    File file = new File(filename);
	    Writer out = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(file), "UTF8"));
	    out.write(kafToStr(kaf));
	    out.flush();
	} catch (Exception e) {
	    System.out.println("Error writing to file");
	}
    }

    /** Writes the content of a KAFDocument object to standard output. */
    static void print(KAFDocument kaf) {
	try {
	    Writer out = new BufferedWriter(new OutputStreamWriter(System.out, "UTF8"));
	    out.write(kafToStr(kaf));
	    out.flush();
	} catch (Exception e) {
	    System.out.println("Encoding error");
	}
    }

    /** Returns a string containing the XML content of a KAFDocument object. */
    static String kafToStr(KAFDocument kaf) {
	XMLOutputter out = new XMLOutputter(Format.getPrettyFormat());
	Document jdom = KAFToDOM(kaf);
	return out.outputString(jdom);
    }

    /** Loads a KAFDocument object from XML content in DOM format */
    private static KAFDocument DOMToKAF(Document dom) {
	HashMap<String, WF> wfIndex = new HashMap<String, WF>();
	HashMap<String, Term> termIndex = new HashMap<String, Term>();
	Element rootElem = dom.getRootElement();
	String lang = getAttribute("lang", rootElem, Namespace.XML_NAMESPACE);
	String kafVersion = getAttribute("version", rootElem);
	KAFDocument kaf = new KAFDocument(lang, kafVersion);

	List<Element> rootChildrenElems = rootElem.getChildren();
	for (Element elem : rootChildrenElems) {
	    if (elem.getName().equals("kafHeader")) {
		List<Element> lpsElems = elem.getChildren();
		for (Element lpsElem : lpsElems) {
		    String layer = getAttribute("layer", lpsElem);
		    List<Element> lpElems = lpsElem.getChildren();
		    for (Element lpElem : lpElems) {
			String name = getAttribute("name", lpElem);
			String timestamp = getOptAttribute("timestamp", lpElem);
			String version = getAttribute("version", lpElem);
			kaf.addLinguisticProcessor(layer, name, timestamp, version);
		    }
		}
	    }
	    if (elem.getName().equals("text")) {
		List<Element> wfElems = elem.getChildren();
		for (Element wfElem : wfElems) {
		    String wid = getAttribute("wid", wfElem);
		    String wForm = wfElem.getText();
		    WF newWf = kaf.createWF(wid, wForm);
		    String wSent = getOptAttribute("sent", wfElem);
		    if (wSent != null) {
			newWf.setSent(Integer.valueOf(wSent));
		    }
		    String wPara = getOptAttribute("para", wfElem);
		    if (wPara != null) {
			newWf.setPara(Integer.valueOf(wPara));
		    }
		    String wPage = getOptAttribute("page", wfElem);
		    if (wPage != null) {
			newWf.setPage(Integer.valueOf(wPage));
		    }
		    String wOffset = getOptAttribute("offset", wfElem);
		    if (wOffset != null) {
			newWf.setOffset(Integer.valueOf(wOffset));
		    }
		    String wLength = getOptAttribute("length", wfElem);
		    if (wLength != null) {
			newWf.setLength(Integer.valueOf(wLength));
		    }
		    String wXpath = getOptAttribute("xpath", wfElem);
		    if (wXpath != null) {
			newWf.setXpath(wXpath);
		    }
		    wfIndex.put(newWf.getId(), newWf);
		}
	    }
	    if (elem.getName().equals("terms")) {
		List<Element> termElems = elem.getChildren();
		for (Element termElem : termElems) {
		    String tid = getAttribute("tid", termElem);
		    String type = getAttribute("type", termElem);
		    String lemma = getAttribute("lemma", termElem);
		    String pos = getAttribute("pos", termElem);
		    List<Element> spans = termElem.getChildren("span");
		    if (spans.size() < 1) {
			throw new IllegalStateException("Every term must contain a span element");
		    }
		    List<Element> termsWfElems = spans.get(0).getChildren();
		    List<WF> termsWfs = new ArrayList<WF>();
		    for (Element termsWfElem : termsWfElems) {
			String wfId = getAttribute("id", termsWfElem);
			termsWfs.add(wfIndex.get(wfId));
		    }
		    Term newTerm = kaf.createTerm(tid, type, lemma, pos, termsWfs);
		    String tMorphofeat = getOptAttribute("morphofeat", termElem);
		    if (tMorphofeat != null) {
			newTerm.setMorphofeat(tMorphofeat);
		    }
		    String tHead = getOptAttribute("head", termElem);
		    if (tHead != null) {
			newTerm.setHead(tHead);
		    }
		    String termcase = getOptAttribute("case", termElem);
		    if (termcase != null) {
			newTerm.setCase(termcase);
		    }
		    List<Element> sentimentElems = termElem.getChildren("sentiment");
		    if (sentimentElems.size() > 0) {
			Element sentimentElem = sentimentElems.get(0);
			Term.Sentiment newSentiment = kaf.createSentiment();
			String sentResource = getOptAttribute("resource", sentimentElem);
			if (sentResource != null) {
			    newSentiment.setResource(sentResource);
			}
			String sentPolarity = getOptAttribute("polarity", sentimentElem);
			if (sentPolarity != null) {
			    newSentiment.setPolarity(sentPolarity);
			}
			String sentStrength = getOptAttribute("strength", sentimentElem);
			if (sentStrength != null) {
			    newSentiment.setStrength(sentStrength);
			}
			String sentSubjectivity = getOptAttribute("subjectivity", sentimentElem);
			if (sentSubjectivity != null) {
			    newSentiment.setSubjectivity(sentSubjectivity);
			}
			String sentSentimentSemanticType = getOptAttribute("sentimentSemanticType", sentimentElem);
			if (sentSentimentSemanticType != null) {
			    newSentiment.setSentimentSemanticType(sentSentimentSemanticType);
			}
			String sentSentimentModifier = getOptAttribute("sentimentModifier", sentimentElem);
			if (sentSentimentModifier != null) {
			    newSentiment.setSentimentModifier(sentSentimentModifier);
			}
			String sentSentimentMarker = getOptAttribute("sentimentMarker", sentimentElem);
			if (sentSentimentMarker != null) {
			    newSentiment.setSentimentMarker(sentSentimentMarker);
			}
			String sentSentimentProductFeature = getOptAttribute("sentimentProductFeature", sentimentElem);
			if (sentSentimentProductFeature != null) {
			    newSentiment.setSentimentProductFeature(sentSentimentProductFeature);
			}
			newTerm.setSentiment(newSentiment);
		    }
		    List<Element> termsComponentElems = termElem.getChildren("component");
		    for (Element termsComponentElem : termsComponentElems) {
			String compId = getAttribute("id", termsComponentElem);
			String compLemma = getAttribute("lemma", termsComponentElem);
			String compPos = getAttribute("pos", termsComponentElem);
			Term.Component newComponent = kaf.createComponent(compId, newTerm, compLemma, compPos);
			List<Element> externalReferencesElems = termsComponentElem.getChildren("externalReferences");
			if (externalReferencesElems.size() > 0) {
			    List<ExternalRef> externalRefs = getExternalReferences(externalReferencesElems.get(0), kaf);
			    newComponent.addExternalRefs(externalRefs);
			}
			newTerm.addComponent(newComponent);
		    }
		    List<Element> externalReferencesElems = termElem.getChildren("externalReferences");
		    if (externalReferencesElems.size() > 0) {
			List<ExternalRef> externalRefs = getExternalReferences(externalReferencesElems.get(0), kaf);
			newTerm.addExternalRefs(externalRefs);
		    }
		    termIndex.put(newTerm.getId(), newTerm);
		}
	    }
	    if (elem.getName().equals("deps")) {
		List<Element> depElems = elem.getChildren();
		for (Element depElem : depElems) {
		    String fromId = getAttribute("from", depElem);
		    Term from = termIndex.get(fromId);
		    String toId = getAttribute("to", depElem);
		    Term to = termIndex.get(toId);
		    String rfunc = getAttribute("rfunc", depElem);
		    Dep newDep = kaf.createDep(from, to, rfunc);
		    String depcase = getOptAttribute("case", depElem);
		    if (depcase != null) {
			newDep.setCase(depcase);
		    }
		}
	    }
	    if (elem.getName().equals("chunks")) {
		List<Element> chunkElems = elem.getChildren();
		for (Element chunkElem : chunkElems) {
		    String chunkId = getAttribute("cid", chunkElem);
		    String headId = getAttribute("head", chunkElem);
		    Term chunkHead = termIndex.get(headId);
		    String chunkPhrase = getAttribute("phrase", chunkElem);
		    List<Element> spans = chunkElem.getChildren("span");
		    if (spans.size() < 1) {
			throw new IllegalStateException("Every chunk must contain a span element");
		    }
		    List<Element> chunksTermElems = spans.get(0).getChildren();
		    List<Term> chunksTerms = new ArrayList<Term>();
		    for (Element chunksTermElem : chunksTermElems) {
			String termId = getAttribute("id", chunksTermElem);
			chunksTerms.add(termIndex.get(termId));
		    }
		    Chunk newChunk = kaf.createChunk(chunkId, chunkHead, chunkPhrase, chunksTerms);
		    String chunkCase = getOptAttribute("case", chunkElem);
		    if (chunkCase != null) {
			newChunk.setCase(chunkCase);
		    }
		}
	    }
	    if (elem.getName().equals("entities")) {
		List<Element> entityElems = elem.getChildren();
		for (Element entityElem : entityElems) {
		    String entId = getAttribute("eid", entityElem);
		    String entType = getAttribute("type", entityElem);
		    List<Element> referencesElem = entityElem.getChildren("references");
		    if (referencesElem.size() < 1) {
			throw new IllegalStateException("Every entity must contain a 'references' element");
		    }
		    List<Element> spanElems = referencesElem.get(0).getChildren();
		    if (spanElems.size() < 1) {
			throw new IllegalStateException("Every entity must contain a 'span' element inside 'references'");
		    }
		    List<List<Term>> references = new ArrayList<List<Term>>();
		    for (Element spanElem : spanElems) {
			List<Term> span = new ArrayList<Term>();
			List<Element> targetElems = spanElem.getChildren();
			if (targetElems.size() < 1) {
			    throw new IllegalStateException("Every span in an entity must contain at least one target inside");  
			}
			for (Element targetElem : targetElems) {
			    String targetTermId = getAttribute("id", targetElem);
			    Term targetTerm = termIndex.get(targetTermId);
			    span.add(targetTerm);
			}
			references.add(span);
		    }
		    Entity newEntity = kaf.createEntity(entId, entType, references);
		    List<Element> externalReferencesElems = entityElem.getChildren("externalReferences");
		    if (externalReferencesElems.size() > 0) {
			List<ExternalRef> externalRefs = getExternalReferences(externalReferencesElems.get(0), kaf);
			newEntity.addExternalRefs(externalRefs);
		    }
		}
	    }
	    if (elem.getName().equals("coreferences")) {
		List<Element> corefElems = elem.getChildren();
		for (Element corefElem : corefElems) {
		    String coId = getAttribute("coid", corefElem);
		    List<Element> referencesElem = corefElem.getChildren("references");
		    if (referencesElem.size() < 1) {
			throw new IllegalStateException("Every coref must contain a 'references' element");
		    }
		    List<Element> spanElems = referencesElem.get(0).getChildren();
		    if (spanElems.size() < 1) {
			throw new IllegalStateException("Every coref must contain a 'span' element inside 'references'");
		    }
		    List<List<Target>> references = new ArrayList<List<Target>>();
		    for (Element spanElem : spanElems) {
			List<Target> span = new ArrayList<Target>();
			List<Element> targetElems = spanElem.getChildren();
			if (targetElems.size() < 1) {
			    throw new IllegalStateException("Every span in an entity must contain at least one target inside");  
			}
			for (Element targetElem : targetElems) {
			    String targetTermId = getAttribute("id", targetElem);
			    Term targetTerm = termIndex.get(targetTermId);
			    String targetTermIsHead = getOptAttribute("head", targetElem);
			    Target target;
			    if ((targetTermIsHead != null) && (targetTermIsHead.equals("yes"))) {
				target = kaf.createTarget(targetTerm, true);
			    } else {
				target = kaf.createTarget(targetTerm);
			    }
			    span.add(target);
			}
			references.add(span);
		    }
		    Coref newCoref = kaf.createCoref(coId, references);
		}
	    }
	}

	return kaf;
    }

    private static List<ExternalRef> getExternalReferences(Element externalReferencesElem, KAFDocument kaf) {
	List<ExternalRef> externalRefs = new ArrayList<ExternalRef>();
	List<Element> externalRefElems = externalReferencesElem.getChildren();
	for (Element externalRefElem : externalRefElems) {
	    ExternalRef externalRef = getExternalRef(externalRefElem, kaf);
	    externalRefs.add(externalRef);
	}
	return externalRefs;
    }

    private static ExternalRef getExternalRef(Element externalRefElem, KAFDocument kaf) {
	String resource = getAttribute("resource", externalRefElem);
	String references = getAttribute("reference", externalRefElem);
	ExternalRef newExternalRef = kaf.createExternalRef(resource, references);
	String confidence = getOptAttribute("confidence", externalRefElem);
	if (confidence != null) {
	    newExternalRef.setConfidence(Float.valueOf(confidence));
	}
	List<Element> subRefElems = externalRefElem.getChildren("externalRef");
	if (subRefElems.size() > 0) {
	    Element subRefElem = subRefElems.get(0);
	    ExternalRef subRef = getExternalRef(subRefElem, kaf);
	    newExternalRef.setExternalRef(subRef);
	}
	return newExternalRef;
    }

    private static String getAttribute(String attName, Element elem) {
	String value = elem.getAttributeValue(attName);
	if (value==null) {
	    throw new IllegalStateException(attName+" attribute must be defined for element "+elem.getName());
	}
	return value;
    }

    private static String getAttribute(String attName, Element elem, Namespace nmspace) {
	String value = elem.getAttributeValue(attName, nmspace);
	if (value==null) {
	    throw new IllegalStateException(attName+" attribute must be defined for element "+elem.getName());
	}
	return value;
    }

    private static String getOptAttribute(String attName, Element elem) {
	String value = elem.getAttributeValue(attName);
	if (value==null || value.equals("")) {
	    return null;
	}
	return value;
    }

    /** Returns the content of the given KAFDocument in a DOM document. */
    private static Document KAFToDOM(KAFDocument kaf) {
	AnnotationContainer annotationContainer = kaf.getAnnotationContainer();
	Element root = new Element("KAF");
	root.setAttribute("lang", kaf.getLang(), Namespace.XML_NAMESPACE);
	root.setAttribute("version", kaf.getVersion());

	Document doc = new Document(root);

	Element kafHeaderElem = new Element("kafHeader");
	root.addContent(kafHeaderElem);

	HashMap<String, List<KAFDocument.LinguisticProcessor>> lps = kaf.getLinguisticProcessors();
	for (Map.Entry entry : lps.entrySet()) {
	    Element lpsElem = new Element("linguisticProcessors");
	    lpsElem.setAttribute("layer", (String) entry.getKey());
	    for (KAFDocument.LinguisticProcessor lp : (List<KAFDocument.LinguisticProcessor>) entry.getValue()) {
		Element lpElem = new Element("lp");
		lpElem.setAttribute("name", lp.name);
		lpElem.setAttribute("timestamp", lp.timestamp);
		lpElem.setAttribute("version", lp.version);
		lpsElem.addContent(lpElem);
	    }
	    kafHeaderElem.addContent(lpsElem);
	}

	List<WF> text = annotationContainer.getText();
	if (text.size() > 0) {
	    Element textElem = new Element("text");
	    for (WF wf : text) {
		Element wfElem = new Element("wf");
		wfElem.setAttribute("wid", wf.getId());
		if (wf.hasSent()) {
		    wfElem.setAttribute("sent", Integer.toString(wf.getSent()));
		}
		if (wf.hasPara()) {
		    wfElem.setAttribute("para", Integer.toString(wf.getPara()));
		}
		if (wf.hasPage()) {
		    wfElem.setAttribute("page", Integer.toString(wf.getPage()));
		}
		if (wf.hasOffset()) {
		    wfElem.setAttribute("offset", Integer.toString(wf.getOffset()));
		}
		if (wf.hasLength()) {
		    wfElem.setAttribute("length", Integer.toString(wf.getLength()));
		}
		if (wf.hasXpath()) {
		    wfElem.setAttribute("xpath", wf.getXpath());
		}
		wfElem.setText(wf.getForm());
		textElem.addContent(wfElem);
	    }
	    root.addContent(textElem);
	}

	List<Term> terms = annotationContainer.getTerms();
	if (terms.size() > 0) {
	    Element termsElem = new Element("terms");
	    for (Term term : terms) {
		String morphofeat;
		Term.Component head;
		String termcase;
		Comment termComment = new Comment(term.getStr());
		termsElem.addContent(termComment);
		Element termElem = new Element("term");
		termElem.setAttribute("tid", term.getId());
		termElem.setAttribute("type", term.getType());
		termElem.setAttribute("lemma", term.getLemma());
		termElem.setAttribute("pos", term.getPos());
		if (term.hasMorphofeat()) {
		    termElem.setAttribute("morphofeat", term.getMorphofeat());
		}
		if (term.hasHead()) {
		    termElem.setAttribute("head", term.getHead().getId());
		}
		if (term.hasCase()) {
		    termElem.setAttribute("case", term.getCase());
		}
		if (term.hasSentiment()) {
		    Term.Sentiment sentiment = term.getSentiment();
		    Element sentimentElem = new Element("sentiment");
		    if (sentiment.hasResource()) {
			sentimentElem.setAttribute("resource", sentiment.getResource());
		    }
		    if (sentiment.hasPolarity()) {
			sentimentElem.setAttribute("polarity", sentiment.getPolarity());
		    }
		    if (sentiment.hasStrength()) {
			sentimentElem.setAttribute("strength", sentiment.getStrength());
		    }
		    if (sentiment.hasSubjectivity()) {
			sentimentElem.setAttribute("subjectivity", sentiment.getSubjectivity());
		    }
		    if (sentiment.hasSentimentSemanticType()) {
			sentimentElem.setAttribute("sentimentSemanticType", sentiment.getSentimentSemanticType());
		    }
		    if (sentiment.hasSentimentModifier()) {
			sentimentElem.setAttribute("sentimentModifier", sentiment.getSentimentModifier());
		    }
		    if (sentiment.hasSentimentMarker()) {
			sentimentElem.setAttribute("sentimentMarker", sentiment.getSentimentMarker());
		    }
		    if (sentiment.hasSentimentProductFeature()) {
			sentimentElem.setAttribute("sentimentProductFeature", sentiment.getSentimentProductFeature());
		    }
		    termElem.addContent(sentimentElem);
		}
		Element spanElem = new Element("span");
		for (WF target : term.getWFs()) {
		    Element targetElem = new Element("target");
		    targetElem.setAttribute("id", target.getId());
		    spanElem.addContent(targetElem);
		}
		termElem.addContent(spanElem);
		List<Term.Component> components = term.getComponents();
		if (components.size() > 0) {
		    for (Term.Component component : components) {
			Element componentElem = new Element("component");
			componentElem.setAttribute("id", component.getId());
			componentElem.setAttribute("lemma", component.getLemma());
			componentElem.setAttribute("pos", component.getPos());
			if (component.hasCase()) {
			    componentElem.setAttribute("case", component.getCase());
			}
			List<ExternalRef> externalReferences = component.getExternalRefs();
			if (externalReferences.size() > 0) {
			    Element externalReferencesElem = externalReferencesToDOM(externalReferences);
			    componentElem.addContent(externalReferencesElem);
			}
			termElem.addContent(componentElem);
		    }
		}
		List<ExternalRef> externalReferences = term.getExternalRefs();
		if (externalReferences.size() > 0) {
		    Element externalReferencesElem = externalReferencesToDOM(externalReferences);
		    termElem.addContent(externalReferencesElem);
		}
		termsElem.addContent(termElem);
	    }
	    root.addContent(termsElem);
	}

	List<Dep> deps = annotationContainer.getDeps();
	if (deps.size() > 0) {
	    Element depsElem = new Element("deps");
	    for (Dep dep : deps) {
		Comment depComment = new Comment(dep.getStr());
		depsElem.addContent(depComment);
		Element depElem = new Element("dep");
		depElem.setAttribute("from", dep.getFrom().getId());
		depElem.setAttribute("to", dep.getTo().getId());
		depElem.setAttribute("rfunc", dep.getRfunc());
		if (dep.hasCase()) {
		    depElem.setAttribute("case", dep.getCase());
		}
		depsElem.addContent(depElem);
	    }
	    root.addContent(depsElem);
	}

	List<Chunk> chunks = annotationContainer.getChunks();
	if (chunks.size() > 0) {
	    Element chunksElem = new Element("chunks");
	    for (Chunk chunk : chunks) {
		Comment chunkComment = new Comment(chunk.getStr());
		chunksElem.addContent(chunkComment);
		Element chunkElem = new Element("chunk");
		chunkElem.setAttribute("cid", chunk.getId());
		chunkElem.setAttribute("head", chunk.getHead().getId());
		chunkElem.setAttribute("phrase", chunk.getPhrase());
		if (chunk.hasCase()) {
		    chunkElem.setAttribute("case", chunk.getCase());
		}
		Element spanElem = new Element("span");
		for (Term target : chunk.getTerms()) {
		    Element targetElem = new Element("target");
		    targetElem.setAttribute("id", target.getId());
		    spanElem.addContent(targetElem);
		}
		chunkElem.addContent(spanElem);
		chunksElem.addContent(chunkElem);
	    }
	    root.addContent(chunksElem);
	}

	List<Entity> entities = annotationContainer.getEntities();
	if (entities.size() > 0) {
	    Element entitiesElem = new Element("entities");
	    for (Entity entity : entities) {
		Element entityElem = new Element("entity");
		entityElem.setAttribute("eid", entity.getId());
		entityElem.setAttribute("type", entity.getType());
		Element referencesElem = new Element("references");
		for (List<Term> span : entity.getReferences()) {
		    Comment spanComment = new Comment(entity.getSpanStr(span));
		    referencesElem.addContent(spanComment);
		    Element spanElem = new Element("span");
		    for (Term term : span) {
			Element targetElem = new Element("target");
			targetElem.setAttribute("id", term.getId());
			spanElem.addContent(targetElem);
		    }
		    referencesElem.addContent(spanElem);
		}
		entityElem.addContent(referencesElem);
		List<ExternalRef> externalReferences = entity.getExternalRefs();
		if (externalReferences.size() > 0) {
		    Element externalReferencesElem = externalReferencesToDOM(externalReferences);
		    entityElem.addContent(externalReferencesElem);
		}
		entitiesElem.addContent(entityElem);
	    }
	    root.addContent(entitiesElem);
	}

	List<Coref> corefs = annotationContainer.getCorefs();
	if (corefs.size() > 0) {
	    Element corefsElem = new Element("coreferences");
	    for (Coref coref : corefs) {
		Element corefElem = new Element("coref");
		corefElem.setAttribute("coid", coref.getId());
		Element referencesElem = new Element("references");
		for (List<Target> span : coref.getReferences()) {
		    Comment spanComment = new Comment(coref.getSpanStr(span));
		    referencesElem.addContent(spanComment);
		    Element spanElem = new Element("span");
		    for (Target target : span) {
			Element targetElem = new Element("target");
			targetElem.setAttribute("id", target.getTerm().getId());
			if (target.isHead()) {
			    targetElem.setAttribute("head", "yes");
			}
			spanElem.addContent(targetElem);
		    }
		    referencesElem.addContent(spanElem);
		}
		corefElem.addContent(referencesElem);
		corefsElem.addContent(corefElem);
	    }
	    root.addContent(corefsElem);
	}

	return doc;
    }

    private static Element externalReferencesToDOM(List<ExternalRef> externalRefs) {
	Element externalReferencesElem = new Element("externalReferences");
	for (ExternalRef externalRef : externalRefs) {
	    Element externalRefElem = externalRefToDOM(externalRef);
	    externalReferencesElem.addContent(externalRefElem);
	}
	return externalReferencesElem;
    }

    private static Element externalRefToDOM(ExternalRef externalRef) {
	Element externalRefElem = new Element("externalRef");
	externalRefElem.setAttribute("resource", externalRef.getResource());
	externalRefElem.setAttribute("reference", externalRef.getReference());
	if (externalRef.hasConfidence()) {
	    externalRefElem.setAttribute("confidence", Float.toString(externalRef.getConfidence()));
	}
	if (externalRef.hasExternalRef()) {
	    Element subExternalRefElem = externalRefToDOM(externalRef.getExternalRef());
	    externalRefElem.addContent(subExternalRefElem);
	}
	return externalRefElem;
    }
}

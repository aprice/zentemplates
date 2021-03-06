package zenpoc;


import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.Writer;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.servlet.ServletContext;
import javax.servlet.http.HttpServletResponse;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringEscapeUtils;
import org.apache.commons.lang3.text.StrSubstitutor;
import org.apache.log4j.Logger;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Attribute;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

/**
 *
 * @author Adrian
 */
public class TemplateRenderer {
	private static final Logger LOG = Logger.getLogger(TemplateRenderer.class);
	private static final String DOCTYPE_HTML5 = "<!DOCTYPE html>\n";

	File templateFile;
	Document inDoc, outDoc;
	String result, templateRoot = null, siteRoot = null;
	ContextualLookup propLookup;
	DocumentContext rootContext;
	final Map<String, Object> model = new HashMap<String, Object>();
	boolean prettyPrint = false;

	public TemplateRenderer(String templatePath, ServletContext servletContext) {
		this(servletContext.getRealPath(templatePath));
	}

	public TemplateRenderer(String templatePath) {
		this(new File(templatePath));
	}

	public TemplateRenderer(File templateFile) {
		this.templateFile = templateFile;
		result = "";
	}

	public void addProperty(String key, Object value) {
		model.put(key, value);
	}

	public void addProperties(Map model) {
		this.model.putAll(model);
	}

	public void render(HttpServletResponse response) throws IOException {
		handleRendering();
		writeOut(response);
	}

	public void render(Writer out) throws IOException {
		handleRendering();
		writeOut(out);
	}

	private void handleRendering() throws IOException {
		LOG.info("Rendering template");
		loadTemplate();
		handleDerivation();
		handleInjection();
		handleSubstitution();
	}

	private void loadTemplate() throws IOException {
		LOG.info("Loading template: " + templateFile);
		inDoc = Jsoup.parse(templateFile, "UTF-8");
		inDoc.outputSettings().prettyPrint(false);
		outDoc = new Document(templateFile.getParent());
		outDoc.outputSettings().prettyPrint(false);
	}

	private void handleDerivation() throws IOException {
		Element inHtml = inDoc.child(0);

		// Locate derived parent
		String derivesFrom = inHtml.dataset().get("derivesfrom");
		if (derivesFrom == null) {
			derivesFrom = inHtml.attr("class");
			if (!derivesFrom.isEmpty()) {
				String[] candidates = derivesFrom.split("\\s");
				for (String parent : candidates) {
					File derivedFile = findTemplateFile(parent + ".html");
					if (derivedFile != null) {
						derivesFrom = derivedFile.getPath();
						break;
					}
				}
			}
		} else {
			File derivedFile = findTemplateFile(derivesFrom);
			derivesFrom = (derivedFile == null) ? null : derivedFile.getPath();
		}

		if (derivesFrom != null && !derivesFrom.isEmpty()) {
			LOG.info("Handling derivation: " + derivesFrom);

			// Handle nested derivation
			TemplateRenderer parentRenderer = new TemplateRenderer(derivesFrom);
			parentRenderer.loadTemplate();
			parentRenderer.handleDerivation();
			outDoc = parentRenderer.outDoc;

			// Handle derived elements
			Elements outPlaceholders = outDoc.getElementsByAttribute("id");
			for (Element outPlaceholder : outPlaceholders) {
				Element inReplacement = inDoc.getElementById(outPlaceholder.id());
				if (inReplacement != null) {
					outPlaceholder.replaceWith(inReplacement);
					if (outPlaceholder.id().endsWith("?")) {
						outPlaceholder.attr("id", outPlaceholder.id().substring(0, outPlaceholder.id().length()-1));
					}
				} else if (outPlaceholder.id().endsWith("?")) {
					outPlaceholder.remove();
				}
			}

			// Handle appended elements
			Elements inAppendices = inDoc.getElementsByAttribute("data-z-append");
			for (Element inAppendix : inAppendices) {
				Element inParent = inAppendix.parent();
				Element outParent = null;
				if (inParent.tagName().equals("head") || inParent.tagName().equals("body")) {
					outParent = outDoc.getElementsByTag(inParent.tagName()).get(0);
				} else if (!inParent.id().isEmpty()) {
					outParent = outDoc.getElementById(inParent.id());
				}

				if (outParent == null) {
					LOG.warn("Notice: no parent found for appendix: "+inAppendix.toString());
					continue;
				}

				String[] appendixData = inAppendix.dataset().get("z-append").split(":",2);

				Element outSibling = outParent.getElementById(appendixData[1]);
				if (outSibling == null) {
					LOG.error("Notice: sibling not found: "+appendixData[1]);
					continue;
				}

				if (appendixData[0].equals("before")) {
					outSibling.before(inAppendix);
				} else if (appendixData[0].equals("after")) {
					outSibling.after(inAppendix);
				}

				inAppendix.removeAttr("data-z-append");
			}
		} else {
			outDoc = inDoc.clone();
		}
	}

	private void handleInjection() {
		rootContext = new DocumentContext(outDoc, new ModelContext(model));
		propLookup = new ContextualLookup(rootContext);

		handleSnippets();

		LOG.trace("Handling lorems");
		Elements dummies = outDoc.getElementsByClass("z-lorem");
		for (Element e: dummies) {
			e.remove();
		}

		LOG.trace("Handling conditionals");
		// TODO: Move to element-level handling as this doesn't support nested contexts
		for (Element e : outDoc.getElementsByAttribute("data-z-if")) {
			String expression = e.dataset().get("z-if").trim();
			if (!rootContext.lookupBoolean(expression)) {
				e.remove();
			} else {
				e.removeAttr("data-z-if");
			}
		}

		LOG.trace("Handling injection");
		processElement(rootContext);
	}

	private void handleSnippets() {
		LOG.trace("Handling snippets");
		for (Element e : outDoc.getElementsByAttribute("data-z-snippet")) {
			String snippetName = e.attr("data-z-snippet");
			String[] snippetParts = snippetName.split("#",2);
			File snippetFile = findTemplateFile(snippetParts[0]);
			if (snippetFile != null) {
				String snippetContents = null;
				try {
					snippetContents = IOUtils.toString(new FileReader(snippetFile));
				} catch (IOException ex) {
					LOG.error("Could not read snippet file "+snippetFile.getPath()+": "+ex.getMessage());
					e.remove();
				}

				if (snippetContents != null) {
					Document snippetDoc = Jsoup.parseBodyFragment(snippetContents);
					if (snippetParts.length == 1) {
						e.replaceWith(snippetDoc.body().child(0));
					} else if (snippetDoc.getElementById(snippetParts[1]) != null) {
						e.replaceWith(snippetDoc.getElementById(snippetParts[1]));
					} else {
						LOG.error("Could not find snippet fragment by ID: "+snippetName);
						e.remove();
					}
				}
			} else {
				LOG.error("Could not locate snippet file: "+snippetName);
				e.remove();
			}

			e.removeAttr("data-z-snippet");
		}
	}

	private void processChildren(DocumentContext context) {
		if (!context.currentNode.children().isEmpty()) {
			Element curElem = context.currentNode.child(0);
			do {
				processElement(new DocumentContext(context, curElem, context.modelContext));
				curElem = curElem.nextElementSibling();
			} while (curElem != null);
		}
		handleSubstitution(context);
	}

	private void processElement(DocumentContext context) {
		LOG.trace("Processing element "+context.getElementPath()+" in scope "+context.modelContext.modelPath);
		Element element = context.currentNode;
		ModelContext modelCtx = context.modelContext;
		String injectKey = null;

		if (element.hasAttr("data-z-inject")) {
			injectKey = element.attr("data-z-inject");
			element.removeAttr("data-z-inject");
		} else if (element.hasAttr("data-z-no-inject")) {
			injectKey = null;
			element.removeAttr("data-z-no-inject");
		} else if (!element.id().isEmpty() && context.hasProperty(element.id())) {
			injectKey = element.id();
		} else if (element.hasAttr("class")) {
			String firstClass;
			if (element.className().isEmpty()) {
				firstClass = null;
			} else {
				firstClass = element.className().split(" ", 2)[0];
			}

			if (firstClass != null) LOG.trace("Model "+modelCtx.modelPath+" has property "+firstClass+"? "+(context.hasProperty(firstClass)?"yes":"no"));

			if (firstClass != null && context.hasProperty(firstClass)) {
				injectKey = firstClass;
			}
		}

		if (injectKey != null) {
			inject(context, injectKey);
		} else {
			processChildren(context);
		}
	}

	private void inject(DocumentContext context, String key) {
		LOG.debug("Injecting key: "+key);
		Object property = context.getProperty(key);
		LOG.trace(key+" is "+(property == null ? "null" : "a "+property.getClass()));

		Element element = context.currentNode;
		if (property == null) {
			LOG.trace("Property is null for key "+key);
			element.html("");
		} else if (property instanceof Map) {
			LOG.trace("Injecting map");
			context.modelContext = new ModelContext(context.modelContext, key);
			processChildren(context);
		} else if (property instanceof Collection) {
			LOG.trace("Injecting collection");
			Element lastElement = element;
			List list = new ArrayList((Collection)property);
			for (int i = 0; i < list.size(); i++) {
				Object item = list.get(i);
				Element newElement = element.clone();
				lastElement.after(newElement);
				lastElement = newElement;
				if (isBasicType(item)) {
					newElement.html(cleanAndParagraph(item.toString()));
				} else {
					DocumentContext itemCtx = new DocumentContext(context, newElement, new ModelContext(context.modelContext, key + "." + String.valueOf(i)));
					processChildren(itemCtx);
				}
			}
			element.remove();
		} else if (isBasicType(property)) {
			LOG.trace("Injecting primitive");
			element.html(cleanAndParagraph(property.toString()));
		} else {
			LOG.trace("Injecting object");
			if (element.children().isEmpty()) {
				LOG.trace("Element has no children, injecting raw");
				element.html(cleanAndParagraph(property.toString()));
			} else {
				LOG.trace("Element has children, injecting & reprocessing");
				context.modelContext = new ModelContext(context.modelContext, key);
				processChildren(context);
			}
		}
	}

	private void handleSubstitution(DocumentContext context) {
		LOG.trace("Handling element-level substitution");
		Element rootElement = context.currentNode;
		propLookup.setContext(context);
		StrSubstitutor ss = new StrSubstitutor(propLookup);

		rootElement.html(ss.replace(rootElement.html()));
		for (Attribute a : rootElement.attributes()) {
			a.setValue(ss.replace(a.getValue()));
		}
	}

	private void handleSubstitution() {
		LOG.info("Handling substitution");
		propLookup.setContext(rootContext);
		StrSubstitutor ss = new StrSubstitutor(propLookup);
		String text = ss.replace(outDoc.html());
		outDoc = Jsoup.parse(text);
	}

	private String getOutput() {
		if (prettyPrint) {
			outDoc.outputSettings().indentAmount(4);
		}
		outDoc.outputSettings().prettyPrint(prettyPrint);
		return DOCTYPE_HTML5 + outDoc.ownerDocument().outerHtml();
	}

	private void writeOut(HttpServletResponse response) throws IOException {
		LOG.info("Flushing output");
		result = getOutput();
		response.setContentType("text/html;charset=utf-8");
		response.getWriter().write(result);
	}

	private void writeOut(Writer out) throws IOException {
		LOG.info("Flushing output");
		result = getOutput();
		out.write(result);
	}

	private boolean isBasicType(Object o) {
		return o instanceof String
				|| o instanceof Byte || o instanceof Short
				|| o instanceof Integer || o instanceof Long
				|| o instanceof Float || o instanceof Double
				|| o instanceof Boolean || o instanceof Character
				|| o instanceof BigInteger || o instanceof BigDecimal
				|| o instanceof java.util.Date || o instanceof java.sql.Date;
	}

	private File findTemplateFile(String name) {
		File f = new File(name);
		if (f.isAbsolute()) {
			return f.exists() ? f : null;
		} else {
			// Check in master template path, if defined
			if (templateRoot != null) {
				f = new File(templateRoot + File.separatorChar + name);
				if (f.exists()) return f;
			}

			// Check in site root path, if defined
			if (siteRoot != null) {
				f = new File(siteRoot + File.separatorChar + name);
				if (f.exists()) return f;
			}

			// Check in template directory
			f = new File(templateFile.getParent() + File.separatorChar + name);
			return f.exists() ? f : null;
		}
	}

	private String cleanHtml(String html) {
		return StringEscapeUtils.escapeXml(html);
	}

	private String newlineToParagraph(String text) {
		if (text.matches("[\\n\\r]")) {
			return text.replaceAll("(?!$|[\\n\\r])+([^\\n\\r]+)(?![\\n\\r]+|^)", "<p>$1</p>");
		} else {
			return text;
		}
	}

	private String cleanAndParagraph(String text) {
		return newlineToParagraph(cleanHtml(text));
	}

	public String getTemplateRoot() {
		return templateRoot;
	}

	public void setTemplateRoot(String templateRoot) {
		if (!new File(templateRoot).isAbsolute()) {
			this.templateRoot = templateFile.getParent() + File.separatorChar + templateRoot;
		} else {
			this.templateRoot = templateRoot;
		}
	}

	public String getSiteRoot() {
		return siteRoot;
	}

	public void setSiteRoot(String siteRoot) {
		this.siteRoot = siteRoot;
	}

	public boolean isPrettyPrint() {
		return prettyPrint;
	}

	public void setPrettyPrint(boolean prettyPrint) {
		this.prettyPrint = prettyPrint;
	}
}
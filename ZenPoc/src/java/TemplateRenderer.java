
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.Writer;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Collection;
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

	File templateFile;
	Document inDoc, outDoc;
	String result, templateRoot = null, siteRoot = null;
	PropertyLookup propLookup;

	public TemplateRenderer(String templatePath, ServletContext servletContext) {
		this(servletContext.getRealPath(templatePath));
	}

	public TemplateRenderer(String templatePath) {
		this(new File(templatePath));
	}

	public TemplateRenderer(File templateFile) {
		this.templateFile = templateFile;
		result = "";
		propLookup = new PropertyLookup();
	}

	void addProperty(String name, Object value) {
		propLookup.addProperty(name, value);
	}

	void addProperties(Map<String,Object> properties) {
		propLookup.addProperties(properties);
	}

	void render(HttpServletResponse response) throws IOException {
		handleRendering();
		writeOut(response);
	}

	void render(Writer out) throws IOException {
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
		outDoc = new Document(templateFile.getParent());
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
			}
		} else {
			outDoc = inDoc.clone();
		}
	}

	private void handleInjection() {
		handleInjection(outDoc);
	}

	private void handleInjection(Element rootElement) {
		LOG.info("Handling injection for "+rootElement.tagName());

		for (Element e : rootElement.getElementsByAttribute("data-z-snippet")) {
			String snippetName = e.dataset().get("z-snippet");
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
		}

		Elements dummies = rootElement.getElementsByClass("z-lorem");
		for (Element e: dummies) {
			e.remove();
		}

		for (Element e : rootElement.getElementsByAttribute("data-z-if")) {
			String expression = e.dataset().get("z-if").trim();
			if (!propLookup.lookupBoolean(expression)) {
				e.remove();
			}
		}

		Elements candInject = rootElement.getElementsByAttribute("class");
		candInject.addAll(rootElement.getElementsByAttribute("id"));
		candInject.removeAll(rootElement.getElementsByAttribute("data-z-no-inject"));
		candInject.addAll(rootElement.getElementsByAttribute("data-z-inject"));
		candInject.remove(rootElement);
		LOG.trace("Injection candidates: "+candInject.size());
		for (Element e: candInject) {
			if (e.hasAttr("data-z-inject")) {
				inject(e, e.dataset().get("z-inject"));
			} else {
				String firstClass;
				if (e.className().isEmpty()) {
					firstClass = null;
				} else {
					firstClass = e.className().split(" ", 2)[0];
				}

				if (!e.id().isEmpty() && propLookup.hasProperty(e.id())) {
					inject(e, e.id());
					break;
				} else if (firstClass != null && propLookup.hasProperty(firstClass)) {
					inject(e, firstClass);
					break;
				}
			}
		}
	}

	private void inject(Element element, String key) {
		LOG.debug("Injecting key: "+key);
		Object property = propLookup.lookupProperty(key);
		if (property instanceof Map) property = ((Map)property).values();
		LOG.trace(key+" is "+(property == null ? "null" : "a "+property.getClass()));

		if (property == null) {
			LOG.trace("Property is null for key "+key);
			element.html("");
		} else if (property instanceof Map) {
			LOG.trace("Injecting map");
			processElement(key, element);
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
					processElement(new String[] {key, String.valueOf(i)}, newElement);
				}
			}
			element.remove();
		} else if (isBasicType(property)) {
			LOG.trace("Injecting primitive");
			element.html(cleanAndParagraph(property.toString()));
		} else {
			LOG.trace("Injecting object");
			processElement(key, element);
		}
	}

	private boolean isBasicType(Object o) {
		return o instanceof String
				|| o instanceof Byte || o instanceof Short
				|| o instanceof Integer || o instanceof Long
				|| o instanceof Float || o instanceof Double
				|| o instanceof Boolean || o instanceof Character
				|| o instanceof BigInteger || o instanceof BigDecimal;
	}

	private void processElement(String scope, Element element) {
		processElement(new String[] {scope}, element);
	}

	private void processElement(String[] scope, Element element) {
		for (String scopePart : scope) {
			propLookup.pushScope(scopePart);
		}
		handleInjection(element);
		handleSubstitution(element);
		for (String scopePart : scope) {
			propLookup.popScope(scopePart);
		}
	}

	private void handleSubstitution() {
		LOG.info("Handling substitution");
		StrSubstitutor ss = new StrSubstitutor(propLookup);
		String text = ss.replace(outDoc.html());
		outDoc = Jsoup.parse(text);
	}

	private void handleSubstitution(Element rootElement) {
		LOG.info("Handling element-level substitution");
		StrSubstitutor ss = new StrSubstitutor(propLookup);
		String text = ss.replace(rootElement.outerHtml());
		Document fragment = Jsoup.parseBodyFragment(text);
		Element processed = fragment.body().child(0);
		rootElement.html(processed.html());
		for (Attribute attr: processed.attributes()) {
			rootElement.attr(attr.getKey(), attr.getValue());
		}
	}

	private void writeOut(HttpServletResponse response) throws IOException {
		LOG.info("Flushing output");
		outDoc.outputSettings().indentAmount(4);
		result = outDoc.ownerDocument().html();
		response.setContentType("text/html;charset=utf-8");
		response.getWriter().write(result);
	}

	private void writeOut(Writer out) throws IOException {
		LOG.info("Flushing output");
		outDoc.outputSettings().indentAmount(4);
		result = outDoc.ownerDocument().html();
		out.write(result);
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
}
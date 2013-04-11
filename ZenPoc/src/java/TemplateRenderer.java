
import java.io.File;
import java.io.IOException;
import java.io.Writer;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import javax.servlet.ServletContext;
import javax.servlet.http.HttpServletResponse;
import org.apache.commons.lang3.StringEscapeUtils;
import org.apache.commons.lang3.text.StrSubstitutor;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

/**
 *
 * @author Adrian
 */
public class TemplateRenderer {
	File templateFile;
	Document inDoc, outDoc;
	Map<String, Object> model = new HashMap<String, Object>();
	String result, templateRoot = null, siteRoot = null;

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

	void setProperty(String name, Object value) {
		model.put(name, value);
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
		System.out.println("Rendering template");
		loadTemplate();
		handleDerivation();
		handleInjection();
		handleSubstitution();
	}

	private void loadTemplate() throws IOException {
		System.out.println("Loading template: " + templateFile);
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
					File derivedFile = findDerivedFile(parent + ".html");
					if (derivedFile != null) {
						derivesFrom = derivedFile.getPath();
						break;
					}
				}
			}
		} else {
			File derivedFile = findDerivedFile(derivesFrom);
			derivesFrom = (derivedFile == null) ? null : derivedFile.getPath();
		}

		if (derivesFrom != null && !derivesFrom.isEmpty()) {
			System.out.println("Handling derivation: " + derivesFrom);

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
					System.out.println("Notice: no parent found for appendix: "+inAppendix.toString());
					continue;
				}

				String[] appendixData = inAppendix.dataset().get("z-append").split(":",2);

				Element outSibling = outParent.getElementById(appendixData[1]);
				if (outSibling == null) {
					System.out.println("Notice: sibling not found: "+appendixData[1]);
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
		System.out.println("Handling injection");
		for (String key : model.keySet()) {
			Elements outMatches = outDoc.getElementsByClass(key);
			for (Element outMatch : outMatches) {
				if (model.get(key) instanceof Collection) {
					Element lastElement = outMatch;
					for (Object item : ((Collection)model.get(key))) {
						Element newElement = outMatch.clone();
						newElement.html(cleanAndParagraph(item.toString()));
						lastElement.after(newElement);
						lastElement = newElement;
					}
					outMatch.remove();
				} else {
					outMatch.html(cleanAndParagraph(model.get(key).toString()));
				}
			}
		}
	}

	private void handleSubstitution() {
		System.out.println("Handling substitution");
		StrSubstitutor ss = new StrSubstitutor(model);
		String text = ss.replace(outDoc.html());
		// TODO: cleanHtml(model.get(key).toString()));
		// Requires implementing a custom StrLookup or decorating an existing StrLookup

		outDoc = Jsoup.parse(text);
	}

	private void writeOut(HttpServletResponse response) throws IOException {
		System.out.println("Flushing output");
		outDoc.outputSettings().indentAmount(4);
		result = outDoc.ownerDocument().html();
		response.setContentType("text/html;charset=utf-8");
		response.getWriter().write(result);
	}

	private void writeOut(Writer out) throws IOException {
		System.out.println("Flushing output");
		outDoc.outputSettings().indentAmount(4);
		result = outDoc.ownerDocument().html();
		out.write(result);
	}

	private File findDerivedFile(String name) {
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
		String out = html.trim().replaceAll("[ \\t]+", " ");
		return StringEscapeUtils.escapeXml(out);
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
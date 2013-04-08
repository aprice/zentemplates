
import java.io.File;
import java.io.IOException;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.servlet.ServletContext;
import javax.servlet.http.HttpServletResponse;
import org.apache.commons.lang3.StringEscapeUtils;
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
	String result;

	TemplateRenderer(String templatePath, ServletContext servletContext) {
		this(servletContext.getRealPath(templatePath));
	}

	private TemplateRenderer(String templatePath) {
		templateFile = new File(templatePath);
		result = "";
	}

	void setProperty(String name, Object value) {
		model.put(name, value);
	}

	void render(HttpServletResponse response) throws IOException {
		long start = System.currentTimeMillis();
		loadTemplate();
		handleDerivation();
		handleInjection();
		handleSubstitution();
		writeOut(response);
		long end = System.currentTimeMillis();
		System.out.println("Rendering time: "+(end - start)+" ms.");
	}

	private void loadTemplate() throws IOException {
		System.out.println("Loading template :" + templateFile);
		long start = System.currentTimeMillis();
		inDoc = Jsoup.parse(templateFile, "UTF-8");
		outDoc = new Document(templateFile.getParent());
		long end = System.currentTimeMillis();
		System.out.println("Load time: "+(end - start)+" ms.");
	}

	private void handleDerivation() throws IOException {
		long start = System.currentTimeMillis();
		Element inHtml = inDoc.child(0);
		String derivesFrom = inHtml.attr("class");
		if (derivesFrom.contains("_derivesFrom(")) {
			System.out.println("Handling derivation: " + derivesFrom);
			// Get parent file name
			Pattern pat = Pattern.compile("_derivesFrom\\('?([^\\)']+)'?\\)");
			Matcher match = pat.matcher(derivesFrom);
			if (match.matches()) {
				String parent = match.group(1);
				if (!(new File(parent).isAbsolute())) {
					parent = templateFile.getParent() + File.separator + parent;
				}

				// Handle nested derivation
				TemplateRenderer parentRenderer = new TemplateRenderer(parent);
				parentRenderer.loadTemplate();
				parentRenderer.handleDerivation();
				outDoc = parentRenderer.outDoc;

				// Handle derived elements
				Elements placeholders = outDoc.getElementsByAttribute("id");
				for (Element placeholder : placeholders) {
					Element replacement = inDoc.getElementById(placeholder.id());
					if (replacement != null) {
						placeholder.replaceWith(replacement);
						if (placeholder.id().endsWith("?")) {
							placeholder.attr("id", placeholder.id().substring(0, placeholder.id().length()-1));
						}
					} else if (placeholder.id().endsWith("?")) {
						placeholder.remove();
					}
				}
			}
		} else {
			outDoc = inDoc.clone();
		}
		long end = System.currentTimeMillis();
		System.out.println("Derivation time: "+(end - start)+" ms.");
	}

	private void handleInjection() {
		System.out.println("Handling injection");
		long start = System.currentTimeMillis();
		for (String key : model.keySet()) {
			Elements matches = outDoc.getElementsByClass(key);
			for (Element match : matches) {
				if (model.get(key) instanceof Collection) {
					Element lastElement = match;
					for (Object item : ((Collection)model.get(key))) {
						Element newElement = match.clone();
						newElement.html(cleanAndParagraph(item.toString()));
						lastElement.after(newElement);
						lastElement = newElement;
					}
					match.remove();
				} else {
					match.html(cleanAndParagraph(model.get(key).toString()));
				}
			}
		}
		long end = System.currentTimeMillis();
		System.out.println("Injection time: "+(end - start)+" ms.");
	}

	private void handleSubstitution() {
		System.out.println("Handling substitution");
		long start = System.currentTimeMillis();
		String text = outDoc.html();
		for (String key : model.keySet()) {
			text = text.replaceAll("\\$\\{"+key+"\\}", cleanHtml(model.get(key).toString()));
		}
		outDoc = Jsoup.parse(text);
		long end = System.currentTimeMillis();
		System.out.println("Substitution time: "+(end - start)+" ms.");
	}

	private void writeOut(HttpServletResponse response) throws IOException {
		System.out.println("Flushing output");
		long start = System.currentTimeMillis();
		outDoc.outputSettings().indentAmount(4);
		result = outDoc.ownerDocument().html();
		response.setContentType("text/html;charset=utf-8");
		response.getWriter().write(result);
		long end = System.currentTimeMillis();
		System.out.println("Write time: "+(end - start)+" ms.");
	}

	private String cleanHtml(String html) {
		String out = html.replaceAll("[ \\t]+", " ");
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
}
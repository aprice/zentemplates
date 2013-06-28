package zenpoc;


import org.jsoup.nodes.Element;


/**
 *
 * @author Adrian
 */
public interface InjectionMethod {
	Element handleInjection(String arg, DocumentContext docContext);
}
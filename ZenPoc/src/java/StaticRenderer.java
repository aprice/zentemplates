
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Collections;
import java.util.Map;
import net.sf.json.JSONObject;
import org.apache.commons.io.IOUtils;
import org.apache.log4j.Logger;

/**
 *
 * @author Adrian
 */
public class StaticRenderer {
	private static final Logger LOG = Logger.getLogger(StaticRenderer.class);
	private static final String INPUT_DIR = "E:\\Development\\Zen Rogue\\templates\\";
	private static final String OUTPUT_DIR = "E:\\Development\\Zen Rogue\\rendered\\";

	public static void main(String... args) {
		String inputPath = args.length > 0 ? args[0] : INPUT_DIR;
		String outputPath = args.length > 1 ? args[1] : OUTPUT_DIR;
		File inputDir = new File(inputPath);
		File outputDir = new File(outputPath);
		File globalJsonFile = new File(inputPath + "global.json");
		Map model = Collections.EMPTY_MAP;
		if (globalJsonFile.exists()) {
			String globalJson = null;
			try {
				globalJson = IOUtils.toString(new FileReader(globalJsonFile));
			} catch (IOException ex) {
				LOG.warn("Unable to read global.json");
			}

			if (globalJson != null) {
				model = JSONObject.fromObject(globalJson);
			}
		}

		StaticRenderer sr = new StaticRenderer(inputDir, outputDir, model);
		sr.run();
	}

	private final File inputDir, outputDir;
	private Map<String,Object> model;
	//private String templateRoot = null;
	private String templateRoot = INPUT_DIR + "tpl";
	private int pageCount = 0;

	private StaticRenderer(File inputDir, File outputDir, Map<String,Object> model) {
		this.inputDir = inputDir;
		this.outputDir = outputDir;
		this.model = model;
	}

	private void run() {
		processDirectory(inputDir);
		LOG.info("Procssed "+pageCount+" pages.");
	}

	private void processDirectory(File inDir) {
		if (inDir.getPath().contains(templateRoot)) return;

		long dStart = System.nanoTime();
		for (File inFile : inDir.listFiles()) {
			if (inFile.isDirectory()) {
				processDirectory(inFile);
			} else if (!inFile.getName().endsWith(".html")) {
				continue;
			} else {
				LOG.info("Processing file: " + inFile.getPath());
				pageCount++;
				long fStart = System.nanoTime();
				TemplateRenderer renderer = new TemplateRenderer(inFile);
				renderer.setTemplateRoot(templateRoot);
				renderer.addProperties(model);
				String relativePath = inFile.getPath().substring(inputDir.getPath().length());
				String outputPath = outputDir.getPath() + File.separatorChar + relativePath;
				File outFile = new File(outputPath);
				outFile.getParentFile().mkdirs();
				try {
					FileWriter fw = new FileWriter(outFile);
					renderer.render(fw);
					fw.flush();
				} catch (IOException ex) {
					LOG.error("Failed to render template file " + outFile.getPath() + ": " + ex.getMessage());
					ex.printStackTrace();
				}
				long fEnd = System.nanoTime();
				LOG.info(String.format("%s completed in %d ms.", inFile.getPath(), (fEnd - fStart) / 1000000));
			}
		}

		long dEnd = System.nanoTime();
		LOG.info(String.format("%s completed in %d ms.", inDir.getPath(), (dEnd - dStart) / 1000000));
	}
}
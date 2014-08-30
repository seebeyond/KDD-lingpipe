package xinghuangxu.lingpipe.snippet.extractor;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import javax.xml.parsers.ParserConfigurationException;

import org.xml.sax.SAXException;

import xinghuangxu.lingpipe.sentiment.logging.Log;
import xinghuangxu.lingpipe.tutorial.sentiment.PolarityBasic;

/**
 * Main entrance
 * 
 * @author xinghuang zhang
 * 
 * 
 */
public class Extractor {

	private final String dir = getPath("/Sentiment_DB");
	// private final String sentimetDir="/Sentiment_DB";
	private final String reviewDir = dir + "/Review_DB";
	private final String trainingDir = dir + "/Training_DB";
	private final String logDir = dir + "/Log_DB";

	private Extractor() {
		// TODO Auto-generated constructor stub
	}

	public String getPath(String FileName) {
		String current;
		try {
			current = new File(".").getCanonicalPath();
		} catch (IOException e) {
			return "";
		}
		// return current + DIR_NAME + "\\"+CORPUS_DIR_NAME+"\\" +
		// CORPUS_FILE_NAME;
		return current + FileName;
	}

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		Extractor extractor = new Extractor();
		try {

			extractor.run(args);
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

	}

	public void run(String[] args) throws Exception {

		Log.createFolder(logDir);

		if (args.length < 1) {
			System.out.println("Usage: - l + r");
			return;
		}

		// Create Folders
		File sentimentFolder = new File(dir);
		sentimentFolder.mkdir();
		File reviewDB = new File(reviewDir);
		reviewDB.mkdir();
		File logDB = new File(logDir);
		logDB.mkdir();
		File trainingDB = new File(trainingDir);
		trainingDB.mkdir();

		int left = 0;
		int right = 0;
		
		for (int i = 0; i < args.length; i++) {
			if ("-".equals(args[i])) {
				left = Integer.parseInt(args[i + 1]);
				i++;
			} else if ("+".equals(args[i])) {
				right = Integer.parseInt(args[i + 1]);
				i++;
			}
		}
		
		String logFolderName = "(" + left + "," + right + ")-";
		Log.createInstance(logDir + "/" + logFolderName);
		Log.createFile("SentimentArgs");
		
		Log.info("SnippetSize: " + "-" + left + " +" + right);
		
		// Initialize Simple Demo Training. Currently the IMDB movie reviews data is used
		Log.info("\nBASIC POLARITY DEMO");
		Log.info("Training Data Directory=" + trainingDir);
		PolarityBasic.create(trainingDir);

		// Load all the review folders and process all the files in them
		// One review Folder per time
		File[] reviewFolders = reviewDB.listFiles();
		for (File reviewFolder : reviewFolders) {
			runSentimentDemo(reviewFolder, left, right);
		}

	}

	/**
	 * Process one folder of reviews + apsects 
	 * (they should be kept in one folder and the difference between review files and aspect files: 
	 * aspect files have names include string "aspect"
	 * 
	 * @param reviewFolder
	 * @param left
	 * @param right
	 * @throws ClassNotFoundException
	 * @throws IOException
	 * @throws ParserConfigurationException
	 * @throws SAXException
	 */
	private void runSentimentDemo(File reviewFolder, int left, int right)
			throws ClassNotFoundException, IOException,
			ParserConfigurationException, SAXException {

		File[] reviewFiles = reviewFolder.listFiles();
		List<File> aspects = new ArrayList<File>();
		List<File> reviews = new ArrayList<File>();
		for (File file : reviewFiles) {
			if (file.getName().endsWith(".xml")) {
				if (file.getName().toLowerCase().contains("aspect")) {
					aspects.add(file);
				} else
					reviews.add(file);
			}
		}
		Log.createFile(reviewFolder.getName() + "(" + left + "," + right + ")");

		// Load all the aspects
		Log.info("\n\n///////////////////////////////////////////////////////////////////////////");
		Log.info("\nLoad Aspects From Directory: " + reviewFolder);
		// File[] aspFiles = aspFolder.listFiles();
		AspDictionary aspDictionary = XMLParser.getAspectsDictionary(aspects
				.toArray(new File[aspects.size()]));
		Log.info("Aspect Size: " + aspDictionary.size());

		for (File reviewFile : reviews) {
			// Load single reviews
			Review review = XMLParser.getReviews(reviewFile);
			Log.info("\nLoad Reviews From File: " + reviewFile);
			Log.info("Review Size: " + review.size());

			// Create Snippet Dictionary acording to (- m + n)
			Log.info("\nCreate Snippet Dictionary");
			SnippetFactory snippetFactory = SnippetFactory.newInstance();
			SnippetDictionary snippetDictionary = snippetFactory
					.newSnippetDictionary(reviewFile.getName(), review,
							aspDictionary);
			snippetDictionary.trimAll(left, right);
			Log.info("Snippet Dictionary Size: " + snippetDictionary.size());
			Log.info(snippetDictionary.toString());
			Log.info("Snippet Size: " + snippetDictionary.snippetSize());
			
			// Log.info(snippetDictionary.toString());

			// Run basic polarity
			PolarityBasic.setSnippetDictionary(snippetDictionary);
			PolarityBasic.run();
			
			Log.infoCSV(snippetDictionary.toStringCSV());
		}

		return;

	}

}

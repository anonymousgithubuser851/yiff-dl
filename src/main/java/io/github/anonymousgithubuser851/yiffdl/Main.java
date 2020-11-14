package io.github.anonymousgithubuser851.yiffdl;

import net.sourceforge.argparse4j.ArgumentParsers;
import net.sourceforge.argparse4j.inf.ArgumentParser;
import net.sourceforge.argparse4j.inf.ArgumentParserException;
import net.sourceforge.argparse4j.inf.Namespace;

public class Main {
	public static final String NAME = "yiff-dl";
	public static final String VERSION = "1.0";
	private static boolean DEBUG_MODE = false;
	public static String FFMPEG = "";

	public static void main(String[] args) {
		Log.out("Thank you for using " + NAME + "! (ver " + VERSION + ")");
		ArgumentParser parser = ArgumentParsers.newFor(NAME).build()
				.defaultHelp(true)
				.description("Download videos from supported sites.");
		parser.addArgument("--debug").choices(true, false).setDefault(false)
				.help("shows stacktraces for errors when yiff-dl had special handling");
		parser.addArgument("--ffmpeg").required(false)
				.help("sets a specific ffmpeg binary");
		parser.addArgument("url").required(true)
				.help("URL to download from");


		Namespace ns = null;
		try {
			ns = parser.parseArgs(args);
		} catch (ArgumentParserException e) {
			parser.handleError(e);
			System.exit(1);
		}

		String url = ns.getString("url");
		DEBUG_MODE = ns.getBoolean("debug");
		FFMPEG = ns.getString("ffmpeg");
		if(FFMPEG == null) {
			FFMPEG = "ffmpeg";
		}

		try {
			new YiffDl(url, System.getProperty("user.dir"));
		} catch (Exception e) {
			if (!(e.getCause() instanceof DoNotShowStacktrace || e instanceof DoNotShowStacktrace) || DEBUG_MODE)
				e.printStackTrace();

			Log.error("Could not download video. Check above for details.");
			System.exit(1);
		}
	}
}

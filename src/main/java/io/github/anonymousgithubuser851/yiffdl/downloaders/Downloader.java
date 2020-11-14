package io.github.anonymousgithubuser851.yiffdl.downloaders;

import io.github.anonymousgithubuser851.yiffdl.Log;
import io.github.anonymousgithubuser851.yiffdl.Main;
import me.tongfei.progressbar.ProgressBar;
import me.tongfei.progressbar.ProgressBarStyle;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.time.Duration;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.stream.Collectors;

public abstract class Downloader {
	private final String url;

	public Downloader(String url) throws Exception {
		this.url = url;
		log("Downloading from URL " + url);
	}

	protected abstract String getName();

	public abstract void download(String downloadDir) throws Exception;

	protected String getUrl() {
		return url;
	}

	private String format(String s) {
		String tag = "[" + getName() + "] ";
		s = tag + s.replace("\n", "\n" + tag);
		return s;
	}

	protected void log(String s) {
		Log.out(format(s));
	}

	protected void error(String s) {
		Log.error(format(s));
	}

	protected String durationString(int secs) { //secs x3333
		int minutes = secs / 60;
		int hours = minutes / 60;

		minutes = minutes % 60;
		secs = secs % 60;

		StringBuilder time = new StringBuilder();

		if (hours > 0)
			time.append(hours).append("h");
		if (minutes > 0)
			time.append(minutes).append("m");
		time.append(secs).append("s");

		return time.toString();
	}

	protected ProgressBar progressBar(String task, int size) {
		return new ProgressBar(format(task), size, 250, System.out, ProgressBarStyle.ASCII, "", 1L, false, null, ChronoUnit.SECONDS, 0L, Duration.ZERO);
	}

	protected String ffmpeg(String... args) throws Exception {
		ArrayList<String> list = new ArrayList<>();
		list.add(Main.FFMPEG);
		list.addAll(Arrays.asList(args));

		Process p = new ProcessBuilder(list.toArray(new String[0]))
				.redirectErrorStream(true)
				.start();

		String output;
		try (BufferedReader reader = new BufferedReader(new InputStreamReader(p.getErrorStream()))) {
			output = reader.lines().collect(Collectors.joining("\n"));
		}

		p.waitFor();

		return output;
	}
}

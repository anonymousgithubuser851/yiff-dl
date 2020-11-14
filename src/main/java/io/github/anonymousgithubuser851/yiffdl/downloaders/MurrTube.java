package io.github.anonymousgithubuser851.yiffdl.downloaders;

import me.tongfei.progressbar.ProgressBar;
import io.github.anonymousgithubuser851.yiffdl.DoNotShowStacktrace;
import io.github.anonymousgithubuser851.yiffdl.Main;
import org.json.JSONObject;
import org.json.JSONTokener;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLConnection;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

public class MurrTube extends Downloader {
	private static final String STORAGE_URL = "https://storage.murrtube.net/murrtube/";
	private static final String INFO_URL = "https://murrtube.net/graphql";
	//I have no idea where to put this, but I have no idea what "slug" means. If I had to guess, it's "simple legible" something something? Some acronym for readable IDs? Dunno.
	private static final String NEEDED_INFO = "{\"operationName\":\"Medium\",\"variables\":{\"id\":\"%s\"},\"query\":\"query Medium($id:ID!){medium(id:$id){slug title key duration}}\"}";

	private final String id;

	public MurrTube(String url) throws Exception {
		super(url);

		if (!url.contains("/videos/")) {
			error("Given URL does not seem to be a valid URL to a MurrTube video.");
			throw new DoNotShowStacktrace();
		}

		//remove everything from the URL except title and ID, so .../videos/title-of-vid-0000/... becomes title-of-vid-0000
		url = url.substring(url.indexOf("murrtube.net") + "murrtube.net".length());
		url = url.substring("/videos/".length());
		url = url.substring(0, url.contains("/") ? url.indexOf("/") : url.length()); //remove / and anything after, if applicable
		url = url.substring(0, url.contains("?") ? url.indexOf("?") : url.length()); //remove ? and anything after, if applicable (i've never seen this in a URL for this site, but just in case)

		id = url.substring(url.length() - 36); //last 36 is ID

		log("Extracted ID: " + id);
	}

	public void download(String downloadDir) throws Exception {
		log("Getting video info...");
		URL infoUrl = new URL(INFO_URL);

		HttpURLConnection infoConnection = (HttpURLConnection) infoUrl.openConnection();
		infoConnection.setRequestProperty("Content-Type", "application/json");
		infoConnection.setRequestProperty("user-agent", Main.NAME + "/" + Main.VERSION);
		infoConnection.setDoOutput(true);

		try (BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(infoConnection.getOutputStream()))) {
			writer.write(String.format(NEEDED_INFO, id));
			writer.flush();
		}

		JSONObject object;
		try (InputStream stream = infoConnection.getInputStream()) {
			object = new JSONObject(new JSONTokener(stream));
		}

		JSONObject data = object.getJSONObject("data").getJSONObject("medium");

		//it's all the same width! super cool!!
		String title = data.getString("title");
		String fileKey = data.getString("key");
		int duration = data.getInt("duration");
		String fileName = data.getString("slug") + ".ts"; //oops ruined it

		log("Found video \"" + title + "\" (" + durationString(duration) + ")");
		log("Getting download info...");

		HttpURLConnection vidInfoConnection = (HttpURLConnection) new URL(STORAGE_URL + fileKey).openConnection();

		String vidInfo;
		try (InputStream stream = vidInfoConnection.getInputStream()) {
			vidInfo = new BufferedReader(new InputStreamReader(stream)).lines().collect(Collectors.joining("\n"));
		}

		//I tried doing this directly with ffmpeg but it didn't work so i'm doing it myself
		//parse index.m3u8
		String[] vidInfoLines = vidInfo.split("\n");

		String best = "";

		int bandwidth = 0;
		for (int i = 0; i < vidInfoLines.length; i++) {
			String vidInfoLine = vidInfoLines[i];
			if (vidInfoLine.contains("BANDWIDTH")) {
				int newBandwidth = Integer.parseInt(vidInfoLine.substring(vidInfoLine.indexOf("BANDWIDTH=") + "BANDWIDTH=".length()).trim());
				if (newBandwidth > bandwidth) {
					bandwidth = newBandwidth;
					best = vidInfoLines[i + 1];
				}
			}
		}

		if (best.isEmpty()) {
			error("Could not find stream to download from!");
			throw new DoNotShowStacktrace();
		}

		//parse actual stream m3u8
		String streamInfoUrl = STORAGE_URL + fileKey.substring(0, fileKey.lastIndexOf("/") + 1) + best;

		String streamInfo;
		try (InputStream stream = new URL(streamInfoUrl).openConnection().getInputStream()) {
			streamInfo = new BufferedReader(new InputStreamReader(stream)).lines().collect(Collectors.joining("\n"));
		}

		List<String> fileUrls = Arrays.stream(streamInfo.split("\n"))
				.filter(s -> s.endsWith(".ts"))
				.map(s -> streamInfoUrl.substring(0, streamInfoUrl.lastIndexOf("/") + 1) + s)
				.collect(Collectors.toList());

		Path tempDir = Paths.get(downloadDir + "/" + id);

		int starting = 1;
		if (Files.exists(tempDir)) {
			for (Path path : Files.list(tempDir).collect(Collectors.toList())) {
				String pathString = path.toString();
				try {
					int num = Integer.parseInt(pathString.substring(pathString.lastIndexOf("/") + 1, pathString.lastIndexOf(".")));
					if (starting < num)
						starting = num;
				} catch (Exception ignored) {

				}
			}
		} else {
			Files.createDirectory(tempDir);
		}

		log("Downloading video...");

		starting -= 1; //in case the file was incomplete

		if(starting > 0)
		log("Resuming previous download...");

		try (ProgressBar bar = progressBar("D/L: ", 100)) {
			int percentPerFile = 100 / fileUrls.size();


			for (int i = starting; i < fileUrls.size(); i++) {
				String fileUrl = fileUrls.get(i);
				Path filePath = Paths.get(tempDir + "/" + i + ".ts");
				int largePercent = (i * 100) / fileUrls.size();
				bar.stepTo(largePercent);

				URLConnection connection = new URL(fileUrl).openConnection();

				long totalBytes = connection.getContentLength();
				long remainingBytes = totalBytes;
				try (BufferedInputStream in = new BufferedInputStream(connection.getInputStream());
					 OutputStream fileOutputStream = Files.newOutputStream(filePath, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING)) {
					byte[] dataBuffer = new byte[8192];
					int bytesRead;
					while ((bytesRead = in.read(dataBuffer, 0, 1024)) != -1) {
						fileOutputStream.write(dataBuffer, 0, bytesRead);
						remainingBytes -= bytesRead;

						int smallPercent = (int) ((((totalBytes - remainingBytes) * 100) / totalBytes) * percentPerFile) / 100;
						bar.stepTo(largePercent + smallPercent);
					}
				}
			}
			bar.stepTo(100);
		}

		log("Combining to a single video file, this may take a few seconds...");

		String files = IntStream.range(0, fileUrls.size()).boxed().map(i -> "file '" + i + ".ts'").collect(Collectors.joining("\n"));

		Path tempFileList = Paths.get(tempDir + "/list.txt");
		try (BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(Files.newOutputStream(tempFileList)))) {
			writer.write(files);
			writer.flush();
		}

		Path finishedPath = Paths.get(fileName);
		Files.deleteIfExists(finishedPath);

		String ffmpegOutput = ffmpeg("-f", "concat", "-i", tempFileList.toString(), "-c", "copy", fileName);


		if (!Files.exists(finishedPath)) {
			log("Something must've gone wrong with ffmpeg :(");
			error(ffmpegOutput);
		} else {
			log("Deleting temporary files...");
			//noinspection ResultOfMethodCallIgnored
			Files.walk(tempDir)
					.map(Path::toFile)
					.sorted((o1, o2) -> -o1.compareTo(o2))
					.forEach(File::delete);
			log("Download complete.");
		}
	}

	@Override
	protected String getName() {
		return "murrtube";
	}
}

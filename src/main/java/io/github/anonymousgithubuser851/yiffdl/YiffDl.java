package io.github.anonymousgithubuser851.yiffdl;

import io.github.anonymousgithubuser851.yiffdl.downloaders.Downloader;
import io.github.anonymousgithubuser851.yiffdl.downloaders.MurrTube;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

public class YiffDl {
	private static final Map<String, Class<? extends Downloader>> DOWNLOADERS;
	static {
		HashMap<String, Class<? extends Downloader>> map = new HashMap<>();

		map.put("murrtube.net", MurrTube.class);

		DOWNLOADERS = Collections.unmodifiableMap(map);
	}

	public YiffDl(String s, String directory) throws Exception {
		try {
			URL url = new URL(s);
			String domain = url.getHost().toLowerCase();

			if(DOWNLOADERS.containsKey(domain)) {
				Class<? extends Downloader> downloader = DOWNLOADERS.get(domain);
				downloader.getConstructor(String.class).newInstance(s).download(directory);
			} else {
				Log.error("Domain \"" + domain + "\" is unsupported by " + Main.NAME + ".");
			}
		} catch (MalformedURLException e) {
			e.initCause(new DoNotShowStacktrace());
			Log.error("Given string does not seem to be a valid URL!");
			throw e;
		}
	}
}

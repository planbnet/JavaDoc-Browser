package org.planbnet.docbrowser;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.StringTokenizer;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

import javax.activation.MimetypesFileTypeMap;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.google.common.io.ByteStreams;

/**
 * <p>Quick and dirty servlet that does on the fly extraction of javadoc jars
 * in a local maven repository.
 * 
 * <p>Use the context param "repository" to define where the local
 * repository is stored.
 * 
 * <p>The artifact information is parsed from the request path:
 * <i>http://server/docservletcontext/GROUPID/ARTIFACTID/VERSION</i>
 * 
 * <p>There is very little error handling right now, but it works for
 * my purposes
 * 
 */
public class DocServlet extends HttpServlet {

	public String repositoryPath;

	@Override
	public void init() throws ServletException {
		super.init();
		repositoryPath = this.getServletContext().getInitParameter("repository");
		if (repositoryPath == null) {
			throw new ServletException("Context param repository not set");
		}

		if (!new File(repositoryPath).exists()) {
			throw new ServletException("Repository path " + repositoryPath
					+ " does not exist");
		}
	}


	@Override
	protected void doGet(HttpServletRequest req, HttpServletResponse resp)
			throws ServletException, IOException {

		String path = req.getPathInfo();

		if (path == null || path.equals("") || path.equals("/")) {
			resp.sendError(404, "/");
			return;
		}

		if (path.startsWith("/"))
			path = path.substring(1);

		String groupId;
		String artifactId;
		String version;
		String file;

		StringTokenizer strTok = new StringTokenizer(path, "/");
		if (!strTok.hasMoreElements()) {
			resp.sendError(404, path);
			return;
		}
		groupId = strTok.nextToken();
		if (!strTok.hasMoreElements()) {
			resp.sendError(404, path);
			return;
		}
		artifactId = strTok.nextToken();
		if (!strTok.hasMoreElements()) {
			resp.sendError(404, path);
			return;
		}
		version = strTok.nextToken();

		String absoluteJarPath = repositoryPath + File.separator
				+ groupId.replace(".", File.separator) + File.separator
				+ artifactId + File.separator + version + File.separator
				+ artifactId + "-" + version + "-javadoc.jar";

		if (!new File(absoluteJarPath).exists()) {
			resp.sendError(404, path);
			return;
		}

		if (!strTok.hasMoreElements()) {
			resp.sendRedirect(version + "/index.html");
			return;
		} else {
			StringBuilder filePath = new StringBuilder();
			while (strTok.hasMoreElements()) {
				filePath.append(strTok.nextToken());
				if (strTok.hasMoreElements()) {
					filePath.append(File.separator);
				}
			}
			file = filePath.toString();
		}

		JarFile jarFile = new JarFile(absoluteJarPath);
		JarEntry entry = jarFile.getJarEntry(file);

		if (entry == null) {
			resp.sendError(404, path);
			return;
		}

		resp.setContentLength((int) entry.getSize());
		String mimetype = MimetypesFileTypeMap.getDefaultFileTypeMap().getContentType(entry.getName());
		resp.setContentType(mimetype);
		InputStream input = jarFile.getInputStream(entry);
		try {
			ByteStreams.copy(input, resp.getOutputStream());
		} finally {
			input.close();
		}
	}
}

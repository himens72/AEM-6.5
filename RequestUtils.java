package com.brp.aem.nextgen.core.utils;

import java.io.BufferedReader;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import org.apache.commons.lang3.StringUtils;
import org.apache.sling.api.SlingHttpServletRequest;
import org.json.JSONException;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class RequestUtils {

  private static final Logger LOG = LoggerFactory.getLogger(RequestUtils.class);

  private static final String REQUEST_URI_HEADER = "REQUEST-URI";

  private RequestUtils() {}

  /** Checks for the REQUEST-URI header that is added via apache. And that it isn't null */
  public static boolean hasApacheHeader(SlingHttpServletRequest request) {
    String header = request.getHeader(REQUEST_URI_HEADER);

    return StringUtils.isNotEmpty(header) && !StringUtils.equals(header, "(null)");
  }

  /**
   * Gets the uri for the request before apache rewrites were processed. If that isn't available
   * (hitting publish directly) then uses the requestURI
   */
  public static String getOriginalRequestUri(SlingHttpServletRequest request) {
    String uri = request.getHeader(REQUEST_URI_HEADER);

    if (StringUtils.isBlank(uri)) {
      uri = request.getRequestURI();
    }

    return uri;
  }

  /**
   * Checks that the uri from this requests matches the uri for this page.
   *
   * @param pageUrl is expected to be a fully rewritten url for the current page.
   * @param exactMatch decides if /content/{site}/ causes the match to fail or not.
   */
  public static boolean isValidUrl(
      SlingHttpServletRequest request, String pageUrl, boolean exactMatch) {
    String requestUri = RequestUtils.getOriginalRequestUri(request);

    try {
      String pageUri = pageUrl;

      if (!StringUtils.startsWith(pageUrl, "/")) {
        URL url = new URL(pageUrl);
        pageUri = url.getPath();
      }

      if (exactMatch) {
        return StringUtils.equals(requestUri, pageUri);
      } else {
        return StringUtils.endsWith(requestUri, pageUri);
      }
    } catch (MalformedURLException e) {
      LOG.error("MalformedUrl '{}' - Could not process redirect logic", pageUrl, e);
    }

    return false;
  }

  public static JSONObject getJSONPostData(SlingHttpServletRequest req) throws IOException {
    JSONObject jsonPostData = null;
    StringBuffer jb = new StringBuffer();
    String line = null;
    try {
      BufferedReader reader = req.getReader();
      while ((line = reader.readLine()) != null) jb.append(line);
    } catch (IOException e) {
      LOG.error(e.getMessage());
      throw new IOException("Error reading request data");
    }
    try {
      jsonPostData = new JSONObject(jb.toString());
    } catch (JSONException e) {
      // crash and burn
      LOG.error(e.getMessage());
    }

    return jsonPostData;
  }
}

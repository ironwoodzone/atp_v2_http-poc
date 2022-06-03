
import javax.net.ssl.*;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.*;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.X509Certificate;
import java.util.HashMap;
import java.util.Map;
import java.util.zip.GZIPInputStream;

public class HttpUtility {

	
	private InputStream responseStream = null;
	private String url = null;
	private Map<String, String> connectionProps = null;
	private String requestMethod = null;
	private String requestPayload = null;
	private String errorPayload = null;
	private String responsePayload = null;
	private boolean isSecure = false;
	private boolean useGzip = true;
	private int connectionTimeOut = 45;
	private int readTimeOut = 30;
	private int responseCode = 500;
	private boolean useProxy = false;
	private String proxyIp = null;
	private int proxyPort = 0;
	private String proxyUser = null;
	private String proxyPassword = null;
	private boolean disableCertValidation = false;
	private String sSLContext = null;
	private boolean followRedirects = true;
	private boolean convertResponseToString = true;


	public void send() {
		System.setProperty("jsse.enableSNIExtension", "false");

		try{
			
			ByteArrayOutputStream baos = new ByteArrayOutputStream();

			if (null != requestPayload) {
				baos.write((requestPayload).getBytes());
			}

			if (disableCertValidation) {

				// Create a trust manager that does not validate certificate chains
				TrustManager[] trustAllCerts = new TrustManager[] { new X509TrustManager() {
					public java.security.cert.X509Certificate[] getAcceptedIssuers() {
						return new X509Certificate[0];
					}

					public void checkClientTrusted(X509Certificate[] certs, String authType) {
						throw new UnsupportedOperationException();
					}

					public void checkServerTrusted(X509Certificate[] certs, String authType) {
						throw new UnsupportedOperationException();
					}
				} };

				// Install the all-trusting trust manager
				SSLContext sc = SSLContext.getInstance(sSLContext != null ? sSLContext : "SSL");
				sc.init(null, trustAllCerts, new java.security.SecureRandom());
				HttpsURLConnection.setDefaultSSLSocketFactory(sc.getSocketFactory());

				// Create all-trusting host name verifier
				HostnameVerifier allHostsValid = (hostname, session) -> hostname
						.equalsIgnoreCase(session.getPeerHost());

				// Install the all-trusting host verifier
				HttpsURLConnection.setDefaultHostnameVerifier(allHostsValid);
			}

			URL mUrl = new URL(this.url);
			URLConnection ucon = null;

			if (useProxy) {

				Proxy proxyServer = new Proxy(Proxy.Type.HTTP, new InetSocketAddress(proxyIp, proxyPort));
				if (proxyUser != null && proxyPassword != null) {

					Authenticator authenticator = new Authenticator() {
						@Override
						public PasswordAuthentication getPasswordAuthentication() {
							return (new PasswordAuthentication(proxyUser, proxyPassword.toCharArray()));
						}
					};
					Authenticator.setDefault(authenticator);
				}

				ucon = mUrl.openConnection(proxyServer);

			} else {

				ucon = mUrl.openConnection();
			}

			if (!(ucon instanceof HttpURLConnection)) {
				throw new IOException("Service URL [" + mUrl + "] is not an HTTP URL");
			}

			HttpURLConnection con = (HttpURLConnection) ucon;

			con.setConnectTimeout(connectionTimeOut * 1000);
			con.setReadTimeout(readTimeOut * 1000);

			con.setDoOutput(true);
			con.setUseCaches(false);
			con.setDoInput(true);
			con.setInstanceFollowRedirects(followRedirects);

			if (requestMethod != null) {

				con.setRequestMethod(requestMethod);
			} else {

				if (null != requestPayload) {
					con.setRequestMethod("POST");
				} else {
					con.setRequestMethod("GET");
				}
			}

			if (useGzip) {
				con.setRequestProperty("Accept-Encoding", "gzip,deflate");
				con.setRequestProperty("user-agent", "Mozilla(MSIE)");
			}

			if (connectionProps != null) {
				for (Map.Entry<String, String> entry : connectionProps.entrySet()) {
					con.setRequestProperty(entry.getKey(), entry.getValue());
				}
			}

			if ("POST".equals(con.getRequestMethod())) {
				baos.writeTo(con.getOutputStream());
			}

			responseCode = con.getResponseCode();

			if (con.getResponseCode() >= 300) {

				InputStream errorInputStream = con.getErrorStream();

				if (errorInputStream != null) {

					ByteArrayOutputStream baosErr = new ByteArrayOutputStream();

					try {

						byte[] buffer = new byte[1024];
						int length = 0;

						while ((length = errorInputStream.read(buffer)) != -1) {
							baosErr.write(buffer, 0, length);
						}

						errorPayload = new String(baosErr.toByteArray());
						System.out.println("ERROR :: HttpUtility :: send() :: errorPayload : " + errorPayload);

					} finally {
						closeStream(errorInputStream);
						closeStream(baosErr);
					}

				}

				this.setResponseCode(con.getResponseCode());

				throw new IOException("Did not receive successful HTTP response: status code = " + con.getResponseCode()
						+ ", status message = [" + con.getResponseMessage() + "]");
			}

			String encodingHeader = con.getHeaderField("Content-Encoding");

			if (encodingHeader != null && encodingHeader.toLowerCase().contains("gzip")) {
				responseStream = new GZIPInputStream(con.getInputStream());
			} else {
				responseStream = con.getInputStream();

			}	
			if(convertResponseToString) executeResponseStreamProcess(responseStream);

		} catch (IOException | KeyManagementException | NoSuchAlgorithmException e) {
			System.out.println(e);
		}
	}

	public void closeStream(InputStream inputStream) {
		try {
			inputStream.close();
		} catch (Exception e) {
			System.out.println(e);
		}
	}

	public void closeStream(ByteArrayOutputStream byteArrayOutputStream) {
		try {
			byteArrayOutputStream.close();
		} catch (Exception e) {
			System.out.println(e);
		}
	}

	public void executeResponseStreamProcess(InputStream responseStream) throws IOException {
			ByteArrayOutputStream baosRsp = new ByteArrayOutputStream();
			byte[] buffer = new byte[4096];
			int length = 0;
			while ((length = responseStream.read(buffer)) != -1) {
				baosRsp.write(buffer, 0, length);
			}

			responsePayload = new String(baosRsp.toByteArray());
	}

	// #region getters and setters

	/**
	 * returns url
	 *
	 * @return
	 */
	public String getUrl() {
		return url;
	}

	/**
	 * sets url
	 *
	 * @param url
	 */
	public void setUrl(String url) {
		this.url = url;
	}

	/**
	 * add connection properties to a HashMap<String, String>
	 *
	 * @param key
	 * @param value
	 */
	public void addConnectionProp(String key, String value) {
		if (connectionProps == null) {
			connectionProps = new HashMap<>();
		}

		connectionProps.put(key, value);
	}

	/**
	 * return connection properties
	 *
	 * @return HashMap<String, String>
	 */
	public Map<String, String> getConnectionProps() {
		return connectionProps;
	}

	/**
	 * set connection properties
	 *
	 * @param connectionProps HashMap<String, String>
	 */
	public void setConnectionProps(Map<String, String> connectionProps) {
		this.connectionProps = connectionProps;
	}

	/**
	 * returns request method
	 *
	 * @return
	 */
	public String getRequestMethod() {
		return requestMethod;
	}

	/**
	 * sets request method
	 *
	 * @param requestMethod
	 */
	public void setRequestMethod(String requestMethod) {
		this.requestMethod = requestMethod;
	}

	/**
	 * returns request payload
	 *
	 * @return
	 */
	public String getRequestPayload() {
		return requestPayload;
	}

	/**
	 * sets request payload
	 *
	 * @param requestPayload
	 */
	public void setRequestPayload(String requestPayload) {
		this.requestPayload = requestPayload;
	}

	/**
	 * returns response payload
	 *
	 * @return
	 */
	public String getResponsePayload() {
		return responsePayload;
	}

	/**
	 * sets reponse payload
	 *
	 * @param responsePayload
	 */
	public void setResponsePayload(String responsePayload) {
		this.responsePayload = responsePayload;
	}

	/**
	 * returns secure parameter (boolean)
	 *
	 * @return
	 */
	public boolean isSecure() {
		return isSecure;
	}

	/**
	 * sets secure parameter (boolean)
	 *
	 * @param isSecure
	 */
	public void setSecure(boolean isSecure) {
		this.isSecure = isSecure;
	}

	/**
	 * returns gzip parameter (boolean)
	 *
	 * @return
	 */
	public boolean isUseGzip() {
		return useGzip;
	}

	/**
	 * sets gzip parameter (boolean)
	 *
	 * @param useGzip
	 */
	public void setUseGzip(boolean useGzip) {
		this.useGzip = useGzip;
	}

	/**
	 * returns connection timeout property
	 *
	 * @return
	 */
	public int getConnectionTimeOut() {
		return connectionTimeOut;
	}

	/**
	 * sets connection timeout property
	 *
	 * @param connectionTimeOut
	 */
	public void setConnectionTimeOut(int connectionTimeOut) {
		this.connectionTimeOut = connectionTimeOut;
	}

	/**
	 * returns read time out property
	 *
	 * @return
	 */
	public int getReadTimeOut() {
		return readTimeOut;
	}

	/**
	 * sets read time out property
	 *
	 * @param readTimeOut
	 */
	public void setReadTimeOut(int readTimeOut) {
		this.readTimeOut = readTimeOut;
	}

	/**
	 * return proxy parameter (boolean)
	 *
	 * @return
	 */
	public boolean isUseProxy() {
		return useProxy;
	}

	/**
	 * sets proxy parameter (boolean)
	 *
	 * @param useProxy
	 */
	public void setUseProxy(boolean useProxy) {
		this.useProxy = useProxy;
	}

	/**
	 * returns proxy ip
	 */
	public String getProxyIp() {
		return proxyIp;
	}

	/**
	 * sets proxy ip
	 *
	 * @param proxyIp
	 */
	public void setProxyIp(String proxyIp) {
		this.proxyIp = proxyIp;
	}

	/**
	 * returns proxy port
	 *
	 * @return
	 */
	public int getProxyPort() {
		return proxyPort;
	}

	/**
	 * sets proxy port
	 *
	 * @param proxyPort
	 */
	public void setProxyPort(int proxyPort) {
		this.proxyPort = proxyPort;
	}

	/**
	 * returns proxy user
	 *
	 * @return
	 */
	public String getProxyUser() {
		return proxyUser;
	}

	/**
	 * sets proxy user
	 *
	 * @param proxyUser
	 */
	public void setProxyUser(String proxyUser) {
		this.proxyUser = proxyUser;
	}

	/**
	 * returns proxy password
	 *
	 * @return
	 */
	public String getProxyPassword() {
		return proxyPassword;
	}

	/**
	 * sets proxy password
	 *
	 * @param proxyPassword
	 */
	public void setProxyPassword(String proxyPassword) {
		this.proxyPassword = proxyPassword;
	}

	/**
	 * returns cert validation (boolean)
	 *
	 * @return
	 */
	public boolean isDisableCertValidation() {
		return disableCertValidation;
	}

	/**
	 * sets cert validation (boolean)
	 *
	 * @param disableCertValidation
	 */
	public void setDisableCertValidation(boolean disableCertValidation) {
		this.disableCertValidation = disableCertValidation;
	}

	/**
	 * returns ssl context
	 *
	 * @return
	 */
	public String getsSLContext() {
		return sSLContext;
	}

	/**
	 * sets ssl context
	 *
	 * @param sSLContext
	 */
	public void setsSLContext(String sSLContext) {
		this.sSLContext = sSLContext;
	}

	/**
	 * returns error payload
	 *
	 * @return
	 */
	public String getErrorPayload() {
		return errorPayload;
	}

	/**
	 * sets error payload
	 *
	 * @param errorPayload
	 */
	public void setErrorPayload(String errorPayload) {
		this.errorPayload = errorPayload;
	}

	/**
	 * returns follow redirects (boolean)
	 *
	 * @param followRedirects
	 */
	public void setfollowRedirects(boolean followRedirects) {
		this.followRedirects = followRedirects;
	}

	/**
	 * returns reponseStatusCode
	 *
	 * @return
	 */
	public int getResponseStatusCode() {
		return responseCode;
	}

	// #endregion getters and setters

	@SuppressWarnings("squid:S4144")
	public Integer getResponseCode() {
		return responseCode;
	}

	private void setResponseCode(Integer responseCode) {
		this.responseCode = responseCode;
	}
	
	public InputStream getResponseStream() {
		return responseStream;
	}

	public void setResponseStream(InputStream responseStream) {
		this.responseStream = responseStream;
	}
	

	public boolean isConvertResponseToString() {
		return convertResponseToString;
	}

	public void setConvertResponseToString(boolean convertResponseToString) {
		this.convertResponseToString = convertResponseToString;
	}
}

import java.io.StringReader;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;
import org.xml.sax.Attributes;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.XMLReader;
import org.xml.sax.helpers.DefaultHandler;

public class HttpUtilityTest {

	private HttpUtility httpUtility;

	public static void main(String[] args) throws Exception {
		
		HttpUtilityTest httpUtilityTest = new HttpUtilityTest();
		httpUtilityTest.send();
		
	}

	public void send() throws Exception {

		boolean streaming = true;
		System.out.println("send().is streaming ? " + streaming);
		
		
		String requestString = "<soap:Envelope xmlns:soap=\"http://www.w3.org/2003/05/soap-envelope\" xmlns:web=\"http://webservicesV2.bookcyprus.com/\">\n"
				+ "   <soap:Header/>\n"
				+ "   <soap:Body>\n"
				+ "      <web:HotelAvailabilitySearch>\n"
				+ "         <web:rq>\n"
				+ "            <web:BaseRequest>\n"
				+ "               <web:UserName>premiereservices@testcredentials.com</web:UserName>\n"
				+ "               <web:Password>premservices123</web:Password>\n"
				+ "               <web:Currency>USD</web:Currency>\n"
				+ "               <web:Language>EN</web:Language>\n"
				+ "            </web:BaseRequest>\n"
				+ "            <web:FromDate>2022-06-14T00:00:00</web:FromDate>\n"
				+ "            <web:ToDate>2022-06-15T00:00:00</web:ToDate>\n"
				+ "           <web:Occupancy>\n"
				+ "               <web:HotelOccupancy>\n"
				+ "                  <web:RoomIndex>0</web:RoomIndex>\n"
				+ "                  <web:Adults>2</web:Adults>\n"
				+ "               </web:HotelOccupancy>\n"
				+ "            </web:Occupancy>\n"
				+ "            <web:LocationId>1957</web:LocationId>\n"
				+ "            <web:RegionId>0</web:RegionId>\n"
				+ "            <web:StarClassificationId>0</web:StarClassificationId>\n"
				+ "            <web:HotelId>0</web:HotelId>\n"
				+ "         </web:rq>\n"
				+ "      </web:HotelAvailabilitySearch>\n"
				+ "   </soap:Body>\n"
				+ "</soap:Envelope>";
		
		System.out.println("send().requestString : " + requestString);
		
		String url = "https://ws-premiere.beta.belugga.net/eBookingService.asmx";
		String soapAction = "http://webservicesV2.bookcyprus.com/HotelAvailabilitySearch";
		
		long start1 = System.currentTimeMillis();
		
		httpUtility =  new HttpUtility();
		httpUtility.setUrl(url);
		httpUtility.setUseGzip(true);
		httpUtility.addConnectionProp("SOAPAction", "http://webservicesV2.bookcyprus.com/HotelAvailabilitySearch");
		httpUtility.addConnectionProp("Content-Type", "text/xml");
		httpUtility.setRequestPayload(requestString);
		httpUtility.setRequestMethod("POST");
		httpUtility.setConvertResponseToString(!streaming);
		System.out.println("HttpUtilityTest.send() done!");
		httpUtility.send();

		String actualResponse = null;
		if(!streaming) {
			actualResponse = httpUtility.getResponsePayload();
			String requestXML = XMLUtil.getPrettyString(actualResponse, 2);
			System.out.println("send().requestXML : " + requestXML);
		}
		
		long start2 = System.currentTimeMillis();
		System.out.println( (streaming ? "Reaching first response bit" : "Downloaded response") + " took : " + (start2-start1)/1000 + "(s)");
		
		SAXParserFactory parserFactor = SAXParserFactory.newInstance();
		SAXParser parser = parserFactor.newSAXParser();
		SAXHandler handler = new SAXHandler();
		
		XMLReader xmlReader = parser.getXMLReader();
		
		xmlReader.setErrorHandler(new CustomErrorHandlerSax(System.err));

        xmlReader.setContentHandler(handler);

		if(streaming) {
//			parser.parse(httpUtility.getResponseStream(), handler);	
			xmlReader.parse(new InputSource(httpUtility.getResponseStream()));
		}else {
			
//			InputSource input = new InputSource(new StringReader(actualResponse));
//			parser.parse(input, handler);
			
			InputSource input = new InputSource(new StringReader(actualResponse));
			input.setEncoding("UTF-8");
			xmlReader.parse(input);
		}
		
		long start3 = System.currentTimeMillis();
		System.out.println((streaming ? "download + " : "") + "decoded took : " + (start3-start2)/1000 + "s");
		System.out.println("Total time : " + (start3-start1)/1000);
		
	}

}


class SAXHandler extends DefaultHandler {


	String content = null;
	
	String xmlCurrency = null;
	
//	<HotelResult>
	int xmlAdultCount = 0;
	int xmlChildCount = 0;
	
	//<HotelResultRoom>
	String xmlRoomId = null;
	String xmlRoomName = null;
	int xmlRoomCnt = 0;
	boolean xmlIsOnRequest = false;
	
	//<ServiceRate>
	int xmlMealId = 0;
	String xmlMealCode = null;
	String xmlMealName = null;
	boolean xmlIsNonRef = false;
	String xmlIsFreeCancellation = null;
	String xmlPaymentType = null;
	double xmlAmount = 0.0d;
	double xmlNetAmount = 0.0d;
	double xmlTaxAmount = 0.0d;
	String xmlGroupIdentifier = null;
	String xmlUniqueRoomCode = null;
	
	@Override
	public void startElement(String uri, String localName, String qName, Attributes attributes) throws SAXException {

//		System.out.println("startElement qName : " + qName);
		
		
		switch(qName){
			case "Hotels":
				System.out.println("<Hotels> : HotelList is created!");
				break;
				
			case "HotelService":
				System.out.println("");
				System.out.println("<HotelService> : Hotel is created! ---------------------------------------------------------------------------------------------------------------------");
				break;
				
			case "ServiceRate":
				
				System.out.println("ServiceRate started!");
	
				xmlMealId = Integer.parseInt(attributes.getValue("MealId"));
				xmlMealCode = attributes.getValue("Meal");
				xmlMealName = attributes.getValue("MealName");
				xmlIsNonRef = Boolean.parseBoolean(attributes.getValue("IsNonRef"));
				xmlIsFreeCancellation = attributes.getValue("FreeCancellation");
				xmlPaymentType = attributes.getValue("PaymentType");
				
				break;
		}

	}

	@Override
	public void endElement(String uri, String localName, String qName) throws SAXException {
//		System.out.println("endElement qName : " + qName);
		
		/*
		 * !!! NonPayableFees
		 */
		
		switch(qName){
			case "Currency":
				xmlCurrency = content;
				break;
				
			case "HotelId":
				System.out.println("</HotelId>  HotelCode : " + content);
				break;
				
			case "HotelName":
				System.out.println("</HotelName>  HotelName : " + content);
				break;
				
			case "StarClassName":
				
				double supplieStarClassName = Double.parseDouble(content);
				int rideStarRating = (int)Math.floor(supplieStarClassName);
				
				System.out.println("</StarClassName>  supplieStarClassName : " + supplieStarClassName + 
						" | rideStarRating : " + rideStarRating+"EST");
				
				break;
				
			case "HotelService":
				break;
				
			case "Hotels":
				
				System.out.println("<Hotels> : HotelList is done!");
				break;
			
			case "Adults":
				xmlAdultCount = Integer.parseInt(content);
				break;
				
			case "Children":
				xmlChildCount = Integer.parseInt(content);
				break;
				
			case "RoomId":
				xmlRoomId = content;
				break;
				
			case "Room":
				xmlRoomName = content;
				break;
				
			case "RoomCnt":
				xmlRoomCnt = Integer.parseInt(content);;
				break;
				
			case "IsOnRequest":
				xmlIsOnRequest = Boolean.parseBoolean(content);
				break;
				
			case "Amount":
				xmlAmount = Double.parseDouble(content);
				break;
				
			case "NetAmount":
				xmlNetAmount = Double.parseDouble(content);;
				break;
				
			case "TaxAmount":
				xmlTaxAmount = Double.parseDouble(content);;
				break;
				
			case "GroupIdentifier":
				xmlGroupIdentifier = content;
				break;
				
			case "UniqueRoomCode":
				xmlUniqueRoomCode = content;
				break;
				
			case "HotelResultRoom":
				
				System.out.println("<HotelResult>  xmlAdultCount : " + xmlAdultCount + 
						" | xmlChildCount : " + xmlChildCount +
						"");
				
				
				System.out.println("<HotelResultRoom>  xmlRoomId : " + xmlRoomId + 
						" | xmlRoomName : " + xmlRoomName +
						" | xmlRoomCnt : " + xmlRoomCnt +
						" | xmlIsOnRequest : " + xmlIsOnRequest +
						"");
				
				
				System.out.println("<ServiceRate>  xmlMealId : " + xmlMealId + 
						" | xmlMealCode : " + xmlMealCode +
						" | xmlMealName : " + xmlMealName +
						" | xmlIsNonRef : " + xmlIsNonRef +
						" | xmlIsFreeCancellation : " + xmlIsFreeCancellation +
						" | xmlPaymentType : " + xmlPaymentType +
						" | xmlAmount : " + xmlAmount +
						" | xmlNetAmount : " + xmlNetAmount +
						" | xmlTaxAmount : " + xmlTaxAmount +
						" | xmlGroupIdentifier : " + xmlGroupIdentifier +
						" | UniqueRoomCode : " + xmlUniqueRoomCode +
						"");
				
				break;
				
		}
		
	}

	@Override
	public void characters(char[] ch, int start, int length) throws SAXException {
		
		content = String.copyValueOf(ch, start, length).trim();
//		System.out.println("content : " + content);
		
	}

}

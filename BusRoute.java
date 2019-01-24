//Alanna Romao CS 320
//This program uses regular expressions to find route information from the Community Transit website.
//The user is prompted to enter the first letter of their destination and is given a list of 
//destinations starting with that letter and bus routes that go there. The user can then pick
//a route and are given the stops on that route.

package busroute;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import static java.lang.System.out;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.net.*;
import java.util.ArrayList;
import java.util.List;
import static java.lang.System.in;

public class BusRoute {
	public static void main(String[] args) {
		String webPageText = ReadWebPage("https://www.communitytransit.org/busservice/schedules/");
		
		if(webPageText == null) {
			out.println("Failed to read website");
			return;
		}
		
		//class to hold route information
		class Route {
			Route(String bus, String link) {
				id = bus;
				URL = link;
			}
			String id;
			String URL;
		}

		//class to hold city information
		class Community {
			Community(String routeName, int beginMatch, int endMatch) {
				 name = routeName;
				 matchStart = beginMatch;
				 matchEnd = endMatch;
			}
			String name;
			int matchStart;
			int matchEnd;
			List<Route> routeList = new ArrayList<>();
		}

		List<Community> communityList = new ArrayList<>();
		int i = 0;
		int matchStart;
		Pattern pattern = Pattern.compile("<hr\\s+id=[^>]+>\\s+<h3>\\s*([^<]+)\\s*</h3>"); //regular expression to find city names
		Matcher matcher = pattern.matcher(webPageText);
		while(matcher.find()) {
			matchStart = matcher.start();
			Community c = new Community(matcher.group(1), matchStart, 0); //stores city information in a community
			communityList.add(c);
			if(i > 0) {
				  Community p = communityList.get(i - 1);
				  p.matchEnd = matchStart; //sets the matchEnd for the previous match
			}
			i++;
		}

		i--;
		if(i >= 0) {
			//sets the matchEnd for the final community 
			Community c = communityList.get(i);
			pattern = Pattern.compile("<table");
			matcher = pattern.matcher(webPageText);
			matcher.region(c.matchStart + 3, webPageText.length() - 1);
			if(matcher.find())
				c.matchEnd = matcher.start();
		}

		//regular expression to find the URL and name for each route
		pattern = Pattern.compile("<a\\s+href=\"([^\"]+)\"\\s*>\\s*([^<\\*]+)\\s*</a>");
		matcher = pattern.matcher(webPageText);
		for(Community c : communityList) {
			matcher.region(c.matchStart, c.matchEnd);
			while(matcher.find()) {
				c.routeList.add(new Route(matcher.group(2).trim(), "https://www.communitytransit.org" + matcher.group(1)));
			}
		}
		
		while(true) {
			//gets starting letter of destination
			String inputLine = readEntry("Please enter a letter that your destination starts with or ! to quit: ");
			if(inputLine.length() < 1)
				continue;
			
			inputLine = inputLine.toUpperCase();
			char startsWith = inputLine.charAt(0);
			if(startsWith == '!')
				return;
			
			boolean found = false;
			//finds and prints out destination routes
			for(Community c : communityList) {
				if(Character.toUpperCase(c.name.charAt(0)) == startsWith){
					found = true;
					out.println("Destinaton: " + c.name);
					for(Route r : c.routeList) {
						out.println("Bus Number: " + r.id);
					}
					out.println("+++++++++++++++++++++++++++++++++++++++++++");
				}
			}
			
			if(!found) {
				out.println("No destination found starting with " + startsWith);
				continue;
			}
			
			Route bus = null;
			//gets the route ID
			inputLine = readEntry("Please enter a route ID as a string: ");
			for(Community c : communityList) {
				for(Route r : c.routeList) {
					if(r.id.equalsIgnoreCase(inputLine)) {
						bus = r;
						break;
					}
				}
				if(bus != null)
					break;
			}
			
			if(bus == null) {
				out.println("No matching route ID found");
				continue;
			}
			
			//gives user URL for route
			out.println("The link for your route is: " + bus.URL);
			webPageText = ReadWebPage(bus.URL);
			if(webPageText == null) {
				out.println("Failed to read website");
				return;
			}
			
			class Destination {
				Destination(String destName) {
					name = destName;
				}
				
				String name;
				int matchStart;
				int matchEnd;
			}
			
			List<Destination> destinationList = new ArrayList<>();
			//regular expression to get route destination name
			pattern = Pattern.compile("<h2>Weekday<small>([^<]+)</small></h2>");
			matcher = pattern.matcher(webPageText);
			i = 0;
			while(matcher.find()) {
				matchStart = matcher.start();
				Destination d = new Destination(matcher.group(1));
				d.matchStart = matchStart;
				destinationList.add(d);
				
				if(i > 0) {
					//sets last destination matchEnd to be the start of current destination matchStart
					Destination p = destinationList.get(i - 1);
					p.matchEnd = matchStart;
				}
				i++;
			}
			
			i--;
			if(i >= 0) {
				//sets last destination matchEnd as the end of the webpage
				Destination d = destinationList.get(i);
				d.matchEnd = webPageText.length() - 1;
			}
			
			//regular expression to get the stop name and stop number for destinations
			pattern = Pattern.compile("<strong[^>]+>([^<]+)<[^<]+</span[^<]+<p>([^<]+)</p>");
			matcher = pattern.matcher(webPageText);
			for(Destination d : destinationList) {
				//prints out destination and stops
				out.println("Destination: " + d.name);
				matcher.region(d.matchStart, d.matchEnd);
				while(matcher.find()) {
					out.println("Stop number: " + matcher.group(1) + " is " + matcher.group(2).replace("&amp;", "&"));
				}
				out.println("+++++++++++++++++++++++++++++++++++++++++++");
			}
		}
	}
	
	//method to read webpage
	static private String ReadWebPage(String url) {
		try {
			URLConnection webSite = new URL(url).openConnection();
			webSite.setRequestProperty("user-Agent", "Mozilla/5.0 (Windows NT 6.1; WOW64) AppleWebKit/537.11 (KHTML, like Gecko) Chrome/23.0.1271.95 Safari/537.11");

			BufferedReader webInput = new BufferedReader(new InputStreamReader(webSite.getInputStream()));

			String inputLine;
			// Using StrinBuilder since it's much faster than string concatenation
			StringBuilder sb = new StringBuilder();
			while ((inputLine = webInput.readLine()) != null) {
				sb.append(inputLine);
			}

			webInput.close();
			return sb.toString();
		}
		
		catch(Exception e) {
			return null;
		}
	}

	//method to read in strings from user
	static String readEntry(String prompt) {
		try {
				StringBuffer buffer = new StringBuffer();
				System.out.print(prompt);
				System.out.flush();
				int c = System.in.read();
				while(c != '\n' && c != -1) {
						buffer.append((char)c);
						c = System.in.read();
				}
				return buffer.toString().trim();
		} catch (IOException e) {
				return "";
		}
	}
}

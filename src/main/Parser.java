package main;

import java.io.IOException;
import java.util.ArrayList;

import org.json.JSONException;
import org.json.JSONObject;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import subtitle_model.Episode;
import subtitle_model.Lang;
import subtitle_model.State;
import subtitle_model.Subtitle;
import subtitle_model.Version;

public class Parser {
	
	
	/**
	 * Method that parses a show page from tusubtitulo.com and returns the number of seasons in that show
	 * 
	 * @param link Link to the show page of interest
	 * @return Array containing the detected seasons, be it 1,5,6 or 1,2,3
	 * @throws IOException
	 */
	public static ArrayList<Integer> parseSeasons(String link) throws IOException{
		Document doc = Jsoup.connect(link).get();
		ArrayList<Integer> seasons = new ArrayList<Integer>();
		
		Element aux = doc.getElementById("content");
		Elements temp = aux.child(1).child(0).child(1).child(3).child(0).getAllElements();
		for(Element i: temp){
			try{
				seasons.add(Integer.parseInt(i.text()));
			}
			catch(Exception e){}
		}
		return seasons;
	}
	
	
	/**
	 * Method that parses a season page from tusubtitulo.com and returns the number of episodes in that season
	 * 
	 * @param season Season to look for
	 * @param showId Show to look for
	 * @return Array containing the episodes in the detected season
	 * @throws IOException
	 * @throws JSONException 
	 */
	public static JSONObject parseEpisodes(int season, String showId) throws IOException, JSONException {
		String link = "https://www.tusubtitulo.com/ajax_loadShow.php?show=" + showId + "&season=" + season;
		Document doc = Jsoup.connect(link).get();
		Elements aux = doc.children().get(0).children().get(1).children();
		ArrayList<String> episodes = new ArrayList<String>();
		JSONObject relations = new JSONObject();
		JSONObject seasonPage = new JSONObject();
		
		for(Element e : aux){
			String episodeName = e.children().get(0).children().get(0).children().get(2).children().get(1).text();
			String episodeLink = e.children().get(0).children().get(0).children().get(2).children().get(1).attr("href");
			episodeLink = "http://" + episodeLink.substring(2);
			episodes.add(e.children().get(0).children().get(0).children().get(2).children().get(1).text());
			relations.put(episodeName, episodeLink);
		}
		seasonPage.put("relations", relations);
		seasonPage.put("titles", episodes);
		
		
		return seasonPage;
	}
	
	
	/**
	 * Method that parses an episode page from tusbtitulo.com and returns all the information well structured as an Episode class
	 * 
	 * @param link Link to the episode page of interest
	 * @return Episode object containing all the information
	 * @throws IOException
	 */
	public static Episode parseEpisodePage(String link) throws IOException{

		String title;
		String season;
		String episode;
		String series;
		Episode currentEpisode;
		
		
		Document doc = Jsoup.connect(link).get();
		Element content = doc.body().getElementById("content");
		Element cabecera = content.getElementById("cabecera-subtitulo");
		String info = cabecera.text().substring(14);
		title = info.split("-")[1];
		String[] med = info.split("-")[0].split(" ");
		season = med[med.length-1].split("x")[0];
		episode = med[med.length-1].split("x")[1];		
		med[med.length-1] = "";
		series = "";
		for(String i : med){
			series += " ";
			series += i;
		}
		title = title.trim();
		System.out.println(title);
		series = series.trim();
		season = season.trim();
		episode = episode.trim();

//		System.out.println(title);
//		System.out.println(season);
//		System.out.println(episode);
//		System.out.println(series);
		
		currentEpisode = new Episode(series, season, title, episode);
		
		
		Elements versions = content.getElementsByClass("ssdiv");
		Elements versions2 = new Elements();
		for(Element e : versions){
			if(!e.id().equals("ssheader")){
				versions2.add(e);
			}
		}
		
		for(Element e : versions2){
			String versionName;
			Elements aux = e.getElementsByClass("bubble");
			versionName = aux.get(0).getElementsByClass("title-sub").text();
			Version temp = new Version(versionName);
			
			Elements subtitleEntries = e.getElementsByClass("sslist");
			for(Element i : subtitleEntries){
				String subLink = "";
				Lang lang = Lang.ERR;
				State state;
				
				
				Elements in = i.getElementsByClass("li-idioma");
				String langText = in.get(0).text();
				
				if(langText.equals("English")){
					lang = Lang.en_GB;
				}
				if(langText.equals("Espa�ol (Espa�a)")){
					lang = Lang.es_ESP;
				}
				if(langText.equals("Espa�ol (Latinoam�rica)")){
					lang = Lang.es_LAT;
				}
				
				String stateText = in.get(0).nextElementSibling().text().trim();
				if(stateText.equals("Completado")){
					state = State.COMPLETE;
				}
				else if(stateText.contains("%")){
					state = State.TRANSLATING;
				}
				else{
					state = State.REVISION;
				}
				
				if(state == State.COMPLETE){
					Elements temp2 = i.children().get(2).children();
					Element actualLink = temp2.get(temp2.size()-2);
					String linkText = actualLink.attr("href");
					subLink = "https://www.tusubtitulo.com" + "/" + linkText;
				}
				
				Subtitle sub = new Subtitle(subLink, lang, state);
				temp.addSubtitle(sub);
			}
			currentEpisode.addVersion(temp);
			
		}
		
		return currentEpisode;
		
	}
}
